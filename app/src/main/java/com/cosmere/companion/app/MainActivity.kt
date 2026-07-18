package com.cosmere.companion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cosmere.companion.app.ui.CompanionApp
import com.cosmere.companion.app.ui.theme.CosmereCompanionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CosmereCompanionTheme {
                CompanionApp()
            }
        }
    }
}
