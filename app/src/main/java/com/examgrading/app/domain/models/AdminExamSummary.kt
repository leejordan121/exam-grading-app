package com.examgrading.app.domain.models

data class AdminExamSummary(
    val examId: String,
    val title: String,
    val subjectName: String?,
    val teacherName: String?,
    val examDate: String?,
    val status: String
)
