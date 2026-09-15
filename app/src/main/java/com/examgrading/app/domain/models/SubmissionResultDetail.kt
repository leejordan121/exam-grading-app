package com.examgrading.app.domain.models

data class SubmissionResultDetail(
    val examTitle: String,
    val status: String,
    val finalScore: Double?,
    val totalMarks: Double?,
    val percentage: Double?,
    val grade: String?,
    val resultVisible: Boolean,
    val showBreakdown: Boolean,
    val questions: List<ResultQuestionRow>
)

data class ResultQuestionRow(
    val questionNumber: Int,
    val questionText: String,
    val score: Double?,
    val maxMarks: Double,
    val feedback: String?
)
