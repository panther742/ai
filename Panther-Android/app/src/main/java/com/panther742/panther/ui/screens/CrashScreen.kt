package com.panther742.panther.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.panther742.panther.ui.theme.Cyan
import com.panther742.panther.ui.theme.Ink
import com.panther742.panther.ui.theme.InkSoft
import com.panther742.panther.ui.theme.TextHi
import com.panther742.panther.ui.theme.TextMid
import com.panther742.panther.ui.theme.Violet

/**
 * Shown on the NEXT launch if the previous run crashed.
 * Displays the recorded error so the user can screenshot it for support,
 * then lets them continue into the app anyway.
 */
@Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("🐾", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Panther pichli baar crash hua tha",
                style = MaterialTheme.typography.titleLarge,
                color = TextHi,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Error neeche likha hai. Screenshot le kar bhejo — turant fix ho jayega. Phir Continue dabao.",
                color = TextMid,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(InkSoft, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text(
                    report,
                    color = Color(0xFFF2B8B5),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink),
            ) {
                Text("Continue →", style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onContinue) {
                Text("App waise bhi chalao", color = TextMid)
            }
        }
    }
}
