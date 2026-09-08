package com.panther742.panther.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.panther742.panther.core.PantherViewModel
import com.panther742.panther.model.Bubble
import com.panther742.panther.model.PantherUiState
import com.panther742.panther.ui.theme.Cyan
import com.panther742.panther.ui.theme.Fuchsia
import com.panther742.panther.ui.theme.GoodGreen
import com.panther742.panther.ui.theme.Ink
import com.panther742.panther.ui.theme.PantherIcons
import com.panther742.panther.ui.theme.SlateGlass
import com.panther742.panther.ui.theme.SlateLine
import com.panther742.panther.ui.theme.TextHi
import com.panther742.panther.ui.theme.TextLow
import com.panther742.panther.ui.theme.TextMid
import com.panther742.panther.ui.theme.Violet
import com.panther742.panther.ui.theme.WarmAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

private val timeFmt = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
private val dateFmt = SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH)

private val SUGGESTIONS = listOf(
    "⏰ Time kya hua?",
    "🔋 Battery kitni hai?",
    "😂 Ek joke sunao",
    "128 × 45 = ?",
    "🎬 YouTube pe Chhava trailer",
    "🌤 Surat ka weather",
    "❓ AI kya hai?",
)

/** Root UI of the app. */
@Composable
fun PantherApp(vm: PantherViewModel) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    // Microphone permission
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var micWait by remember { mutableStateOf(false) }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        micGranted = granted
        vm.attachSpeechPermission(granted)
        if (granted && micWait) {
            micWait = false
            vm.toggleListening()
        }
    }
    val pressMic: () -> Unit = {
        if (micGranted) {
            vm.toggleListening()
        } else {
            micWait = true
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        vm.start()
        vm.attachSpeechPermission(micGranted)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        // Ambient glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.20f),
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * 0.5f, size.height * 0.20f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Violet.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.95f),
                ),
                radius = size.width * 0.95f,
                center = Offset(size.width * 0.9f, size.height * 0.95f),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            ChatColumn(
                ui = ui,
                providerLabel = providerLabel(ui),
                speechSupported = SpeechRecognizer.isRecognitionAvailable(context),
                onOpenSettings = { vm.openSettings() },
                onSuggestion = { vm.sendText(it) },
                onPressMic = pressMic,
                onSendText = { vm.sendText(it, fromVoice = false) },
            )
        }

        if (ui.showSettings) {
            SettingsPanel(vm = vm, onClose = { vm.closeSettings() })
        }
    }
}

private fun providerLabel(ui: PantherUiState): String {
    if (!ui.backendReady) return "Demo mode"
    return "AI ready"
}

// ================================================================
// Main column: header + chat + composer
// ================================================================
@Composable
private fun ChatColumn(
    ui: PantherUiState,
    providerLabel: String,
    speechSupported: Boolean,
    onOpenSettings: () -> Unit,
    onSuggestion: (String) -> Unit,
    onPressMic: () -> Unit,
    onSendText: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }

    // Auto-scroll when a new message arrives (only if already near the bottom).
    val newestCount = ui.bubbles.size + if (ui.draftReply != null) 1 else 0
    LaunchedEffect(newestCount) {
        if (newestCount > 0) {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = info.totalItemsCount
            if (total - lastVisible <= 3) {
                listState.animateScrollToItem(total - 1)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { Header(ui = ui, providerLabel = providerLabel, onOpenSettings = onOpenSettings) }

            if (ui.bubbles.isEmpty() && ui.draftReply == null && !ui.thinking) {
                item(key = "orb") {
                    HeroOrb(
                        listening = ui.listening,
                        speaking = ui.speaking,
                        onTap = onPressMic,
                    )
                }
                item(key = "suggest") { Suggestions(onPick = onSuggestion) }
            } else {
                items(ui.bubbles, key = { it.id }) { bubble -> BubbleRow(bubble) }
                if (ui.draftReply != null) {
                    item(key = "draft") { DraftRow(text = ui.draftReply ?: "") }
                } else if (ui.thinking) {
                    item(key = "thinking") { ThinkingRow() }
                }
            }
        }

        ComposerBar(
            ui = ui,
            speechSupported = speechSupported,
            input = input,
            onInput = { input = it },
            onSend = {
                val t = input.trim()
                if (t.isNotEmpty()) {
                    input = ""
                    onSendText(t)
                }
            },
            onPressMic = onPressMic,
        )
    }
}

@Composable
private fun Header(ui: PantherUiState, providerLabel: String, onOpenSettings: () -> Unit) {
    val now = rememberTime()
    val date = rememberDate()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Cyan, Violet))),
            contentAlignment = Alignment.Center,
        ) {
            Text("🐾", fontSize = 21.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "PANTHER",
                style = TextStyle(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 6.sp,
                ),
                color = TextHi,
            )
            Text(
                "Aapka Personal AI Assistant",
                color = TextMid,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val battery = rememberBattery()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    now,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextHi,
                    fontSize = 14.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        PantherIcons.Battery,
                        contentDescription = "Battery",
                        tint = if (battery <= 15) WarmAmber else GoodGreen,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("$battery%", color = TextMid, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextMid)
            }
        }
    }

    Spacer(Modifier.height(2.dp))
    Text(
        ui.greeting,
        style = MaterialTheme.typography.displaySmall,
        color = TextHi,
    )
    Text(
        "$date  •  Panther online • $providerLabel",
        color = TextMid,
        fontSize = 12.sp,
    )
}

// ================================================================
// Hero orb — tap to talk
// ================================================================
@Composable
private fun HeroOrb(listening: Boolean, speaking: Boolean, onTap: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(216.dp)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            OrbCanvas(active = listening || speaking)
        }
        Text(
            when {
                listening -> "🎙️ Sun raha hoon — bolo Boss..."
                else -> "Baithne ki jagah — Panther se baat karne ke liye orb dabao"
            },
            color = if (listening) GoodGreen else TextMid,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun OrbCanvas(active: Boolean) {
    val infinite = rememberInfiniteTransition()
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val ripple by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart,
        ),
    )

    Canvas(Modifier.size(200.dp)) {
        val c = center
        val base = size.minDimension * 0.5f
        val r = base * pulse

        // Halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Cyan.copy(alpha = 0.35f), Violet.copy(alpha = 0.12f), Color.Transparent),
                center = c,
            ),
            radius = base * 1.9f,
            center = c,
        )

        // Orb body — pale core → cyan → blue → violet
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFF1FBFF),
                    0.5f to Cyan,
                    0.8f to Color(0xFF3B82F6),
                    1.0f to Violet,
                ),
                center = Offset(c.x - r * 0.3f, c.y - r * 0.35f),
                radius = r * 1.8f,
            ),
            radius = r,
            center = c,
        )
        // Specular highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.5f),
            radius = r * 0.18f,
            center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
        )

        // Rotating orbit arcs
        rotate(degrees = spin) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, Fuchsia.copy(alpha = 0.9f), Color.Transparent)),
                startAngle = 0f,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.16f, c.y - r * 1.16f),
                size = Size(r * 2.32f, r * 2.32f),
                style = Stroke(width = 2.5f),
            )
        }
        rotate(degrees = -spin * 1.7f) {
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 90f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.34f, c.y - r * 1.34f),
                size = Size(r * 2.68f, r * 2.68f),
                style = Stroke(width = 1.4f),
            )
        }

        // Active ripple ring
        if (active) {
            val alpha = (0.55f - (ripple - 1f) * 0.9f).coerceAtLeast(0f)
            drawCircle(
                color = GoodGreen.copy(alpha = alpha),
                radius = r * ripple,
                center = c,
                style = Stroke(width = 2.5f),
            )
        }
    }
}

@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    val state = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(state),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SUGGESTIONS.forEach { s ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SlateGlass)
                    .border(1.dp, SlateLine, RoundedCornerShape(50))
                    .clickable { onPick(s) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                Text(s, color = TextHi, fontSize = 13.sp)
            }
        }
    }
}

// ================================================================
// Chat bubbles
// ================================================================
@Composable
private fun BubbleRow(bubble: Bubble) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (bubble.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!bubble.fromUser) {
            MiniOrb(Modifier.size(27.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(
            Modifier.widthIn(max = 302.dp),
            horizontalAlignment = if (bubble.fromUser) Alignment.End else Alignment.Start,
        ) {
            val shape = RoundedCornerShape(
                topStart = if (bubble.fromUser) 18.dp else 5.dp,
                topEnd = if (bubble.fromUser) 5.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp,
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(
                        if (bubble.fromUser) {
                            Brush.linearGradient(listOf(Color(0xFF16D3EA), Violet))
                        } else {
                            SlateGlass
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    bubble.text,
                    color = if (bubble.fromUser) Ink else TextHi,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                bubble.timeLabel,
                color = TextLow,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun DraftRow(text: String) {
    Row {
        MiniOrb(Modifier.size(27.dp))
        Spacer(Modifier.width(8.dp))
        val shape = RoundedCornerShape(topStart = 5.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
        Box(
            Modifier
                .clip(shape)
                .background(SlateGlass)
                .border(1.dp, Cyan.copy(alpha = 0.3f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text.ifBlank { "…" },
                color = TextHi,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniOrb(Modifier.size(27.dp))
        Spacer(Modifier.width(10.dp))
        EqualizerBars()
        Spacer(Modifier.width(8.dp))
        Text("Panther soch raha hai...", color = TextMid, fontSize = 13.sp)
    }
}

/** Small glowing orb avatar. */
@Composable
fun MiniOrb(modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF1FBFF), Cyan, Violet),
                center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
                radius = r * 2f,
            ),
            radius = r,
            center = center,
        )
    }
}

@Composable
private fun EqualizerBars() {
    val infinite = rememberInfiniteTransition()
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850, easing = LinearEasing), RepeatMode.Restart),
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 0..4) {
            val h = 6f + ((sin(t * 2 * PI + i * 0.9) + 1) / 2).toFloat() * 14f
            Box(
                Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Cyan, Violet))),
            )
        }
    }
}

// ================================================================
// Composer
// ================================================================
@Composable
private fun ComposerBar(
    ui: PantherUiState,
    speechSupported: Boolean,
    input: String,
    onInput: (String) -> Unit,
    onSend: () -> Unit,
    onPressMic: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Status / live transcript line
        val dotColor = when {
            ui.listening -> GoodGreen
            ui.thinking -> Violet
            ui.speaking -> Cyan
            ui.backendReady -> Cyan
            else -> WarmAmber
        }
        val statusLabel = when {
            ui.listening && ui.transcript.isNotBlank() -> "🎙️ ${ui.transcript}"
            ui.listening -> ui.status
            ui.thinking -> "Panther soch raha hai..."
            ui.speaking -> "Panther bol raha hai..."
            ui.backendReady -> "Ready • AI connected"
            else -> "Ready • Demo mode (local commands chalti hain, AI ke liye ⚙️ me key daalo)"
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                statusLabel,
                color = TextMid,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))

        // Input pill
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(SlateGlass)
                .border(
                    1.dp,
                    if (ui.listening) GoodGreen.copy(alpha = 0.55f) else SlateLine,
                    RoundedCornerShape(30.dp),
                )
                .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = input,
                onValueChange = onInput,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 9.dp),
                textStyle = TextStyle(color = TextHi, fontSize = 15.sp),
                cursorBrush = SolidColor(Cyan),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) {
                            Text(
                                when {
                                    ui.listening -> "Bolo Boss, main sun raha hoon..."
                                    else -> if (speechSupported) "Type karo ya mic dabao..." else "Speech support nahi — type karke baat karo"
                                },
                                color = TextMid,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    }
                },
            )

            when {
                input.isNotBlank() -> {
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Cyan, Violet)))
                            .clickable(onClick = onSend),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            PantherIcons.Send,
                            contentDescription = "Send",
                            tint = Ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                ui.listening -> {
                    MicStopButton(onStop = onPressMic)
                }
                else -> {
                    if (speechSupported) {
                        MicStartButton(onStart = onPressMic)
                    } else {
                        Spacer(Modifier.size(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MicStartButton(onStart: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 6.dp)
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Cyan, Violet)))
            .clickable(onClick = onStart),
    ) {
        Icon(
            PantherIcons.Mic,
            contentDescription = "Start listening",
            tint = Ink,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MicStopButton(onStop: () -> Unit) {
    val infinite = rememberInfiniteTransition()
    val ring by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
    )
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(58.dp)
            .clip(CircleShape)
            .clickable(onClick = onStop),
        contentAlignment = Alignment.Center,
    ) {
        // pulsing ring
        Canvas(
            Modifier
                .size(58.dp * ring)
                .clip(CircleShape)
                .background(GoodGreen.copy(alpha = 0.22f)),
        ) {}
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GoodGreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                PantherIcons.Mic,
                contentDescription = "Stop listening",
                tint = Ink,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

// ================================================================
// Time/battery helpers
// ================================================================
@Composable
private fun rememberTime(): String {
    var now by remember { mutableStateOf(timeFmt.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            now = timeFmt.format(Date())
            delay(30_000)
        }
    }
    return now
}

@Composable
private fun rememberDate(): String {
    var d by remember { mutableStateOf(dateFmt.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            d = dateFmt.format(Date())
            delay(60_000)
        }
    }
    return d
}

@Composable
private fun rememberBattery(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(readBattery(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            level = readBattery(context)
            delay(30_000)
        }
    }
    return level.coerceIn(0, 100)
}

private fun readBattery(context: Context): Int {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    return bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
}
