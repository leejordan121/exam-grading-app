package com.examgrading.app.ui.teacher.examcreate

import com.examgrading.app.domain.models.QuestionDraft
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.Subject

data class ExamWizardState(
    val step: Int = 0,
    val subjects: List<Subject> = emptyList(),
    val classes: List<SchoolClass> = emptyList(),
    val schoolId: String? = null,
    val teacherId: String? = null,
    val examId: String? = null,

    val title: String = "",
    val description: String = "",
    val instructions: String = "",
    val subjectId: String? = null,
    val classId: String? = null,
    val examDate: String = "",
    val durationMinutes: String = "",

    val paperFileName: String? = null,
    val answerKeyFileName: String? = null,

    val questions: List<QuestionDraft> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null,
    val published: Boolean = false
) {
    val basicInfoValid: Boolean
        get() = title.isNotBlank() && subjectId != null && classId != null

    val canGoToQuestions: Boolean
        get() = examId != null && paperFileName != null && answerKeyFileName != null
}

val WIZARD_STEP_TITLES = listOf(
    "Basic Info",
    "Exam Paper",
    "Answer Key",
    "Questions",
    "Review & Publish"
)
