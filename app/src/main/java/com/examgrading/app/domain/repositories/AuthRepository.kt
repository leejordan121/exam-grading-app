package com.examgrading.app.domain.repositories

import com.examgrading.app.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isAuthenticated: Flow<Boolean>

    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun getCurrentProfile(): Result<UserProfile>
}
