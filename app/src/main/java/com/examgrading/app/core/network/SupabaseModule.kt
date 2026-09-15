package com.examgrading.app.core.network

import com.examgrading.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

/**
 * Client is configured with the publishable (anon) key only — safe to embed
 * in the app. All RLS-protected data (answer keys, grading internals) stays
 * inaccessible from here regardless of what the client code requests; that
 * boundary is enforced server-side by the Supabase RLS policies, not by
 * anything in this module.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(androidSessionManager: AndroidSessionManager): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth) {
            sessionManager = androidSessionManager
            // The default pauses auto-refresh when the app loses focus, which
            // happens constantly here (camera, file/document pickers) - that
            // was letting the access token actually expire before the app
            // came back, instead of getting silently refreshed in time.
            enableLifecycleCallbacks = false
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
        install(Functions)
    }
}
