package com.layzbug.app.di

import android.content.Context
import com.layzbug.app.notifications.NotificationPrefsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationPrefsRepository(
        @ApplicationContext context: Context
    ): NotificationPrefsRepository = NotificationPrefsRepository(context)
}
