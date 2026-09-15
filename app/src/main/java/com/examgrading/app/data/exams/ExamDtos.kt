package com.examgrading.app.data.exams

import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.Subject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val id: String,
    val name: String,
    val code: String? = null
)

fun SubjectDto.toDomain() = Subject(id = id, name = name, code = code)

@Serializable
data class ClassDto(
    val id: String,
    val name: String,
    val grade: String? = null,
    val section: String? = null
)

fun ClassDto.toDomain() = SchoolClass(id = id, name = name, grade = grade, section = section)

@Serializable
data class ClassStudentDto(
    @SerialName("student_id") val studentId: String
)

@Serializable
data class ExamInsertDto(
    @SerialName("school_id") val schoolId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    val description: String,
    val instructions: String,
    @SerialName("exam_date") val examDate: String?,
    @SerialName("duration_minutes") val durationMinutes: Int?,
    @SerialName("total_marks") val totalMarks: Double = 0.0,
    val status: String = "draft"
)

@Serializable
data class ExamIdDto(val id: String)

@Serializable
data class ExamBasicInfoUpdateDto(
    @SerialName("subject_id") val subjectId: String,
    val title: String,
    val description: String,
    val instructions: String,
    @SerialName("exam_date") val examDate: String?,
    @SerialName("duration_minutes") val durationMinutes: Int?
)

@Serializable
data class ExamPaperPathUpdateDto(@SerialName("paper_file_path") val paperFilePath: String)

@Serializable
data class AnswerKeyInsertDto(
    @SerialName("exam_id") val examId: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("uploaded_by") val uploadedBy: String
)

@Serializable
data class ExamValidationRow(
    val id: String,
    val title: String,
    @SerialName("subject_id") val subjectId: String?,
    @SerialName("paper_file_path") val paperFilePath: String?,
    val status: String
)

@Serializable
data class ExamTotalMarksUpdateDto(
    @SerialName("total_marks") val totalMarks: Double,
    val status: String,
    @SerialName("published_at") val publishedAt: String
)

@Serializable
data class ExamAssignmentInsertDto(
    @SerialName("exam_id") val examId: String,
    @SerialName("student_id") val studentId: String
)
