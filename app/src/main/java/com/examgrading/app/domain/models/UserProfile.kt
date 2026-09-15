package com.examgrading.app.domain.models

data class UserProfile(
    val id: String,
    val schoolId: String,
    val role: UserRole,
    val fullName: String,
    val email: String,
    val avatarUrl: String?
)
