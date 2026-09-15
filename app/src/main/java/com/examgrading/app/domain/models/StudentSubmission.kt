package com.examgrading.app.domain.models

data class StudentSubmission(
    val id: String,
    val examId: String,
    val examTitle: String,
    val status: String,
    val finalScore: Double?,
    val totalMarks: Double?,
    val percentage: Double?,
    val grade: String?,
    val resultVisible: Boolean
)
