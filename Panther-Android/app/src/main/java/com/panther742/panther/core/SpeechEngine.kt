package com.panther742.panther.core

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around Android's on-device speech recognition.
 * Supports Hinglish via hi-IN with automatic fallback to en-IN.
 */
class SpeechEngine(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningEnd: () -> Unit,
) : RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var started = false
    private var currentLang = LANG_HI
    private var missCount = 0

    fun isSupported(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (started) return
        if (!isSupported()) {
            onStatus("Speech recognition available nahi hai is device pe.")
            return
        }
        val rec = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = rec
        rec.setRecognitionListener(this)
        started = true
        rec.startListening(buildIntent(currentLang))
    }

    fun stop() {
        started = false
        missCount = 0
        recognizer?.let {
            runCatching { it.stopListening() }
            runCatching { it.cancel() }
            it.destroy()
        }
        recognizer = null
    }

    private fun buildIntent(lang: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

    override fun onReadyForSpeech(params: Bundle?) {
        onStatus("Sun raha hoon... 🎙️")
    }

    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onPartialResults(partialResults: Bundle?) {
        val text = bestResult(partialResults)
        if (!text.isNullOrBlank()) {
            missCount = 0
            onPartial(text)
        }
    }

    override fun onResults(results: Bundle?) {
        val text = bestResult(results)
        started = false
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null
        if (!text.isNullOrBlank()) {
            missCount = 0
            onFinal(text)
        } else {
            onError("Nahi suna — thoda aur clear bolo Boss! 🔊")
        }
        onListeningEnd()
    }

    override fun onError(error: Int) {
        started = false
        recognizer?.let { runCatching { it.cancel() }; runCatching { it.destroy() } }
        recognizer = null

        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> {
                missCount++
                if (missCount >= 3) {
                    onError("Teen baar try kiya, kuch samajh nahi aaya. Main chup ho jata hoon. 😅")
                    onListeningEnd()
                } else {
                    onStatus("Dobara sun raha hoon... kuch bolo Boss?")
                    restart()
                }
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (missCount >= 3) {
                    onError("Koi awaaz nahi mili. Chup ho gaya. 😴")
                    onListeningEnd()
                } else {
                    missCount++
                    restart()
                }
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                onError("Microphone permission chahiye — Settings me jaake de do. 🎤")
                onListeningEnd()
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                onError("Recognizer busy hai, ek second...")
                restartAfterDelay(800)
            }
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                // On-device or Google-backed recognizer briefly offline → one retry.
                if (missCount >= 2) {
                    onError("Speech recognition ka network nahi chal raha. Baad me try karo.")
                    onListeningEnd()
                } else {
                    missCount++
                    restart()
                }
            }
            else -> {
                onError("Kuch gadbad hui ($error). Dobara try karo?")
                onListeningEnd()
            }
        }
    }

    fun requestRestart() {
        if (!started) restart()
    }

    private fun restart() {
        if (started) return
        started = true
        runCatching {
            val rec = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = rec
            rec.setRecognitionListener(this)
            rec.startListening(buildIntent(currentLang))
        }
    }

    private fun restartAfterDelay(ms: Long) {
        if (!started) {
            started = true
            mainHandler.postDelayed({
                if (started) {
                    started = false
                    restart()
                }
            }, ms)
        }
    }

    private fun bestResult(bundle: Bundle?): String? {
        val list = bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?: return null
        return list.firstOrNull { it.isNotBlank() }?.trim()
    }

    private companion object {
        const val LANG_HI = "hi-IN"
        const val LANG_EN = "en-IN"
    }
}
