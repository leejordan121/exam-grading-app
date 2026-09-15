package com.examgrading.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.examgrading.app.domain.models.UserRole
import com.examgrading.app.ui.admin.AdminHomeScreen
import com.examgrading.app.ui.admin.classes.ClassesScreen
import com.examgrading.app.ui.admin.exams.AdminExamsScreen
import com.examgrading.app.ui.admin.people.PeopleScreen
import com.examgrading.app.ui.admin.subjects.SubjectsScreen
import com.examgrading.app.ui.auth.AuthViewModel
import com.examgrading.app.ui.auth.LoginScreen
import com.examgrading.app.ui.auth.LoginUiState
import com.examgrading.app.ui.student.StudentHomeScreen
import com.examgrading.app.ui.student.examdetail.ExamDetailScreen
import com.examgrading.app.ui.student.result.ResultDetailScreen
import com.examgrading.app.ui.student.submission.SubmissionScreen
import com.examgrading.app.ui.teacher.TeacherHomeScreen
import com.examgrading.app.ui.teacher.examcreate.ExamWizardScreen
import com.examgrading.app.ui.teacher.submissions.ExamSubmissionsScreen
import com.examgrading.app.ui.teacher.submissions.SubmissionReviewScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Login.route) {
        composable(Routes.Login.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState) {
                val state = uiState
                if (state is LoginUiState.Success) {
                    val destination = when (state.profile.role) {
                        UserRole.ADMIN -> Routes.AdminHome.route
                        UserRole.TEACHER -> Routes.TeacherHome.route
                        UserRole.STUDENT -> Routes.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(uiState = uiState, onSignIn = viewModel::signIn)
        }
        composable(Routes.StudentHome.route) {
            StudentHomeScreen(
                onOpenExam = { examId -> navController.navigate(Routes.ExamDetail.build(examId)) },
                onOpenResult = { submissionId -> navController.navigate(Routes.ResultDetail.build(submissionId)) }
            )
        }
        composable(
            Routes.ResultDetail.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) {
            ResultDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.ExamDetail.route,
            arguments = listOf(navArgument("examId") { type = NavType.StringType })
        ) {
            ExamDetailScreen(
                onBack = { navController.popBackStack() },
                onStartSubmission = { examId -> navController.navigate(Routes.Submission.build(examId)) }
            )
        }
        composable(
            Routes.Submission.route,
            arguments = listOf(navArgument("examId") { type = NavType.StringType })
        ) {
            SubmissionScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.popBackStack(Routes.StudentHome.route, inclusive = false)
                }
            )
        }
        composable(Routes.TeacherHome.route) {
            TeacherHomeScreen(
                onCreateExam = { navController.navigate(Routes.ExamWizard.route) },
                onOpenExam = { examId -> navController.navigate(Routes.ExamSubmissions.build(examId)) }
            )
        }
        composable(Routes.AdminHome.route) {
            AdminHomeScreen(
                onOpenExams = { navController.navigate(Routes.AdminExams.route) },
                onOpenSubjects = { navController.navigate(Routes.AdminSubjects.route) },
                onOpenClasses = { navController.navigate(Routes.AdminClasses.route) },
                onOpenTeachers = { navController.navigate(Routes.AdminPeople.build("teacher")) },
                onOpenStudents = { navController.navigate(Routes.AdminPeople.build("student")) }
            )
        }
        composable(Routes.AdminExams.route) {
            AdminExamsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.AdminSubjects.route) {
            SubjectsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.AdminClasses.route) {
            ClassesScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.AdminPeople.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) {
            PeopleScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ExamWizard.route) {
            ExamWizardScreen(
                onBack = { navController.popBackStack() },
                onPublished = {
                    navController.popBackStack(Routes.TeacherHome.route, inclusive = false)
                }
            )
        }
        composable(
            Routes.ExamSubmissions.route,
            arguments = listOf(navArgument("examId") { type = NavType.StringType })
        ) {
            ExamSubmissionsScreen(
                onBack = { navController.popBackStack() },
                onOpenSubmission = { submissionId ->
                    navController.navigate(Routes.SubmissionReview.build(submissionId))
                }
            )
        }
        composable(
            Routes.SubmissionReview.route,
            arguments = listOf(navArgument("submissionId") { type = NavType.StringType })
        ) {
            SubmissionReviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
