package com.examgrading.app.ui.navigation

sealed class Routes(val route: String) {
    data object Login : Routes("login")
    data object StudentHome : Routes("student_home")
    data object TeacherHome : Routes("teacher_home")
    data object AdminHome : Routes("admin_home")
}
