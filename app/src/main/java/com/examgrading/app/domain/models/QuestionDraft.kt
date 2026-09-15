package com.examgrading.app.domain.models

data class QuestionDraft(
    val localId: String, // client-side identity for list editing, not a DB id
    val questionNumber: Int,
    val questionText: String,
    val questionType: QuestionType,
    val maxMarks: String,
    val correctAnswer: String,
    val gradingMode: GradingMode = GradingMode.AI
)
