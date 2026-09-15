package com.examgrading.app.ui.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object StudentHome : Routes("student_home")
    data object TeacherHome : Routes("teacher_home")
    data object AdminHome : Routes("admin_home")
    data object ExamWizard : Routes("exam_wizard")
    data object ExamDetail : Routes("exam_detail/{examId}") {
        fun build(examId: String) = "exam_detail/$examId"
    }
    data object Submission : Routes("submission/{examId}") {
        fun build(examId: String) = "submission/$examId"
    }
    data object ExamSubmissions : Routes("exam_submissions/{examId}") {
        fun build(examId: String) = "exam_submissions/$examId"
    }
    data object SubmissionReview : Routes("submission_review/{submissionId}") {
        fun build(submissionId: String) = "submission_review/$submissionId"
    }
}
