package com.panther742.panther

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.panther742.panther.core.PantherViewModel
import com.panther742.panther.ui.screens.PantherApp
import com.panther742.panther.ui.theme.PantherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PantherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PantherTheme {
                PantherApp(vm = viewModel)
            }
        }
    }
}
