package com.examgrading.app.domain.models

data class ExamSummary(
    val examId: String,
    val title: String,
    val subjectName: String?,
    val examDate: String?,
    val durationMinutes: Int?,
    val totalMarks: Double,
    val examStatus: String, // published / closed / grading / completed
    val assignmentStatus: String // assigned / downloaded / submitted / graded
)

data class ExamDetail(
    val examId: String,
    val title: String,
    val description: String?,
    val instructions: String?,
    val subjectName: String?,
    val examDate: String?,
    val durationMinutes: Int?,
    val totalMarks: Double,
    val paperFilePath: String?
)
