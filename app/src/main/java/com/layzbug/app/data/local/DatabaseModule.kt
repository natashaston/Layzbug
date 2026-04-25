package com.layzbug.app.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.room.Room
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.InstallationTracker
import com.layzbug.app.data.auth.AuthManager
import com.layzbug.app.data.local.AppDatabase
import com.layzbug.app.data.local.MIGRATION_1_2
import com.layzbug.app.data.local.MIGRATION_2_3
import com.layzbug.app.data.local.MIGRATION_3_4
import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.repository.SupabaseRepository
import com.layzbug.app.data.repository.WalkRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "layzbug_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }

    @Provides
    fun provideWalkDao(database: AppDatabase): WalkDao {
        return database.walkDao()
    }

    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context
    ): AuthManager {
        return AuthManager(context)
    }

    @Provides
    @Singleton
    fun provideInstallationTracker(@ApplicationContext context: Context): InstallationTracker {
        return InstallationTracker(context)
    }

    @Provides
    @Singleton
    fun provideWalkRepository(
        walkDao: WalkDao,
        supabaseRepository: SupabaseRepository
    ): WalkRepository {
        return WalkRepository(walkDao, supabaseRepository)
    }

    @Provides
    @Singleton
    fun provideFitSyncManager(healthConnectClient: HealthConnectClient): FitSyncManager {
        return FitSyncManager(healthConnectClient)
    }
}