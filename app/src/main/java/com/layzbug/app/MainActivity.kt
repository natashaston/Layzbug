package com.layzbug.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.layzbug.app.ui.navigation.LayzbugNavHost
import com.layzbug.app.ui.theme.LayzbugTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This enables the transparent status/nav bars you defined in Theme.kt
        enableEdgeToEdge()

        setContent {
            LayzbugTheme {
                // Surface ensures the background color matches your 'Background' color variable
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.layzbug.app.ui.theme.Surface
                ) {
                    LayzbugNavHost()
                }
            }
        }
    }
}