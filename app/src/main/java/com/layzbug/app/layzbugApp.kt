package com.layzbug.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LayzbugApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase removed - using Supabase for backend
    }
}