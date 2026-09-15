package com.examgrading.app.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/**
 * Storage/Functions plugins silently fall back to the publishable key when
 * no valid session is found (rather than throwing), which turns into a
 * confusing "row-level security policy" error instead of a clear one. Call
 * this before any RLS-protected storage/functions request so a stale or
 * not-yet-refreshed session surfaces as an obvious auth error instead.
 */
suspend fun SupabaseClient.ensureValidSession() {
    if (auth.currentAccessTokenOrNull() != null) return
    runCatching { auth.refreshCurrentSession() }
    check(auth.currentAccessTokenOrNull() != null) { "Your session has expired. Please sign in again." }
}
