package com.layzbug.app.di

import android.content.Context
import com.layzbug.app.BuildConfig
import com.layzbug.app.data.repository.SupabaseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideUserId(@ApplicationContext context: Context): String {
        // Get the signed-in Google account's ID
        return try {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            account?.id ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseRepository(
        supabase: SupabaseClient,
        userId: String
    ): SupabaseRepository {
        return SupabaseRepository(supabase, userId)
    }
}