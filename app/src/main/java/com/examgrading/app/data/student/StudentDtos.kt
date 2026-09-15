package com.examgrading.app.data.student

import com.examgrading.app.domain.models.ExamDetail
import com.examgrading.app.domain.models.ExamSummary
import com.examgrading.app.domain.models.StudentSubmission
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectEmbedDto(val name: String? = null)

@Serializable
data class ExamEmbedDto(
    val id: String,
    val title: String,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    val status: String,
    val subject: SubjectEmbedDto? = null
)

@Serializable
data class AssignedExamRow(
    val status: String,
    val exam: ExamEmbedDto
)

fun AssignedExamRow.toDomain() = ExamSummary(
    examId = exam.id,
    title = exam.title,
    subjectName = exam.subject?.name,
    examDate = exam.examDate,
    durationMinutes = exam.durationMinutes,
    totalMarks = exam.totalMarks,
    examStatus = exam.status,
    assignmentStatus = status
)

@Serializable
data class ExamDetailRow(
    val id: String,
    val title: String,
    val description: String? = null,
    val instructions: String? = null,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    @SerialName("paper_file_path") val paperFilePath: String? = null,
    val subject: SubjectEmbedDto? = null
)

fun ExamDetailRow.toDomain() = ExamDetail(
    examId = id,
    title = title,
    description = description,
    instructions = instructions,
    subjectName = subject?.name,
    examDate = examDate,
    durationMinutes = durationMinutes,
    totalMarks = totalMarks,
    paperFilePath = paperFilePath
)

@Serializable
data class SubmissionIdDto(val id: String)

@Serializable
data class SubmissionStatusDto(
    val id: String,
    val status: String
)

@Serializable
data class SubmissionInsertDto(
    @SerialName("exam_id") val examId: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("attempt_number") val attemptNumber: Int = 1,
    val status: String = "draft"
)

@Serializable
data class SubmissionSubmitUpdateDto(
    val status: String,
    @SerialName("submitted_at") val submittedAt: String
)

@Serializable
data class SubmissionPageInsertDto(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("page_number") val pageNumber: Int,
    @SerialName("file_path") val filePath: String
)

@Serializable
data class SubmissionPageIdDto(val id: String)

@Serializable
data class MySubmissionRow(
    val id: String,
    val status: String,
    @SerialName("final_score") val finalScore: Double? = null,
    @SerialName("total_marks") val totalMarks: Double? = null,
    val percentage: Double? = null,
    val grade: String? = null,
    @SerialName("result_visible") val resultVisible: Boolean = false,
    val exam: ExamTitleEmbedDto
)

@Serializable
data class ExamTitleEmbedDto(val title: String)

fun MySubmissionRow.toDomain() = StudentSubmission(
    id = id,
    examId = "",
    examTitle = exam.title,
    status = status,
    finalScore = finalScore,
    totalMarks = totalMarks,
    percentage = percentage,
    grade = grade,
    resultVisible = resultVisible
)
