package com.examgrading.app.domain.repositories

import com.examgrading.app.domain.models.ExamDetail
import com.examgrading.app.domain.models.ExamSummary
import com.examgrading.app.domain.models.StudentSubmission
import com.examgrading.app.domain.models.SubmissionResultDetail

interface StudentExamRepository {
    suspend fun getAssignedExams(studentId: String): Result<List<ExamSummary>>
    suspend fun getExamDetail(examId: String): Result<ExamDetail>
    suspend fun getPaperDownloadUrl(paperFilePath: String): Result<String>

    /** Resumes an in-progress submission or creates a fresh draft. */
    suspend fun getOrCreateDraftSubmission(examId: String, studentId: String): Result<String>

    suspend fun uploadSubmissionPage(
        schoolId: String,
        submissionId: String,
        pageNumber: Int,
        bytes: ByteArray
    ): Result<Unit>

    suspend fun deleteSubmissionPage(schoolId: String, submissionId: String, pageNumber: Int): Result<Unit>
    suspend fun getSubmissionPageCount(submissionId: String): Result<Int>

    /** Marks the submission uploaded and ready for the grading pipeline. */
    suspend fun submitExam(submissionId: String): Result<Unit>

    /** Invokes the grade-submission Edge Function. Safe to call more than once. */
    suspend fun triggerGrading(submissionId: String): Result<Unit>

    suspend fun getMySubmissions(studentId: String): Result<List<StudentSubmission>>

    suspend fun getSubmissionResultDetail(submissionId: String): Result<SubmissionResultDetail>
}
