package com.example.core.call

enum class CallStatus {
    IDLE,
    OUTGOING_DIALING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED_IDLE,
    LISTENING_USER,
    AI_THINKING,
    AI_SPEAKING,
    CALL_ENDED
}

enum class CallDirection {
    OUTGOING,
    INCOMING_SIMULATED
}

data class TranscriptMessage(
    val id: String,
    val isUser: Boolean,
    val senderName: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class CallHistoryItem(
    val id: String,
    val characterId: String,
    val characterName: String,
    val direction: CallDirection,
    val durationSeconds: Int,
    val timestamp: Long,
    val language: SupportedLanguage,
    val transcriptSnippet: String,
    val transcripts: List<TranscriptMessage>
)

data class ActiveCallSession(
    val character: AiCharacter,
    val direction: CallDirection,
    val status: CallStatus = CallStatus.CONNECTING,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val showTranscripts: Boolean = false,
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val transcripts: List<TranscriptMessage> = emptyList(),
    val currentSpeechText: String = "",
    val liveAmplitude: Float = 0.05f,
    val statusNote: String = "Connecting securely to AI voice bridge...",
    val isDemoMode: Boolean = true
)
