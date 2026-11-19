package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.ThemePreferences
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.AnvSchoolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themePreferences = remember { ThemePreferences(applicationContext) }
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            AnvSchoolTheme(themeMode = themeMode) {
                AppNavigation(
                    themePreferences = themePreferences,
                    activeThemeMode = themeMode
                )
            }
        }
    }
}
