package com.examgrading.app.data.auth

import com.examgrading.app.domain.models.UserProfile
import com.examgrading.app.domain.repositories.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> =
        supabase.auth.sessionStatus.map { it is SessionStatus.Authenticated }

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun getCurrentProfile(): Result<UserProfile> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated user")

        supabase.postgrest.from("profiles")
            .select(columns = Columns.ALL) {
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileDto>()
            .toDomain()
    }
}
