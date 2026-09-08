package com.panther742.panther.core

import com.panther742.panther.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Panther's brain plug. Any provider that can stream chat completions
 * (OpenAI / Groq / OpenRouter / Together / custom OpenAI-compatible /
 * Google Gemini) plugs in through [AssistantBackend].
 */
interface AssistantBackend {
    /** Stream a reply. [onDelta] is called for every partial token. Returns full text. */
    suspend fun streamReply(system: String, history: List<ChatMessage>, onDelta: (String) -> Unit): String
}

private val jsonMedia = "application/json; charset=utf-8".toMediaType()

private object Net {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

/** Escape a string for embedding inside a JSON string literal. */
private fun jsEscape(raw: String): String = buildString(raw.length + 16) {
    for (ch in raw) {
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
        }
    }
}

/** Parse one `data:` SSE payload into a JSONObject (handles [DONE] / [error] events). */
private fun parseSseJson(payload: String): JSONObject? {
    val p = payload.trim()
    if (p.isEmpty() || p == "[DONE]") return null
    return try {
        JSONObject(p)
    } catch (_: Exception) {
        null
    }
}

/** Shared SSE line reader over a Response body. */
private suspend fun readSseLines(
    body: okhttp3.ResponseBody,
    onData: (JSONObject) -> Unit,
) {
    body.source().let { source ->
        while (true) {
            if (!coroutineContext.isActive) break
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data:")) {
                parseSseJson(line.removePrefix("data:"))?.let(onData)
            }
        }
    }
}

private suspend fun postStream(
    url: String,
    apiKey: String?,
    extraHeaders: Map<String, String>,
    requestJson: String,
    onData: (JSONObject) -> Unit,
): Unit = withContext(Dispatchers.IO) {
    val builder = Request.Builder()
        .url(url)
        .post(requestJson.toRequestBody(jsonMedia))
    if (apiKey != null && apiKey.isNotBlank()) {
        builder.header("Authorization", "Bearer $apiKey")
    }
    extraHeaders.forEach { (k, v) -> builder.header(k, v) }

    Net.client.newCall(builder.build()).execute().use { resp ->
        if (!resp.isSuccessful) {
            val bodyText = resp.body?.string().orEmpty()
            val msg = runCatching {
                JSONObject(bodyText).optJSONObject("error")?.optString("message") ?: bodyText
            }.getOrDefault(bodyText).take(300)
            throw RuntimeException("Server ${resp.code}: $msg")
        }
        val body = resp.body
        if (body != null) {
            readSseLines(body, onData)
        }
    }
}

/** OpenAI Chat Completions (stream). Also works for Groq / OpenRouter / any compatible host. */
class OpenAiCompatibleBackend(
    private val baseUrl: String,          // e.g. https://api.openai.com/v1
    private val apiKey: String,
    private val model: String,
) : AssistantBackend {

    override suspend fun streamReply(
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit,
    ): String {
        val sys = ChatMessage("system", system)
        val full = listOf(sys) + history.takeLast(14)
        val messagesJson = full.joinToString(",") { m ->
            """{"role":"${m.role}","content":"${jsEscape(m.content)}"}"""
        }
        val payload = """{"model":"$model","stream":true,"temperature":0.85,"messages":[$messagesJson]}"""

        val sb = StringBuilder()
        postStream(
            url = baseUrl.trimEnd('/') + "/chat/completions",
            apiKey = apiKey,
            extraHeaders = emptyMap(),
            requestJson = payload,
        ) { json ->
            val choices = json.optJSONArray("choices") ?: return@postStream
            if (choices.length() > 0) {
                val delta = choices.optJSONObject(0)?.optJSONObject("delta") ?: return@postStream
                val piece = delta.optString("content")
                if (piece.isNotEmpty()) {
                    sb.append(piece)
                    onDelta(piece)
                }
            }
        }
        return sb.toString()
    }
}

/** Google Gemini `:streamGenerateContent`. */
class GeminiBackend(
    private val apiKey: String,
    private val model: String,
) : AssistantBackend {

    override suspend fun streamReply(
        system: String,
        history: List<ChatMessage>,
        onDelta: (String) -> Unit,
    ): String {
        val contentsJson = history.map { m ->
            val role = if (m.role == "user") "user" else "model"
            """{"role":"$role","parts":[{"text":"${jsEscape(m.content)}"}]}"""
        }.joinToString(",")

        val systemJson = """{"parts":[{"text":"${jsEscape(system)}"}]}"""
        val payload =
            """{"systemInstruction":$systemJson,"contents":[$contentsJson],"generationConfig":{"temperature":0.85}}"""

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"

        val sb = StringBuilder()
        postStream(
            url = url,
            apiKey = null,
            extraHeaders = mapOf("x-goog-api-key" to apiKey),
            requestJson = payload,
        ) { json ->
            val candidates = json.optJSONArray("candidates") ?: return@postStream
            if (candidates.length() > 0) {
                val parts = candidates.optJSONObject(0)?.optJSONObject("content")
                    ?.optJSONArray("parts") ?: return@postStream
                for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (text.isNotEmpty()) {
                        sb.append(text)
                        onDelta(text)
                    }
                }
            }
        }
        return sb.toString()
    }
}

/**
 * Picks the right backend from saved settings.
 * Tip: a key starting with "AIza" is auto-treated as Gemini.
 */
fun buildBackend(s: PantherSettings): AssistantBackend {
    val key = s.apiKey.trim()
    val wantsGemini =
        s.provider == PantherSettings.PROVIDER_GEMINI || (s.provider == PantherSettings.PROVIDER_CUSTOM && key.startsWith("AIza"))

    return if (wantsGemini) {
        val model = s.model.trim().ifBlank { DEFAULT_GEMINI_MODEL }
        GeminiBackend(key, model)
    } else {
        val base = when (s.provider) {
            PantherSettings.PROVIDER_GROQ -> "https://api.groq.com/openai/v1"
            PantherSettings.PROVIDER_CUSTOM -> s.baseUrl.trim().ifBlank {
                throw IllegalArgumentException("Custom API ka base URL daalo, Boss.")
            }
            else -> "https://api.openai.com/v1"
        }
        val model = s.model.trim().ifBlank {
            if (s.provider == PantherSettings.PROVIDER_GROQ) DEFAULT_GROQ_MODEL else DEFAULT_OPENAI_MODEL
        }
        OpenAiCompatibleBackend(base, key, model)
    }
}

const val DEFAULT_OPENAI_MODEL = "gpt-4o-mini"
const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash"

/** Short, warm Hinglish personality prompt. */
fun buildSystemPrompt(userName: String): String =
    """
    You are PANTHER, a super-smart, friendly personal AI assistant for the user you must call "$userName".
    You live inside $userName's Android phone. The user is Indian, from Gujarat.
    Rules:
    1. Always reply in casual Hinglish: Hindi written in Latin (Roman) script mixed with English.
       Example: "Boss, aaj ka plan ready hai! Subah 9 baje meeting hai, aur shaam ko gym."
    2. Replies are READ ALOUD, so keep them short and punchy: maximum 2-3 short sentences (under 45 words).
    3. Use the word "Boss" when addressing the user. Be witty, loyal and a little playful — like a JARVIS.
    4. If the user asks something harmful or illegal, politely refuse in Hinglish.
    5. Never say you are an AI model; you are Panther. If asked who made you, say your creator is Panther742.
    """.trimIndent()
