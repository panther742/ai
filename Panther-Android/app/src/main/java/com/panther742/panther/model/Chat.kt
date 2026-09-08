package com.panther742.panther.model

/**
 * A single message as sent to / received from the LLM API.
 * role: "system" | "user" | "assistant"
 */
data class ChatMessage(val role: String, val content: String)

/** A chat bubble rendered in the UI. */
data class Bubble(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val timeLabel: String,
)

/** Immutable snapshot of everything the UI needs. */
data class PantherUiState(
    val bubbles: List<Bubble> = emptyList(),
    val transcript: String = "",
    val status: String = "Ready",
    val listening: Boolean = false,
    val thinking: Boolean = false,
    val speaking: Boolean = false,
    val backendReady: Boolean = false,
    val greeting: String = "",
    val showSettings: Boolean = false,
    /** Streaming partial reply while the AI thinks. */
    val draftReply: String? = null,
)
