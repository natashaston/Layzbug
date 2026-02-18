package com.layzbug.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.lifecycleScope
import com.layzbug.app.ui.navigation.LayzbugNavHost
import com.layzbug.app.ui.theme.LayzbugTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Health Connect availability check
        lifecycleScope.launch {
            val status = HealthConnectClient.getSdkStatus(this@MainActivity)
            when (status) {
                HealthConnectClient.SDK_UNAVAILABLE ->
                    Log.e("LAYZBUG", "Health Connect is not available on this device")
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                    Log.e("LAYZBUG", "Health Connect needs an update")
                else ->
                    Log.d("LAYZBUG", "Health Connect is READY")
            }
        }

        enableEdgeToEdge()

        setContent {
            LayzbugTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LayzbugNavHost()

                }
            }
        }
    }
}