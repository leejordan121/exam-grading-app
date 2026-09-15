package com.examgrading.app.domain.repositories

import com.examgrading.app.domain.models.SubmissionListItem
import com.examgrading.app.domain.models.SubmissionReviewDetail
import com.examgrading.app.domain.models.TeacherExamSummary

interface TeacherSubmissionsRepository {
    suspend fun getMyExams(teacherId: String): Result<List<TeacherExamSummary>>
    suspend fun getExamSubmissions(examId: String): Result<List<SubmissionListItem>>
    suspend fun getSubmissionReview(submissionId: String): Result<SubmissionReviewDetail>

    /**
     * Teacher override for one question's score (spec section 36): the
     * original AI score is never overwritten, only the review trail and
     * the submission's aggregate score are updated.
     */
    suspend fun overrideQuestionScore(
        submissionId: String,
        extractedAnswerId: String,
        questionId: String,
        newScore: Double,
        reason: String,
        reviewerId: String
    ): Result<Unit>
}
