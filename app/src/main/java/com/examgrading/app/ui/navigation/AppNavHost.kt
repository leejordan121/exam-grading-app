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
import com.examgrading.app.ui.auth.AuthViewModel
import com.examgrading.app.ui.auth.LoginScreen
import com.examgrading.app.ui.auth.LoginUiState
import com.examgrading.app.ui.student.StudentHomeScreen
import com.examgrading.app.ui.student.examdetail.ExamDetailScreen
import com.examgrading.app.ui.student.submission.SubmissionScreen
import com.examgrading.app.ui.teacher.TeacherHomeScreen
import com.examgrading.app.ui.teacher.examcreate.ExamWizardScreen

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
                onOpenExam = { examId -> navController.navigate(Routes.ExamDetail.build(examId)) }
            )
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
            TeacherHomeScreen(onCreateExam = { navController.navigate(Routes.ExamWizard.route) })
        }
        composable(Routes.AdminHome.route) { AdminHomeScreen() }
        composable(Routes.ExamWizard.route) {
            ExamWizardScreen(
                onBack = { navController.popBackStack() },
                onPublished = {
                    navController.popBackStack(Routes.TeacherHome.route, inclusive = false)
                }
            )
        }
    }
}
