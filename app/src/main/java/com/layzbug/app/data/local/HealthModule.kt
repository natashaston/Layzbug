package com.layzbug.app.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HealthModule {

    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient {
        return HealthConnectClient.getOrCreate(context)
    }
}