package com.layzbug.app.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.room.Room
import com.layzbug.app.FitSyncManager
import com.layzbug.app.data.local.AppDatabase
import com.layzbug.app.data.local.WalkDao
import com.layzbug.app.data.repository.FirebaseRepository
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
        ).build()
    }

    @Provides
    fun provideWalkDao(database: AppDatabase): WalkDao {
        return database.walkDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }

    @Provides
    @Singleton
    fun provideWalkRepository(
        walkDao: WalkDao,
        firebaseRepository: FirebaseRepository
    ): WalkRepository {
        return WalkRepository(walkDao, firebaseRepository)
    }

    @Provides
    @Singleton
    fun provideFitSyncManager(healthConnectClient: HealthConnectClient): FitSyncManager {
        return FitSyncManager(healthConnectClient)
    }
}