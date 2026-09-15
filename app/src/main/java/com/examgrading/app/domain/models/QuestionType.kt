package com.examgrading.app.domain.models

enum class QuestionType(val dbValue: String, val label: String) {
    MULTIPLE_CHOICE("multiple_choice", "Multiple Choice"),
    TRUE_FALSE("true_false", "True / False"),
    FILL_BLANK("fill_blank", "Fill in the Blank"),
    SHORT_ANSWER("short_answer", "Short Answer"),
    LONG_ANSWER("long_answer", "Long Answer / Essay"),
    MATHEMATICAL("mathematical", "Mathematics"),
    DIAGRAM("diagram", "Diagram / Labeling");

    companion object {
        fun fromDb(value: String): QuestionType = entries.first { it.dbValue == value }
    }
}

enum class GradingMode(val dbValue: String, val label: String) {
    AI("ai", "AI Graded"),
    MANUAL("manual", "Manual"),
    HYBRID("hybrid", "AI + Teacher Review");

    companion object {
        fun fromDb(value: String): GradingMode = entries.first { it.dbValue == value }
    }
}
