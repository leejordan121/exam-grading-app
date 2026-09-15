package com.examgrading.app.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The SDK's default SettingsSessionManager relies on multiplatform-settings
 * picking up an Android Context automatically, which isn't reliable in every
 * setup and was the root cause of the app losing its session on process
 * death (e.g. after backgrounding for the system file/camera picker, which
 * this app does constantly). This talks to SharedPreferences directly so
 * there's no ambiguity about whether persistence is actually wired up.
 */
@Singleton
class AndroidSessionManager @Inject constructor(
    @ApplicationContext context: Context
) : SessionManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit { putString(KEY, json.encodeToString(session)) }
    }

    override suspend fun loadSession(): UserSession? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    override suspend fun deleteSession() {
        prefs.edit { remove(KEY) }
    }

    private companion object {
        const val KEY = "session"
    }
}
