package com.examgrading.app.data.teacher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectNameDto(val name: String? = null)

@Serializable
data class StudentNameDto(@SerialName("full_name") val fullName: String)

@Serializable
data class TeacherExamRow(
    val id: String,
    val title: String,
    @SerialName("exam_date") val examDate: String? = null,
    val status: String,
    val subject: SubjectNameDto? = null
)

@Serializable
data class ExamIdOnlyDto(@SerialName("exam_id") val examId: String)

@Serializable
data class SubmissionCountRow(
    @SerialName("exam_id") val examId: String,
    val status: String,
    @SerialName("needs_review") val needsReview: Boolean = false
)

@Serializable
data class SubmissionRow(
    val id: String,
    val status: String,
    @SerialName("final_score") val finalScore: Double? = null,
    @SerialName("total_marks") val totalMarks: Double? = null,
    val percentage: Double? = null,
    @SerialName("needs_review") val needsReview: Boolean = false,
    val student: StudentNameDto
)

@Serializable
data class SubmissionDetailRow(
    val id: String,
    val status: String,
    val student: StudentNameDto
)

@Serializable
data class QuestionEmbedDto(
    @SerialName("question_number") val questionNumber: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("max_marks") val maxMarks: Double
)

@Serializable
data class ExtractedAnswerRow(
    val id: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("ai_score") val aiScore: Double? = null,
    @SerialName("max_score") val maxScore: Double,
    val confidence: Double? = null,
    @SerialName("grading_reason") val gradingReason: String? = null,
    @SerialName("needs_review") val needsReview: Boolean = false,
    @SerialName("final_score") val finalScore: Double? = null,
    val question: QuestionEmbedDto
)

@Serializable
data class QuestionAnswerLookupRow(
    @SerialName("question_id") val questionId: String,
    @SerialName("correct_answer") val correctAnswer: String? = null
)

@Serializable
data class ExtractedAnswerFinalScoreDto(
    @SerialName("final_score") val finalScore: Double,
    @SerialName("reviewed_by") val reviewedBy: String,
    @SerialName("reviewed_at") val reviewedAt: String,
    @SerialName("needs_review") val needsReview: Boolean = false
)

@Serializable
data class GradeReviewInsertDto(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("original_ai_score") val originalAiScore: Double?,
    @SerialName("final_score") val finalScore: Double,
    @SerialName("reviewed_by") val reviewedBy: String,
    val reason: String
)

@Serializable
data class SubmissionAggregateSourceRow(
    @SerialName("final_score") val finalScore: Double? = null,
    @SerialName("ai_score") val aiScore: Double? = null,
    @SerialName("max_score") val maxScore: Double,
    @SerialName("needs_review") val needsReview: Boolean = false
)

@Serializable
data class SubmissionAggregateUpdateDto(
    @SerialName("final_score") val finalScore: Double,
    @SerialName("total_marks") val totalMarks: Double,
    val percentage: Double,
    val grade: String,
    val status: String,
    @SerialName("needs_review") val needsReview: Boolean
)

@Serializable
data class GradeBand(val min: Double, val max: Double, val grade: String)

@Serializable
data class GradingScaleRow(val bands: List<GradeBand>)

@Serializable
data class AiScoreOnlyDto(@SerialName("ai_score") val aiScore: Double? = null)

@Serializable
data class SchoolIdEmbedDto(@SerialName("school_id") val schoolId: String)

@Serializable
data class SchoolIdLookupDto(val exam: SchoolIdEmbedDto)
