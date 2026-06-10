package com.example.whatsappstatussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.whatsappstatussaver.theme.WhatsAppStatusSaverTheme
import com.example.whatsappstatussaver.ui.navigation.WhatsAppStatusSaverNavHost

import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.example.whatsappstatussaver.ui.settings.SettingsViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            WhatsAppStatusSaverTheme(darkTheme = isDarkMode) { 
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
                    val navController = rememberNavController()
                    WhatsAppStatusSaverNavHost(navController = navController)
                } 
            }
        }
    }
}
