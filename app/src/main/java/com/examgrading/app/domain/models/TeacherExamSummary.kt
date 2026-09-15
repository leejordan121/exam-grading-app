package com.examgrading.app.domain.models

data class TeacherExamSummary(
    val examId: String,
    val title: String,
    val subjectName: String?,
    val examDate: String?,
    val status: String,
    val assignedCount: Int,
    val submittedCount: Int,
    val gradedCount: Int,
    val needsReviewCount: Int
)
