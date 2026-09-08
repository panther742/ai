package com.panther742.panther.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Panther's voice output. Tries an Indian Hindi male voice first,
 * then any hi-IN / en-IN voice, and finally the default engine voice.
 *
 * Fully crash-safe: if the device has no TTS engine, it silently disables
 * itself instead of taking the app down. All callbacks are posted to the
 * main thread.
 */
class VoiceBox(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayDeque<String>()
    private var onQueueEmpty: (() -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                mainHandler.post {
                    if (status == TextToSpeech.SUCCESS) {
                        ready = true
                        configureVoice()
                        drain()
                    } else {
                        // Engine failed to init — Panther just stays silent.
                        ready = false
                    }
                }
            }.also { engine ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            if (pending.isNotEmpty()) {
                                val next = pending.removeFirst()
                                speakNow(next)
                            } else {
                                onQueueEmpty?.invoke()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onDone(utteranceId)
                    }
                })
            }
        }
    }

    private fun configureVoice() {
        val engine = tts ?: return
        runCatching {
            val voices = engine.voices.orEmpty()

            fun pick(preferred: List<Locale>): android.speech.tts.Voice? {
                for (loc in preferred) {
                    // prefer male, then any
                    val male = voices.firstOrNull { it.locale == loc && it.name.contains("male", true) }
                    if (male != null) return male
                    val any = voices.firstOrNull { it.locale == loc }
                    if (any != null) return any
                }
                return null
            }

            val chosen = pick(
                listOf(
                    Locale("hi", "IN"),
                    Locale("en", "IN"),
                    Locale("en", "US"),
                ),
            )
            if (chosen != null) {
                runCatching { engine.voice = chosen }
            } else {
                val result = engine.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    runCatching { engine.setLanguage(Locale("en", "IN")) }
                }
            }
            engine.setPitch(0.8f)
            engine.setSpeechRate(0.95f)
        }
    }

    /** Speak immediately (queueing anything already speaking). */
    fun speak(text: String, onEmpty: (() -> Unit)? = null) {
        if (text.isBlank()) return
        onQueueEmpty = onEmpty
        runCatching {
            if (ready) {
                pending.addLast(text)
                if (pending.size == 1) speakNow(pending.removeFirst())
            } else {
                // Buffer a few lines while the TTS engine warms up.
                if (pending.size < 5) pending.addLast(text)
            }
        }
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return
        val utteranceId = UUID.randomUUID().toString()
        // QUEUE_FLUSH replaces any overlapping speech.
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        pending.clear()
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }

    private fun drain() {
        if (ready && pending.isNotEmpty()) {
            val next = pending.removeFirst()
            speakNow(next)
        }
    }
}
