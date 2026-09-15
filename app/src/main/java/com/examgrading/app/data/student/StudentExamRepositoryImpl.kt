package com.examgrading.app.data.student

import com.examgrading.app.core.network.ensureValidSession
import com.examgrading.app.domain.models.ExamDetail
import com.examgrading.app.domain.models.ExamSummary
import com.examgrading.app.domain.models.ResultQuestionRow
import com.examgrading.app.domain.models.StudentSubmission
import com.examgrading.app.domain.models.SubmissionResultDetail
import com.examgrading.app.domain.repositories.StudentExamRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class StudentExamRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : StudentExamRepository {

    override suspend fun getAssignedExams(studentId: String): Result<List<ExamSummary>> = runCatching {
        supabase.postgrest.from("exam_assignments")
            .select(
                columns = Columns.raw(
                    "status,exam:exams(id,title,exam_date,duration_minutes,total_marks,status,subject:subjects(name))"
                )
            ) {
                filter { eq("student_id", studentId) }
            }
            .decodeList<AssignedExamRow>()
            .map { it.toDomain() }
    }

    override suspend fun getExamDetail(examId: String): Result<ExamDetail> = runCatching {
        supabase.postgrest.from("exams")
            .select(
                columns = Columns.raw(
                    "id,title,description,instructions,exam_date,duration_minutes,total_marks,paper_file_path,subject:subjects(name)"
                )
            ) {
                filter { eq("id", examId) }
            }
            .decodeSingle<ExamDetailRow>()
            .toDomain()
    }

    override suspend fun getPaperDownloadUrl(paperFilePath: String): Result<String> = runCatching {
        supabase.storage.from("exam-papers").createSignedUrl(paperFilePath, 1.hours)
    }

    override suspend fun getOrCreateDraftSubmission(examId: String, studentId: String): Result<String> = runCatching {
        val existing = supabase.postgrest.from("submissions")
            .select(columns = Columns.list("id", "status")) {
                filter {
                    eq("exam_id", examId)
                    eq("student_id", studentId)
                    eq("attempt_number", 1)
                }
            }
            .decodeList<SubmissionStatusDto>()
            .firstOrNull()

        when {
            existing == null -> supabase.postgrest.from("submissions")
                .insert(SubmissionInsertDto(examId = examId, studentId = studentId)) {
                    select(columns = Columns.list("id"))
                }
                .decodeSingle<SubmissionIdDto>()
                .id

            existing.status in setOf("draft", "uploading") -> existing.id

            else -> error("You have already submitted this exam.")
        }
    }

    override suspend fun uploadSubmissionPage(
        schoolId: String,
        submissionId: String,
        pageNumber: Int,
        bytes: ByteArray
    ): Result<Unit> = runCatching {
        supabase.ensureValidSession()
        val path = "$schoolId/$submissionId/page-$pageNumber.jpg"
        supabase.storage.from("student-submissions").upload(path, bytes) { upsert = true }
        supabase.postgrest.from("submission_pages").upsert(
            SubmissionPageInsertDto(submissionId = submissionId, pageNumber = pageNumber, filePath = path)
        ) {
            onConflict = "submission_id,page_number"
        }
        Unit
    }

    override suspend fun deleteSubmissionPage(
        schoolId: String,
        submissionId: String,
        pageNumber: Int
    ): Result<Unit> = runCatching {
        val path = "$schoolId/$submissionId/page-$pageNumber.jpg"
        supabase.storage.from("student-submissions").delete(path)
        supabase.postgrest.from("submission_pages").delete {
            filter {
                eq("submission_id", submissionId)
                eq("page_number", pageNumber)
            }
        }
        Unit
    }

    override suspend fun getSubmissionPageCount(submissionId: String): Result<Int> = runCatching {
        supabase.postgrest.from("submission_pages")
            .select(columns = Columns.list("id")) {
                filter { eq("submission_id", submissionId) }
            }
            .decodeList<SubmissionPageIdDto>()
            .size
    }

    override suspend fun submitExam(submissionId: String): Result<Unit> = runCatching {
        supabase.postgrest.from("submissions").update(
            SubmissionSubmitUpdateDto(status = "uploaded", submittedAt = Instant.now().toString())
        ) {
            filter { eq("id", submissionId) }
        }
        Unit
    }

    override suspend fun getSubmissionResultDetail(submissionId: String): Result<SubmissionResultDetail> = runCatching {
        val submission = supabase.postgrest.from("submissions")
            .select(
                columns = Columns.raw(
                    "status,final_score,total_marks,percentage,grade,result_visible," +
                        "exam:exams(title,show_question_breakdown,show_ai_feedback)"
                )
            ) {
                filter { eq("id", submissionId) }
            }
            .decodeSingle<SubmissionResultRow>()

        val questions = if (submission.resultVisible && submission.exam.showQuestionBreakdown) {
            supabase.postgrest.from("extracted_answers")
                .select(
                    columns = Columns.raw(
                        "final_score,grading_reason,question:questions(question_number,question_text,max_marks)"
                    )
                ) {
                    filter { eq("submission_id", submissionId) }
                }
                .decodeList<ResultExtractedAnswerRow>()
                .sortedBy { it.question.questionNumber }
                .map {
                    ResultQuestionRow(
                        questionNumber = it.question.questionNumber,
                        questionText = it.question.questionText,
                        score = it.finalScore,
                        maxMarks = it.question.maxMarks,
                        feedback = if (submission.exam.showAiFeedback) it.gradingReason else null
                    )
                }
        } else {
            emptyList()
        }

        SubmissionResultDetail(
            examTitle = submission.exam.title,
            status = submission.status,
            finalScore = submission.finalScore,
            totalMarks = submission.totalMarks,
            percentage = submission.percentage,
            grade = submission.grade,
            resultVisible = submission.resultVisible,
            showBreakdown = submission.exam.showQuestionBreakdown,
            questions = questions
        )
    }

    override suspend fun triggerGrading(submissionId: String): Result<Unit> = runCatching {
        supabase.ensureValidSession()
        supabase.functions.invoke("grade-submission", body = GradeSubmissionRequest(submissionId))
        Unit
    }

    override suspend fun getMySubmissions(studentId: String): Result<List<StudentSubmission>> = runCatching {
        supabase.postgrest.from("submissions")
            .select(
                columns = Columns.raw(
                    "id,status,final_score,total_marks,percentage,grade,result_visible,exam:exams(title)"
                )
            ) {
                filter { eq("student_id", studentId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<MySubmissionRow>()
            .map { it.toDomain() }
    }
}
