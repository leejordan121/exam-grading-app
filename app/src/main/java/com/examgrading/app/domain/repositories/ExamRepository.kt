package com.examgrading.app.domain.repositories

import com.examgrading.app.domain.models.QuestionDraft
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.Subject

interface ExamRepository {
    suspend fun getSubjects(schoolId: String): Result<List<Subject>>
    suspend fun getClasses(schoolId: String): Result<List<SchoolClass>>

    /** Creates a draft exam row and returns its id. */
    suspend fun createDraftExam(
        schoolId: String,
        teacherId: String,
        title: String,
        description: String,
        instructions: String,
        subjectId: String,
        examDate: String?,
        durationMinutes: Int?
    ): Result<String>

    suspend fun updateExamBasicInfo(
        examId: String,
        title: String,
        description: String,
        instructions: String,
        subjectId: String,
        examDate: String?,
        durationMinutes: Int?
    ): Result<Unit>

    /** Uploads the exam paper to the exam-papers bucket and records the path on the exam. */
    suspend fun uploadExamPaper(
        schoolId: String,
        examId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String>

    /** Uploads the official answer key to the answer-keys bucket (never student-readable). */
    suspend fun uploadAnswerKey(
        schoolId: String,
        examId: String,
        teacherId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String>

    /** Replaces all questions (and their protected answers) for this exam. */
    suspend fun saveQuestions(examId: String, questions: List<QuestionDraft>): Result<Unit>

    /**
     * Validates the exam is ready (spec section 80), assigns it to every
     * student in [classId], and flips status draft -> published.
     */
    suspend fun publishExam(examId: String, classId: String): Result<Unit>
}
