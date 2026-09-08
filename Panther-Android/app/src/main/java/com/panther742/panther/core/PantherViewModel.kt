package com.panther742.panther.core

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panther742.panther.model.Bubble
import com.panther742.panther.model.ChatMessage
import com.panther742.panther.model.PantherUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PantherViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)
    private val clock = SimpleDateFormat("hh:mm a", Locale.ENGLISH)

    // IMPORTANT: settings must be initialised before greeting/_ui — greetingForHour()
    // reads _settings.value, and _ui reads greetingText during construction.
    private val _settings = MutableStateFlow(store.load())
    val settings: StateFlow<PantherSettings> = _settings.asStateFlow()

    private val greetingText: String by lazy {
        runCatching { greetingForHour() }.getOrDefault("Namaste, Boss!")
    }

    private val _ui = MutableStateFlow(PantherUiState(greeting = greetingText))
    val ui: StateFlow<PantherUiState> = _ui.asStateFlow()

    private var localBrain: LocalBrain? = null
    private lateinit var voice: VoiceBox
    private var speech: SpeechEngine? = null
    private var lastWasVoice = false
    private var bubbleId = 0L
    private var welcomeShown = false
    private var speechPermissionGranted = false

    override fun onCleared() {
        speech?.stop()
        if (::voice.isInitialized) voice.shutdown()
        super.onCleared()
    }

    fun attachSpeechPermission(granted: Boolean) {
        speechPermissionGranted = granted
        if (!granted) {
            _ui.update { it.copy(listening = false) }
        }
    }

    // ------------------------------------------------------------------
    // Setup helpers
    // ------------------------------------------------------------------
    private fun ensureVoice() {
        if (!::voice.isInitialized) {
            voice = VoiceBox(getApplication())
        }
    }

    private fun handleSpeechIdle() {
        _ui.update { it.copy(speaking = false) }
        // If always-listening is on, jump back to hearing the Boss.
        val s = _settings.value
        if (s.alwaysListen && !_ui.value.listening && lastWasVoice && !_ui.value.thinking) {
            startListeningInternal()
        }
    }

    private fun ensureBrain(): LocalBrain {
        val b = localBrain
        if (b != null) return b
        val nb = LocalBrain(getApplication<Application>().applicationContext, _settings.value.userName)
        localBrain = nb
        return nb
    }

    /** If the previous run crashed, surface the recorded error to the user. */
    private fun showPreviousCrash() {
        val app = getApplication<Application>()
        val report = (app as? com.panther742.panther.PantherApplication)?.readCrashReport() ?: return
        val firstLines = report.lineSequence().take(6).joinToString("\n")
        val msg = "⚠️ Pichli baar app crash hua tha. Error: $firstLines"
        addPantherBubble(msg)
    }

    private fun markBackendState() {
        val s = _settings.value
        val usable = s.apiKey.isNotBlank() || s.provider == PantherSettings.PROVIDER_CUSTOM
        _ui.update { it.copy(backendReady = usable) }
    }

    fun start() {
        ensureVoice()
        markBackendState()
        showPreviousCrash()
        if (!welcomeShown) {
            welcomeShown = true
            viewModelScope.launch {
                delay(650)
                val line = listOf(
                    "Namaste ${_settings.value.userName}! Main hoon Panther, aapka personal AI assistant. Mic dabao aur bolo, ya neeche likho — main sun raha hoon.",
                    "Hello Boss! Panther ready hai. Bolo kya karein aaj?",
                    "Namaste Boss! Subah ka time hai — chai peelo aur mujhse baat karo. ☕ Main yahan hoon!",
                ).random()
                addPantherBubble(line)
                voice.speak(line)
            }
        }
    }

    // ------------------------------------------------------------------
    // Public actions from the UI
    // ------------------------------------------------------------------
    fun toggleListening() {
        if (_ui.value.listening) {
            stopListening()
        } else {
            startListeningInternal()
        }
    }

    fun stopListening() {
        speech?.stop()
        _ui.update { it.copy(listening = false, status = "Ready") }
    }

    private fun startListeningInternal() {
        if (!speechPermissionGranted) {
            _ui.update {
                it.copy(status = "Microphone permission chahiye Boss — allow dabao. 🎤")
            }
            return
        }
        ensureVoice()
        speech?.stop()
        val engine = SpeechEngine(
            context = getApplication(),
            onPartial = { partial ->
                _ui.update { it.copy(transcript = partial, listening = true) }
            },
            onFinal = { text ->
                _ui.update { it.copy(transcript = "", listening = false) }
                sendText(text, fromVoice = true)
            },
            onStatus = { msg -> _ui.update { it.copy(status = msg) } },
            onError = { msg ->
                _ui.update {
                    it.copy(status = msg, transcript = "", listening = false)
                }
            },
            onListeningEnd = {
                _ui.update { it.copy(listening = false) }
            },
        )
        speech = engine
        _ui.update { it.copy(listening = true, transcript = "", status = "Sun raha hoon... 🎙️") }
        engine.start()
    }

    /** Send a command / question typed or spoken. */
    fun sendText(raw: String, fromVoice: Boolean = false) {
        val text = raw.trim()
        if (text.isEmpty()) return
        if (_ui.value.listening) stopListening()
        lastWasVoice = fromVoice

        addUserBubble(text)
        _ui.update { it.copy(status = "Soch raha hoon...", transcript = "") }

        val local = ensureBrain().handle(text)
        if (local != null) {
            when (local) {
                is SkillResult.Speak -> {
                    _ui.update { it.copy(status = "Ready", thinking = false) }
                    addPantherBubble(local.text)
                    speakLine(local.text)
                }
                is SkillResult.Launch -> {
                    _ui.update { it.copy(status = "Ready", thinking = false) }
                    addPantherBubble(local.spoken)
                    speakLine(local.spoken)
                    runCatching {
                        val intent = local.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        getApplication<Application>().startActivity(intent)
                    }.onFailure {
                        addPantherBubble("App kholne me dikkat aayi, par maine koshish ki. 🛠️")
                    }
                }
            }
            return
        }

        // Not a local skill → cloud AI brain
        val s = _settings.value
        if (s.apiKey.isBlank()) {
            val msg = "Boss, real AI baat karne ke liye pehle Settings ⚙️ me API key daalo — ya phir upar wale commands try karo, wo bina internet ke chalti hain. 🐾"
            _ui.update { it.copy(status = "API key missing", thinking = false) }
            addPantherBubble(msg)
            speakLine(msg)
            return
        }

        _ui.update { it.copy(thinking = true) }
        viewModelScope.launch {
            try {
                val backend = runCatching { buildBackend(s) }.getOrElse { e ->
                    val msg = "Settings me gadbad hai: ${e.message ?: "unknown"}"
                    _ui.update { it.copy(thinking = false, status = "Config error") }
                    addPantherBubble(msg)
                    return@launch
                }
                // Build conversation context from the visible chat bubbles.
                val convo = _ui.value.bubbles
                    .filterNot { it.text.startsWith("Server") }
                    .map { ChatMessage(if (it.fromUser) "user" else "assistant", it.text) }
                    .takeLast(16)

                val system = buildSystemPrompt(s.userName)
                var collected = StringBuilder()

                backend.streamReply(
                    system = system,
                    history = if (convo.isEmpty()) listOf(ChatMessage("user", text)) else convo,
                ) { delta ->
                    collected.append(delta)
                    _ui.update {
                        it.copy(
                            status = "Soch raha hoon...",
                            draftReply = collected.toString(),
                        )
                    }
                }

                val final = collected.toString().trim()
                _ui.update { it.copy(thinking = false, status = "Ready", draftReply = null) }

                if (final.isEmpty()) {
                    val msg = "Kuch reply nahi aaya Boss — API key/model check karo Settings me. 🤔"
                    addPantherBubble(msg)
                    speakLine(msg)
                } else {
                    addPantherBubble(final)
                    speakLine(final)
                }
            } catch (e: Exception) {
                Log.e("Panther", "AI error", e)
                _ui.update { it.copy(thinking = false, status = "Error", draftReply = null) }
                val msg = "Arre Boss, AI se connect nahi ho paya: ${friendly(e.message)} Dobara try karo ya API key check karo."
                addPantherBubble(msg)
            }
        }
    }

    // ------------------------------------------------------------------
    // Settings
    // ------------------------------------------------------------------
    fun updateSettings(s: PantherSettings) {
        _settings.value = s
        store.save(s)
        localBrain = LocalBrain(getApplication<Application>().applicationContext, s.userName)
        markBackendState()
    }

    /** Quick connectivity test for the settings panel. */
    suspend fun testConnection(apiKey: String, provider: String, model: String, baseUrl: String): String {
        return try {
            val s = PantherSettings(
                provider = provider,
                apiKey = apiKey.trim(),
                model = model.trim(),
                baseUrl = baseUrl.trim(),
            )
            val backend = buildBackend(s)
            withTimeout(20_000) {
                val reply = backend.streamReply(
                    system = buildSystemPrompt("Boss"),
                    history = listOf(ChatMessage("user", "Sirf 'OK' bolo — ye ek connection test hai.")),
                ) {}
                "✓ Connected! Panther ne kaha: ${reply.trim().take(90)}"
            }
        } catch (e: Exception) {
            Log.e("Panther", "test failed", e)
            "✗ Fail: ${e.message?.take(140) ?: "unknown error"}"
        }
    }

    fun openSettings() = _ui.update { it.copy(showSettings = true) }
    fun closeSettings() = _ui.update { it.copy(showSettings = false) }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------
    private fun speakLine(text: String) {
        ensureVoice()
        _ui.update { it.copy(speaking = true) }
        voice.speak(text) { handleSpeechIdle() }
    }

    private fun addUserBubble(text: String) {
        appendBubble(Bubble(id = ++bubbleId, text = text, fromUser = true, timeLabel = clock.format(Date())))
    }

    private fun addPantherBubble(text: String) {
        appendBubble(Bubble(id = ++bubbleId, text = text, fromUser = false, timeLabel = clock.format(Date())))
    }

    private fun appendBubble(b: Bubble) {
        _ui.update { it.copy(bubbles = (it.bubbles + b).takeLast(120)) }
    }

    private fun greetingForHour(): String {
        val h = Date().hoursCompat()
        return when (h) {
            in 5..11 -> "Good Morning, ${_settings.value.userName}!"
            in 12..16 -> "Good Afternoon, ${_settings.value.userName}!"
            in 17..21 -> "Good Evening, ${_settings.value.userName}!"
            else -> "Late night, ${_settings.value.userName}!"
        }
    }

    private fun friendly(msg: String?): String {
        val m = msg ?: return "network issue"
        return when {
            m.contains("401") || m.contains("api key") || m.contains("API key") -> "API key galat lag rahi hai (401)"
            m.contains("404") -> "Model nahi mila — model name check karo (404)"
            m.contains("429") -> "Rate limit hit — thodi der baad try karo (429)"
            m.contains("timeout") || m.contains("timed out") || m.contains("Socket") -> "network slow/timeout"
            else -> m.take(160)
        }
    }
}

private fun Date.hoursCompat(): Int {
    @Suppress("DEPRECATION")
    return hours
}
