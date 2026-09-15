package com.examgrading.app.domain.models

data class SubmissionListItem(
    val submissionId: String,
    val studentName: String,
    val status: String,
    val finalScore: Double?,
    val totalMarks: Double?,
    val percentage: Double?,
    val needsReview: Boolean
)

data class SubmissionReviewDetail(
    val submissionId: String,
    val studentName: String,
    val status: String,
    val questions: List<QuestionReview>
)

data class QuestionReview(
    val extractedAnswerId: String,
    val questionId: String,
    val questionNumber: Int,
    val questionText: String,
    val maxMarks: Double,
    val correctAnswer: String?,
    val rawText: String?,
    val aiScore: Double?,
    val confidence: Double?,
    val gradingReason: String?,
    val needsReview: Boolean,
    val finalScore: Double?
)
