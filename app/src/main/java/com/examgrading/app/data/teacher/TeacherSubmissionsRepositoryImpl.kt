package com.examgrading.app.data.teacher

import com.examgrading.app.domain.models.QuestionReview
import com.examgrading.app.domain.models.SubmissionListItem
import com.examgrading.app.domain.models.SubmissionReviewDetail
import com.examgrading.app.domain.models.TeacherExamSummary
import com.examgrading.app.domain.repositories.TeacherSubmissionsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TeacherSubmissionsRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TeacherSubmissionsRepository {

    override suspend fun getMyExams(teacherId: String): Result<List<TeacherExamSummary>> = runCatching {
        val exams = supabase.postgrest.from("exams")
            .select(columns = Columns.raw("id,title,exam_date,status,subject:subjects(name)")) {
                filter { eq("teacher_id", teacherId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<TeacherExamRow>()

        if (exams.isEmpty()) return@runCatching emptyList()
        val examIds = exams.map { it.id }

        val assignmentCounts = supabase.postgrest.from("exam_assignments")
            .select(columns = Columns.list("exam_id")) {
                filter { isIn("exam_id", examIds) }
            }
            .decodeList<ExamIdOnlyDto>()
            .groupingBy { it.examId }
            .eachCount()

        val submissionRows = supabase.postgrest.from("submissions")
            .select(columns = Columns.list("exam_id", "status", "needs_review")) {
                filter { isIn("exam_id", examIds) }
            }
            .decodeList<SubmissionCountRow>()
            .groupBy { it.examId }

        exams.map { exam ->
            val subs = submissionRows[exam.id].orEmpty()
            TeacherExamSummary(
                examId = exam.id,
                title = exam.title,
                subjectName = exam.subject?.name,
                examDate = exam.examDate,
                status = exam.status,
                assignedCount = assignmentCounts[exam.id] ?: 0,
                submittedCount = subs.count { it.status != "draft" },
                gradedCount = subs.count { it.status == "graded" },
                needsReviewCount = subs.count { it.needsReview }
            )
        }
    }

    override suspend fun getExamSubmissions(examId: String): Result<List<SubmissionListItem>> = runCatching {
        supabase.postgrest.from("submissions")
            .select(
                columns = Columns.raw(
                    "id,status,final_score,total_marks,percentage,needs_review,student:profiles(full_name)"
                )
            ) {
                filter { eq("exam_id", examId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<SubmissionRow>()
            .map {
                SubmissionListItem(
                    submissionId = it.id,
                    studentName = it.student.fullName,
                    status = it.status,
                    finalScore = it.finalScore,
                    totalMarks = it.totalMarks,
                    percentage = it.percentage,
                    needsReview = it.needsReview
                )
            }
    }

    override suspend fun getSubmissionReview(submissionId: String): Result<SubmissionReviewDetail> = runCatching {
        val submission = supabase.postgrest.from("submissions")
            .select(columns = Columns.raw("id,status,student:profiles(full_name)")) {
                filter { eq("id", submissionId) }
            }
            .decodeSingle<SubmissionDetailRow>()

        val extractedAnswers = supabase.postgrest.from("extracted_answers")
            .select(
                columns = Columns.raw(
                    "id,question_id,raw_text,ai_score,max_score,confidence,grading_reason,needs_review,final_score," +
                        "question:questions(question_number,question_text,max_marks)"
                )
            ) {
                filter { eq("submission_id", submissionId) }
            }
            .decodeList<ExtractedAnswerRow>()

        val correctAnswers = if (extractedAnswers.isEmpty()) {
            emptyMap()
        } else {
            supabase.postgrest.from("question_answers")
                .select(columns = Columns.list("question_id", "correct_answer")) {
                    filter { isIn("question_id", extractedAnswers.map { it.questionId }) }
                }
                .decodeList<QuestionAnswerLookupRow>()
                .associateBy { it.questionId }
        }

        SubmissionReviewDetail(
            submissionId = submission.id,
            studentName = submission.student.fullName,
            status = submission.status,
            questions = extractedAnswers
                .sortedBy { it.question.questionNumber }
                .map { row ->
                    QuestionReview(
                        extractedAnswerId = row.id,
                        questionId = row.questionId,
                        questionNumber = row.question.questionNumber,
                        questionText = row.question.questionText,
                        maxMarks = row.question.maxMarks,
                        correctAnswer = correctAnswers[row.questionId]?.correctAnswer,
                        rawText = row.rawText,
                        aiScore = row.aiScore,
                        confidence = row.confidence,
                        gradingReason = row.gradingReason,
                        needsReview = row.needsReview,
                        finalScore = row.finalScore
                    )
                }
        )
    }

    override suspend fun overrideQuestionScore(
        submissionId: String,
        extractedAnswerId: String,
        questionId: String,
        newScore: Double,
        reason: String,
        reviewerId: String
    ): Result<Unit> = runCatching {
        val current = supabase.postgrest.from("extracted_answers")
            .select(columns = Columns.list("ai_score")) {
                filter { eq("id", extractedAnswerId) }
            }
            .decodeSingle<AiScoreOnlyDto>()

        val now = Instant.now().toString()

        supabase.postgrest.from("extracted_answers").update(
            ExtractedAnswerFinalScoreDto(
                finalScore = newScore,
                reviewedBy = reviewerId,
                reviewedAt = now,
                needsReview = false
            )
        ) {
            filter { eq("id", extractedAnswerId) }
        }

        supabase.postgrest.from("grade_reviews").insert(
            GradeReviewInsertDto(
                submissionId = submissionId,
                questionId = questionId,
                originalAiScore = current.aiScore,
                finalScore = newScore,
                reviewedBy = reviewerId,
                reason = reason
            )
        )

        recomputeSubmissionAggregate(submissionId)
    }

    private suspend fun recomputeSubmissionAggregate(submissionId: String) {
        val rows = supabase.postgrest.from("extracted_answers")
            .select(columns = Columns.list("final_score", "ai_score", "max_score", "needs_review")) {
                filter { eq("submission_id", submissionId) }
            }
            .decodeList<SubmissionAggregateSourceRow>()

        if (rows.isEmpty()) return

        val totalMarks = rows.sumOf { it.maxScore }
        val earned = rows.sumOf { it.finalScore ?: it.aiScore ?: 0.0 }
        val percentage = if (totalMarks > 0) (earned / totalMarks) * 100.0 else 0.0
        val stillNeedsReview = rows.any { it.needsReview }
        val schoolId = supabase.postgrest.from("submissions")
            .select(columns = Columns.raw("exam:exams(school_id)")) {
                filter { eq("id", submissionId) }
            }
            .decodeSingle<SchoolIdLookupDto>()
            .exam.schoolId

        val grade = resolveGrade(schoolId, percentage)

        supabase.postgrest.from("submissions").update(
            SubmissionAggregateUpdateDto(
                finalScore = earned,
                totalMarks = totalMarks,
                percentage = percentage,
                grade = grade,
                status = if (stillNeedsReview) "needs_review" else "graded",
                needsReview = stillNeedsReview
            )
        ) {
            filter { eq("id", submissionId) }
        }
    }

    private suspend fun resolveGrade(schoolId: String, percentage: Double): String {
        val bands = runCatching {
            supabase.postgrest.from("grading_scales")
                .select(columns = Columns.list("bands")) {
                    filter {
                        eq("school_id", schoolId)
                        eq("is_default", true)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<GradingScaleRow>()
                ?.bands
        }.getOrNull()

        val effectiveBands = bands ?: DEFAULT_GRADE_BANDS
        return effectiveBands.firstOrNull { percentage >= it.min && percentage <= it.max }?.grade
            ?: effectiveBands.minByOrNull { it.min }?.grade
            ?: "-"
    }

    companion object {
        // Fallback only — a school-configured grading_scales row (spec
        // section 38) always takes precedence when one is marked default.
        private val DEFAULT_GRADE_BANDS = listOf(
            GradeBand(90.0, 100.0, "A+"),
            GradeBand(80.0, 89.99, "A"),
            GradeBand(70.0, 79.99, "B"),
            GradeBand(60.0, 69.99, "C"),
            GradeBand(50.0, 59.99, "D"),
            GradeBand(0.0, 49.99, "F")
        )
    }
}
