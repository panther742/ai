package com.panther742.panther

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.panther742.panther.core.PantherViewModel
import com.panther742.panther.ui.screens.CrashScreen
import com.panther742.panther.ui.screens.PantherApp
import com.panther742.panther.ui.theme.PantherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PantherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If the previous run crashed, show the recorded error first so the user
        // can screenshot it (and we can fix the exact bug).
        val app = application as PantherApplication
        val crashReport = app.readCrashReport()

        setContent {
            PantherTheme {
                var showCrash by remember { mutableStateOf(crashReport != null) }
                if (showCrash) {
                    CrashScreen(report = crashReport ?: "Unknown error", onContinue = { showCrash = false })
                } else {
                    PantherApp(vm = viewModel)
                }
            }
        }
    }
}
