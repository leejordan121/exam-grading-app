package com.examgrading.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.examgrading.app.domain.models.UserRole
import com.examgrading.app.ui.admin.AdminHomeScreen
import com.examgrading.app.ui.auth.AuthViewModel
import com.examgrading.app.ui.auth.LoginScreen
import com.examgrading.app.ui.auth.LoginUiState
import com.examgrading.app.ui.student.StudentHomeScreen
import com.examgrading.app.ui.teacher.TeacherHomeScreen

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
        composable(Routes.StudentHome.route) { StudentHomeScreen() }
        composable(Routes.TeacherHome.route) { TeacherHomeScreen() }
        composable(Routes.AdminHome.route) { AdminHomeScreen() }
    }
}
