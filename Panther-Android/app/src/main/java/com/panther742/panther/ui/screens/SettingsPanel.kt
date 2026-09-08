package com.panther742.panther.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.panther742.panther.core.PantherSettings
import com.panther742.panther.core.PantherViewModel
import com.panther742.panther.ui.theme.Cyan
import com.panther742.panther.ui.theme.GoodGreen
import com.panther742.panther.ui.theme.Ink
import com.panther742.panther.ui.theme.InkSoft
import com.panther742.panther.ui.theme.SlateGlass
import com.panther742.panther.ui.theme.TextHi
import com.panther742.panther.ui.theme.TextMid
import com.panther742.panther.ui.theme.Violet
import kotlinx.coroutines.launch

private data class ProviderOption(val id: String, val label: String, val hint: String)

private val PROVIDERS = listOf(
    ProviderOption(PantherSettings.PROVIDER_OPENAI, "OpenAI", "api.openai.com · gpt model"),
    ProviderOption(PantherSettings.PROVIDER_GROQ, "Groq", "api.groq.com · free & fast"),
    ProviderOption(PantherSettings.PROVIDER_GEMINI, "Google Gemini", "AI Studio se key lo"),
    ProviderOption(PantherSettings.PROVIDER_CUSTOM, "Custom (OpenAI-compatible)", "koi bhi base URL"),
)

/** Full-screen settings panel shown above the chat UI. */
@Composable
fun SettingsPanel(vm: PantherViewModel, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsState()
    val ui by vm.ui.collectAsState()

    var provider by rememberSaveable { mutableStateOf(settings.provider) }
    var apiKey by rememberSaveable { mutableStateOf(settings.apiKey) }
    var model by rememberSaveable { mutableStateOf(settings.model) }
    var baseUrl by rememberSaveable { mutableStateOf(settings.baseUrl) }
    var alwaysListen by rememberSaveable { mutableStateOf(settings.alwaysListen) }
    var userName by rememberSaveable { mutableStateOf(settings.userName) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(InkSoft, Ink)), RoundedCornerShape(28.dp))
                .imePadding(),
        ) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⚙️ Panther Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHi,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMid)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    "AI provider choose karo aur API key daalo. Key sirf aapke phone me save hoti hai.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMid,
                )

                Spacer(Modifier.height(18.dp))
                Text("Provider", style = MaterialTheme.typography.labelSmall, color = TextMid)
                Spacer(Modifier.height(8.dp))
                PROVIDERS.forEach { opt ->
                    val selected = provider == opt.id
                    val accent = if (selected) Cyan else TextMid
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { provider = opt.id }
                            .background(
                                if (selected) SlateGlass else Color.Transparent,
                                RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (selected) "●" else "○",
                            color = accent,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(opt.label, color = TextHi, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                opt.hint,
                                color = TextMid.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trim() },
                    label = { Text("API Key") },
                    placeholder = { Text(if (provider == PantherSettings.PROVIDER_GEMINI) "AIza... (Google AI Studio)" else "sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model (optional)") },
                    placeholder = {
                        Text(
                            when (provider) {
                                PantherSettings.PROVIDER_GROQ -> "llama-3.3-70b-versatile"
                                PantherSettings.PROVIDER_GEMINI -> "gemini-2.0-flash"
                                else -> "gpt-4o-mini"
                            },
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (provider == PantherSettings.PROVIDER_CUSTOM) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL (OpenAI-compatible)") },
                        placeholder = { Text("https://your-host.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Panther aapko kya bulaaye?") },
                    placeholder = { Text("Boss") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Always Listening", color = TextHi, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Jawab ke baad wapas sunna shuru karo (battery zyada lagti hai)",
                            color = TextMid.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(checked = alwaysListen, onCheckedChange = { alwaysListen = it })
                }

                Spacer(Modifier.height(16.dp))

                // Test connection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = apiKey.isNotBlank() && !testing, onClick = {
                        testing = true
                        testResult = null
                        scope.launch {
                            testResult = vm.testConnection(apiKey, provider, model, baseUrl)
                            testing = false
                        }
                    }) {
                        Text(if (testing) "Testing..." else "🔌 Test Connection")
                    }
                    if (testing) {
                        CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp, color = Cyan)
                    }
                }
                testResult?.let { res ->
                    val ok = res.startsWith("✓")
                    Text(
                        res,
                        color = if (ok) GoodGreen else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                Button(
                    onClick = {
                        vm.updateSettings(
                            PantherSettings(
                                provider = provider,
                                apiKey = apiKey.trim(),
                                model = model.trim(),
                                baseUrl = baseUrl.trim(),
                                alwaysListen = alwaysListen,
                                userName = userName.trim().ifBlank { "Boss" },
                            ),
                        )
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Violet,
                        contentColor = Ink,
                    ),
                ) {
                    Text("Save Settings", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
