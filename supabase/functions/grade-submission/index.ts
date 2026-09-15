// Supabase Edge Function: grade-submission
//
// Pipeline (spec section 28-34, 65-66, 86-88):
//   submission (status=uploaded)
//     -> download page images
//     -> single multimodal Gemini call: OCR + per-question answer
//        extraction + grading against the official answer key, returning
//        structured JSON (never a bare score)
//     -> confidence < 0.6 => never auto-finalized, submission flagged
//        needs_review (spec section 34, 44)
//     -> extracted_answers written, submissions aggregate recomputed
//
// AI API keys and the answer key never leave this server-side function.
import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
// SUPABASE_ANON_KEY isn't reliably auto-injected for projects on the newer
// publishable/secret key format, so this is set explicitly as a function
// secret (names starting with SUPABASE_ are reserved and can't be set manually).
const ANON_KEY = (Deno.env.get("SUPABASE_ANON_KEY") || Deno.env.get("PUBLISHABLE_KEY"))!;
const GEMINI_API_KEY = Deno.env.get("GEMINI_API_KEY")!;
// Tried in order; Gemini's newest preview models occasionally return 503
// under high demand, so we fall back rather than failing the submission.
const GEMINI_MODELS = (Deno.env.get("GEMINI_MODEL") ?? "gemini-3.5-flash,gemini-3-flash-preview,gemini-3.7-flash")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

const CONFIDENCE_AUTO_ACCEPT_THRESHOLD = 0.6;

const DEFAULT_GRADE_BANDS = [
  { min: 90, max: 100, grade: "A+" },
  { min: 80, max: 89.99, grade: "A" },
  { min: 70, max: 79.99, grade: "B" },
  { min: 60, max: 69.99, grade: "C" },
  { min: 50, max: 59.99, grade: "D" },
  { min: 0, max: 49.99, grade: "F" },
];

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return json({ error: "POST only" }, 405);
  }

  let submissionId: string;
  try {
    const body = await req.json();
    submissionId = body.submission_id;
    if (!submissionId) throw new Error("missing submission_id");
  } catch {
    return json({ error: "submission_id is required" }, 400);
  }

  // Authorization: confirm the caller can actually see this submission
  // under RLS (its own student, or the exam's teacher/admin) before doing
  // any privileged work with the service-role client.
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "Missing authorization" }, 401);

  const callerClient = createClient(SUPABASE_URL, ANON_KEY, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: authCheck } = await callerClient
    .from("submissions")
    .select("id")
    .eq("id", submissionId)
    .maybeSingle();
  if (!authCheck) return json({ error: "Not authorized for this submission" }, 403);

  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

  try {
    const { data: submission, error: subErr } = await supabase
      .from("submissions")
      .select("id, exam_id, student_id, status")
      .eq("id", submissionId)
      .single();

    if (subErr || !submission) return json({ error: "Submission not found" }, 404);

    if (!["uploaded", "failed"].includes(submission.status)) {
      return json({ message: `Submission already ${submission.status}, skipping.` }, 200);
    }

    await supabase.from("submissions").update({ status: "processing" }).eq("id", submissionId);

    const { data: pages } = await supabase
      .from("submission_pages")
      .select("page_number, file_path")
      .eq("submission_id", submissionId)
      .order("page_number", { ascending: true });

    if (!pages || pages.length === 0) {
      await markFailed(supabase, submissionId, "No submission pages found.");
      return json({ error: "No submission pages found" }, 400);
    }

    const { data: questions } = await supabase
      .from("questions")
      .select("id, question_number, question_text, question_type, max_marks")
      .eq("exam_id", submission.exam_id)
      .order("question_number", { ascending: true });

    if (!questions || questions.length === 0) {
      await markFailed(supabase, submissionId, "No questions configured for this exam.");
      return json({ error: "No questions found" }, 400);
    }

    const { data: answerRows } = await supabase
      .from("question_answers")
      .select("question_id, correct_answer, rubric, grading_instructions")
      .in("question_id", questions.map((q) => q.id));

    const answerByQuestion = new Map((answerRows ?? []).map((r) => [r.question_id, r]));

    await supabase.from("submissions").update({ status: "ocr_processing" }).eq("id", submissionId);

    const imageParts: { inlineData: { mimeType: string; data: string } }[] = [];
    for (const page of pages) {
      const { data: blob, error: dlErr } = await supabase.storage
        .from("student-submissions")
        .download(page.file_path);
      if (dlErr || !blob) continue;
      const bytes = new Uint8Array(await blob.arrayBuffer());
      imageParts.push({ inlineData: { mimeType: "image/jpeg", data: base64Encode(bytes) } });
    }

    if (imageParts.length === 0) {
      await markFailed(supabase, submissionId, "Could not download any submission pages.");
      return json({ error: "Could not download pages" }, 500);
    }

    await supabase.from("submissions").update({ status: "grading" }).eq("id", submissionId);

    const prompt = buildPrompt(questions, answerByQuestion, imageParts.length);
    const geminiRequestBody = JSON.stringify({
      contents: [{ role: "user", parts: [...imageParts, { text: prompt }] }],
      generationConfig: { responseMimeType: "application/json" },
    });

    let geminiRes: Response | null = null;
    let lastErrText = "";
    for (const model of GEMINI_MODELS) {
      const res = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${GEMINI_API_KEY}`,
        { method: "POST", headers: { "Content-Type": "application/json" }, body: geminiRequestBody },
      );
      if (res.ok) {
        geminiRes = res;
        break;
      }
      lastErrText = await res.text();
      // Only fall through to the next model on transient capacity errors;
      // any other error (bad request, auth, etc.) is the same for all models.
      if (res.status !== 503 && res.status !== 429) {
        geminiRes = res;
        break;
      }
    }

    if (!geminiRes || !geminiRes.ok) {
      console.error(`Gemini error for submission ${submissionId}: ${lastErrText.slice(0, 500)}`);
      await markFailed(supabase, submissionId, `AI grading unavailable: ${lastErrText.slice(0, 300)}`);
      return json({ error: "AI grading unavailable" }, 502);
    }

    const geminiJson = await geminiRes.json();
    const responseText = geminiJson?.candidates?.[0]?.content?.parts?.find((p: any) => p.text)?.text;

    let parsed: any[] = [];
    try {
      parsed = JSON.parse(responseText ?? "[]");
      if (!Array.isArray(parsed)) throw new Error("not an array");
    } catch {
      await markFailed(supabase, submissionId, "AI returned an unparseable response.");
      return json({ error: "Unparseable AI response" }, 502);
    }

    const gradedByQuestionId = new Map(parsed.map((p) => [String(p.question_id), p]));

    const extractedRows = questions.map((q) => {
      const maxScore = Number(q.max_marks);
      const g = gradedByQuestionId.get(q.id);
      if (!g) {
        return {
          submission_id: submissionId,
          question_id: q.id,
          raw_text: null,
          normalized_answer: null,
          ai_score: 0,
          max_score: maxScore,
          confidence: 0,
          grading_reason: "AI did not return a result for this question.",
          needs_review: true,
          final_score: null as number | null,
        };
      }
      const confidence = clamp(Number(g.confidence ?? 0), 0, 1);
      const aiScore = clamp(Number(g.score ?? 0), 0, maxScore);
      // Never auto-finalize a low-confidence answer (spec section 34/44).
      const autoAccept = confidence >= CONFIDENCE_AUTO_ACCEPT_THRESHOLD && !g.needs_review;
      return {
        submission_id: submissionId,
        question_id: q.id,
        raw_text: g.raw_text ?? null,
        normalized_answer: g.normalized_answer ?? null,
        ai_score: aiScore,
        max_score: maxScore,
        confidence,
        grading_reason: g.grading_reason ?? null,
        needs_review: !autoAccept,
        final_score: autoAccept ? aiScore : null,
      };
    });

    await supabase.from("extracted_answers").upsert(extractedRows, { onConflict: "submission_id,question_id" });

    const totalMarks = extractedRows.reduce((sum, r) => sum + r.max_score, 0);
    const earned = extractedRows.reduce((sum, r) => sum + (r.final_score ?? 0), 0);
    const aiTotal = extractedRows.reduce((sum, r) => sum + r.ai_score, 0);
    const percentage = totalMarks > 0 ? (earned / totalMarks) * 100 : 0;
    const anyNeedsReview = extractedRows.some((r) => r.needs_review);

    const { data: examRow } = await supabase
      .from("exams")
      .select("school_id, show_result_immediately, require_teacher_approval")
      .eq("id", submission.exam_id)
      .single();

    const grade = await resolveGrade(supabase, examRow?.school_id, percentage);
    const resultVisible = !anyNeedsReview &&
      !(examRow?.require_teacher_approval ?? false) &&
      (examRow?.show_result_immediately ?? true);

    await supabase
      .from("submissions")
      .update({
        ai_score: aiTotal,
        final_score: earned,
        total_marks: totalMarks,
        percentage,
        grade,
        needs_review: anyNeedsReview,
        status: anyNeedsReview ? "needs_review" : "graded",
        result_visible: resultVisible,
      })
      .eq("id", submissionId);

    await supabase.from("notifications").insert({
      user_id: submission.student_id,
      title: anyNeedsReview ? "Your exam is being reviewed" : "Your exam has been graded",
      message: anyNeedsReview
        ? "Some answers need teacher review before your result is final."
        : `You scored ${earned}/${totalMarks}.`,
      type: "grading_completed",
      related_exam_id: submission.exam_id,
      related_submission_id: submissionId,
    });

    return json(
      { status: anyNeedsReview ? "needs_review" : "graded", final_score: earned, total_marks: totalMarks },
      200,
    );
  } catch (e) {
    console.error(e);
    await markFailed(supabase, submissionId, String(e));
    return json({ error: String(e) }, 500);
  }
});

function buildPrompt(
  questions: { id: string; question_number: number; question_text: string; question_type: string; max_marks: number }[],
  answerByQuestion: Map<string, { correct_answer: string | null; rubric: unknown; grading_instructions: string | null }>,
  pageCount: number,
): string {
  const questionBlock = questions
    .map((q) => {
      const ans = answerByQuestion.get(q.id);
      return [
        `Question ${q.question_number} (id: ${q.id})`,
        `Type: ${q.question_type}`,
        `Max marks: ${q.max_marks}`,
        `Text: ${q.question_text}`,
        ans?.correct_answer ? `Official answer: ${ans.correct_answer}` : "Official answer: (not provided)",
        ans?.rubric ? `Rubric: ${JSON.stringify(ans.rubric)}` : "",
        ans?.grading_instructions ? `Grading instructions: ${ans.grading_instructions}` : "",
      ].filter(Boolean).join("\n");
    })
    .join("\n\n");

  return `You are grading a school examination from photographs of a student's handwritten answer sheet.

You are given ${pageCount} page image(s) of the student's completed paper, in order, followed by the exam's questions and official answers below.

${questionBlock}

For EACH question listed above, find the student's handwritten answer in the images and grade it strictly against the official answer/rubric.

Critical rules:
- Do NOT invent or guess an answer you cannot read. If the handwriting is unclear or the answer is missing, set raw_text to null, score to 0, confidence below 0.4, and needs_review to true, with a reason explaining why.
- For multiple choice / true-false, match the selected option exactly.
- For mathematical answers, treat mathematically equivalent forms (e.g. 2/4 and 1/2) as correct.
- confidence must be a number from 0 to 1 reflecting how certain you are of BOTH the OCR reading and the grade.
- Grade strictly according to the rubric/official answer given; do not use outside knowledge to override it.

Return ONLY a JSON array, one object per question, in this exact shape:
[
  {
    "question_id": "<the id given above>",
    "raw_text": "<verbatim transcription of the student's answer, or null>",
    "normalized_answer": "<cleaned/normalized answer, or null>",
    "score": <number, 0 to max marks>,
    "confidence": <number, 0 to 1>,
    "needs_review": <boolean>,
    "grading_reason": "<short explanation of the score>"
  }
]`;
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, Number.isFinite(n) ? n : min));
}

function base64Encode(bytes: Uint8Array): string {
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

async function markFailed(supabase: ReturnType<typeof createClient>, submissionId: string, reason: string) {
  await supabase.from("submissions").update({ status: "failed" }).eq("id", submissionId);
  console.error(`Submission ${submissionId} failed: ${reason}`);
}

async function resolveGrade(
  supabase: ReturnType<typeof createClient>,
  schoolId: string | undefined,
  percentage: number,
): Promise<string> {
  if (schoolId) {
    const { data } = await supabase
      .from("grading_scales")
      .select("bands")
      .eq("school_id", schoolId)
      .eq("is_default", true)
      .limit(1)
      .maybeSingle();
    const bands = data?.bands as { min: number; max: number; grade: string }[] | undefined;
    const match = bands?.find((b) => percentage >= b.min && percentage <= b.max);
    if (match) return match.grade;
  }
  return DEFAULT_GRADE_BANDS.find((b) => percentage >= b.min && percentage <= b.max)?.grade ?? "-";
}
