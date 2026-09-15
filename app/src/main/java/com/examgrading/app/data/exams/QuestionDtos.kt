package com.examgrading.app.data.exams

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionInsertDto(
    @SerialName("exam_id") val examId: String,
    @SerialName("question_number") val questionNumber: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_type") val questionType: String,
    @SerialName("max_marks") val maxMarks: Double,
    @SerialName("grading_mode") val gradingMode: String
)

@Serializable
data class QuestionIdDto(val id: String)

@Serializable
data class QuestionMarksDto(
    val id: String,
    @SerialName("max_marks") val maxMarks: Double
)

@Serializable
data class QuestionAnswerInsertDto(
    @SerialName("question_id") val questionId: String,
    @SerialName("correct_answer") val correctAnswer: String
)
