package com.examgrading.app.data.exams

import com.examgrading.app.domain.models.QuestionDraft
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.Subject
import com.examgrading.app.core.network.ensureValidSession
import com.examgrading.app.domain.repositories.ExamRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ExamRepository {

    override suspend fun getSubjects(schoolId: String): Result<List<Subject>> = runCatching {
        supabase.postgrest.from("subjects")
            .select(columns = Columns.list("id", "name", "code")) {
                filter { eq("school_id", schoolId) }
                order("name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList<SubjectDto>()
            .map { it.toDomain() }
    }

    override suspend fun getClasses(schoolId: String): Result<List<SchoolClass>> = runCatching {
        supabase.postgrest.from("classes")
            .select(columns = Columns.list("id", "name", "grade", "section")) {
                filter { eq("school_id", schoolId) }
                order("name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }
            .decodeList<ClassDto>()
            .map { it.toDomain() }
    }

    override suspend fun createDraftExam(
        schoolId: String,
        teacherId: String,
        title: String,
        description: String,
        instructions: String,
        subjectId: String,
        examDate: String?,
        durationMinutes: Int?
    ): Result<String> = runCatching {
        val dto = ExamInsertDto(
            schoolId = schoolId,
            teacherId = teacherId,
            subjectId = subjectId,
            title = title,
            description = description,
            instructions = instructions,
            examDate = examDate,
            durationMinutes = durationMinutes
        )
        supabase.postgrest.from("exams")
            .insert(dto) { select(columns = Columns.list("id")) }
            .decodeSingle<ExamIdDto>()
            .id
    }

    override suspend fun updateExamBasicInfo(
        examId: String,
        title: String,
        description: String,
        instructions: String,
        subjectId: String,
        examDate: String?,
        durationMinutes: Int?
    ): Result<Unit> = runCatching {
        val dto = ExamBasicInfoUpdateDto(
            subjectId = subjectId,
            title = title,
            description = description,
            instructions = instructions,
            examDate = examDate,
            durationMinutes = durationMinutes
        )
        supabase.postgrest.from("exams").update(dto) {
            filter { eq("id", examId) }
        }
        Unit
    }

    override suspend fun uploadExamPaper(
        schoolId: String,
        examId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String> = runCatching {
        supabase.ensureValidSession()
        val extension = fileName.substringAfterLast('.', "pdf")
        val path = "$schoolId/$examId/paper.$extension"
        supabase.storage.from("exam-papers").upload(path, bytes) { upsert = true }
        supabase.postgrest.from("exams").update(ExamPaperPathUpdateDto(paperFilePath = path)) {
            filter { eq("id", examId) }
        }
        path
    }

    override suspend fun uploadAnswerKey(
        schoolId: String,
        examId: String,
        teacherId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String> = runCatching {
        supabase.ensureValidSession()
        val extension = fileName.substringAfterLast('.', "pdf")
        val path = "$schoolId/$examId/answer-key.$extension"
        supabase.storage.from("answer-keys").upload(path, bytes) { upsert = true }
        supabase.postgrest.from("answer_keys").insert(
            AnswerKeyInsertDto(examId = examId, filePath = path, uploadedBy = teacherId)
        )
        path
    }

    override suspend fun saveQuestions(examId: String, questions: List<QuestionDraft>): Result<Unit> = runCatching {
        // Draft-only exam: no submissions can exist yet, so a clean
        // replace is safe and keeps the wizard's "edit questions" step simple.
        supabase.postgrest.from("questions").delete {
            filter { eq("exam_id", examId) }
        }

        val insertedIds = supabase.postgrest.from("questions")
            .insert(
                questions.map {
                    QuestionInsertDto(
                        examId = examId,
                        questionNumber = it.questionNumber,
                        questionText = it.questionText,
                        questionType = it.questionType.dbValue,
                        maxMarks = it.maxMarks.toDoubleOrNull() ?: 0.0,
                        gradingMode = it.gradingMode.dbValue
                    )
                }
            ) { select(columns = Columns.list("id")) }
            .decodeList<QuestionIdDto>()

        val answerRows = insertedIds.zip(questions).map { (idDto, draft) ->
            QuestionAnswerInsertDto(questionId = idDto.id, correctAnswer = draft.correctAnswer)
        }
        if (answerRows.isNotEmpty()) {
            supabase.postgrest.from("question_answers").insert(answerRows)
        }
        Unit
    }

    override suspend fun publishExam(examId: String, classId: String): Result<Unit> = runCatching {
        val exam = supabase.postgrest.from("exams")
            .select(columns = Columns.list("id", "title", "subject_id", "paper_file_path", "status")) {
                filter { eq("id", examId) }
            }
            .decodeSingle<ExamValidationRow>()

        require(exam.title.isNotBlank()) { "Exam title is required." }
        require(exam.subjectId != null) { "Select a subject before publishing." }
        require(exam.paperFilePath != null) { "Upload the exam paper before publishing." }
        require(exam.status == "draft") { "Only a draft exam can be published." }

        val answerKeyCount = supabase.postgrest.from("answer_keys")
            .select(columns = Columns.list("id")) { filter { eq("exam_id", examId) } }
            .decodeList<QuestionIdDto>()
        require(answerKeyCount.isNotEmpty()) { "Upload the official answer key before publishing." }

        val questions = supabase.postgrest.from("questions")
            .select(columns = Columns.list("id", "max_marks")) { filter { eq("exam_id", examId) } }
            .decodeList<QuestionMarksDto>()
        require(questions.isNotEmpty()) { "Add at least one question before publishing." }
        val totalMarks = questions.sumOf { it.maxMarks }

        val students = supabase.postgrest.from("class_students")
            .select(columns = Columns.list("student_id")) { filter { eq("class_id", classId) } }
            .decodeList<ClassStudentDto>()
        require(students.isNotEmpty()) { "Assign at least one student/class before publishing." }

        supabase.postgrest.from("exam_assignments").upsert(
            students.map { ExamAssignmentInsertDto(examId = examId, studentId = it.studentId) }
        ) {
            onConflict = "exam_id,student_id"
        }

        supabase.postgrest.from("exams").update(
            ExamTotalMarksUpdateDto(
                totalMarks = totalMarks,
                status = "published",
                publishedAt = Instant.now().toString()
            )
        ) {
            filter { eq("id", examId) }
        }
        Unit
    }
}
