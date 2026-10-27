package com.example.core.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

private const val TAG = "AiCallEngine"

class AiCallEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var waveformJob: Job? = null
    private var aiJob: Job? = null

    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _callSession = MutableStateFlow<ActiveCallSession?>(null)
    val callSession: StateFlow<ActiveCallSession?> = _callSession.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallHistoryItem>>(emptyList())
    val callHistory: StateFlow<List<CallHistoryItem>> = _callHistory.asStateFlow()

    // Gemini Model lazy initialization via Firebase AI Logic
    private val geminiModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase AI not yet provisioned, fallback to character dialogue", e)
            null
        }
    }

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    updateStatus(CallStatus.AI_SPEAKING, "Speaking...")
                    startWaveformSimulation(intensity = 0.8f)
                }

                override fun onDone(utteranceId: String?) {
                    stopWaveformSimulation()
                    updateStatus(CallStatus.CONNECTED_IDLE, "Listening for your voice...")
                    // Automatically prompt listening if mic is unmuted
                    scope.launch {
                        delay(200)
                        startListening()
                    }
                }

                override fun onError(utteranceId: String?) {
                    stopWaveformSimulation()
                    updateStatus(CallStatus.CONNECTED_IDLE, "Audio playback ready.")
                }
            })
        } else {
            Log.e(TAG, "TTS Initialization failed: status=$status")
        }
    }

    fun startOutgoingCall(character: AiCharacter, language: SupportedLanguage, isDemoMode: Boolean) {
        endCall(saveHistory = false)

        val session = ActiveCallSession(
            character = character,
            direction = CallDirection.OUTGOING,
            status = CallStatus.OUTGOING_DIALING,
            language = language,
            isDemoMode = isDemoMode,
            statusNote = "Calling ${character.name}... (Ringing)"
        )
        _callSession.value = session

        scope.launch {
            startWaveformSimulation(intensity = 0.3f)
            delay(2200) // Simulate dialing
            if (_callSession.value?.status == CallStatus.OUTGOING_DIALING) {
                updateStatus(CallStatus.CONNECTING, "Securing AI voice channel...")
                delay(1200)
                connectCall()
            }
        }
    }

    fun prepareIncomingCall(character: AiCharacter, language: SupportedLanguage, isDemoMode: Boolean) {
        endCall(saveHistory = false)

        val session = ActiveCallSession(
            character = character,
            direction = CallDirection.INCOMING_SIMULATED,
            status = CallStatus.INCOMING_RINGING,
            language = language,
            isDemoMode = isDemoMode,
            statusNote = "Incoming call from ${character.name}"
        )
        _callSession.value = session
        startWaveformSimulation(intensity = 0.5f)
    }

    fun acceptIncomingCall() {
        val current = _callSession.value ?: return
        if (current.status == CallStatus.INCOMING_RINGING) {
            updateStatus(CallStatus.CONNECTING, "Answering call...")
            scope.launch {
                delay(800)
                connectCall()
            }
        }
    }

    fun declineIncomingCall() {
        endCall(saveHistory = false)
    }

    private fun connectCall() {
        val session = _callSession.value ?: return
        _callSession.value = session.copy(
            status = CallStatus.CONNECTED_IDLE,
            statusNote = "Connected • AI Character Call"
        )
        startCallTimer()

        // Character greets user
        val greeting = when (session.language) {
            SupportedLanguage.HINDI -> session.character.greetingHindi
            SupportedLanguage.HINGLISH -> session.character.greetingHinglish
            SupportedLanguage.ENGLISH -> session.character.greetingEnglish
        }

        speakAiMessage(greeting)
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                _callSession.value?.let { current ->
                    if (current.status != CallStatus.CALL_ENDED && current.status != CallStatus.INCOMING_RINGING && current.status != CallStatus.OUTGOING_DIALING) {
                        _callSession.value = current.copy(durationSeconds = current.durationSeconds + 1)
                    }
                }
            }
        }
    }

    fun speakAiMessage(message: String) {
        val session = _callSession.value ?: return

        // Add to transcripts
        val transcript = TranscriptMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            senderName = session.character.name,
            text = message
        )
        _callSession.value = session.copy(
            transcripts = session.transcripts + transcript,
            currentSpeechText = message
        )

        if (!session.isSpeakerOn) {
            // Speaker muted
            updateStatus(CallStatus.CONNECTED_IDLE, "Speaker is muted")
            return
        }

        if (isTtsReady && textToSpeech != null) {
            try {
                val locale = when (session.language) {
                    SupportedLanguage.HINDI -> Locale("hi", "IN")
                    SupportedLanguage.HINGLISH -> Locale("en", "IN")
                    SupportedLanguage.ENGLISH -> Locale.US
                }
                textToSpeech?.language = locale
                textToSpeech?.setPitch(session.character.ttsPitch)
                textToSpeech?.setSpeechRate(session.character.ttsSpeechRate)

                textToSpeech?.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "utterance_${System.currentTimeMillis()}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak error", e)
                updateStatus(CallStatus.CONNECTED_IDLE, "Listening...")
            }
        } else {
            // Simulated speech duration
            scope.launch {
                updateStatus(CallStatus.AI_SPEAKING, "Speaking...")
                startWaveformSimulation(intensity = 0.7f)
                val readTime = (message.length * 55L).coerceIn(1500L, 5000L)
                delay(readTime)
                stopWaveformSimulation()
                updateStatus(CallStatus.CONNECTED_IDLE, "Listening for your voice...")
            }
        }
    }

    fun startListening() {
        val session = _callSession.value ?: return
        if (session.isMuted) return
        if (session.status == CallStatus.AI_SPEAKING || session.status == CallStatus.AI_THINKING) return

        updateStatus(CallStatus.LISTENING_USER, "Listening... speak now")
        startWaveformSimulation(intensity = 0.6f)

        try {
            if (speechRecognizer == null) {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createSpeechListener())
                    }
                }
            }

            speechRecognizer?.let { recognizer ->
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    val langTag = when (session.language) {
                        SupportedLanguage.HINDI -> "hi-IN"
                        SupportedLanguage.HINGLISH -> "en-IN"
                        SupportedLanguage.ENGLISH -> "en-US"
                    }
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(intent)
                isListening = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer start error", e)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer stop error", e)
        }
        stopWaveformSimulation()
    }

    private fun createSpeechListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                updateStatus(CallStatus.LISTENING_USER, "You are speaking...")
                startWaveformSimulation(intensity = 0.9f)
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 14f).coerceIn(0.1f, 1.0f)
                _callSession.value = _callSession.value?.copy(liveAmplitude = normalized)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                stopWaveformSimulation()
                updateStatus(CallStatus.AI_THINKING, "AI is thinking...")
            }

            override fun onError(error: Int) {
                isListening = false
                stopWaveformSimulation()
                updateStatus(CallStatus.CONNECTED_IDLE, "Tap mic or say something...")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim()
                if (!spokenText.isNullOrBlank()) {
                    onUserSpoke(spokenText)
                } else {
                    updateStatus(CallStatus.CONNECTED_IDLE, "Tap mic to speak...")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    _callSession.value = _callSession.value?.copy(currentSpeechText = text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun onUserSpoke(text: String) {
        val session = _callSession.value ?: return
        val userTranscript = TranscriptMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            senderName = "You",
            text = text
        )

        _callSession.value = session.copy(
            transcripts = session.transcripts + userTranscript,
            currentSpeechText = text,
            status = CallStatus.AI_THINKING,
            statusNote = "${session.character.name} is thinking..."
        )

        generateAiReply(text, session)
    }

    private fun generateAiReply(userPrompt: String, session: ActiveCallSession) {
        aiJob?.cancel()
        aiJob = scope.launch {
            try {
                if (!session.isDemoMode && geminiModel != null) {
                    val fullPrompt = buildString {
                        appendLine(session.character.systemPrompt)
                        appendLine("Language requested: ${session.language.displayName}")
                        appendLine("User says: $userPrompt")
                        appendLine("Respond directly in 1-2 spoken sentences:")
                    }

                    val response = withTimeout(20_000L) {
                        geminiModel?.generateContent(fullPrompt)
                    }

                    val responseText = response?.text?.trim()
                    if (!responseText.isNullOrBlank()) {
                        speakAiMessage(responseText)
                        return@launch
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini online call exception, using conversational character fallback: ${e.message}")
            }

            // Demo Mode / Smart Fallback Dialogue
            val fallbackReply = generateCharacterFallbackReply(
                character = session.character,
                userPrompt = userPrompt,
                language = session.language
            )
            delay(600) // Realistic conversational pause
            speakAiMessage(fallbackReply)
        }
    }

    private fun generateCharacterFallbackReply(
        character: AiCharacter,
        userPrompt: String,
        language: SupportedLanguage
    ): String {
        val lower = userPrompt.lowercase()
        return when (character.id) {
            "luna_cyber" -> when {
                language == SupportedLanguage.HINDI -> "यह एक शानदार सवाल है! भविष्य की तकनीक में हम हमेशा नए रास्ते खोजते हैं।"
                language == SupportedLanguage.HINGLISH -> "Yeh idea solid hai! Future mein hum ise quantum computing se solve karte hain. Aur batao!"
                lower.contains("hello") || lower.contains("hi") -> "Transmission loud and clear! I'm monitoring the timelines. How can I assist you right now?"
                lower.contains("who") || lower.contains("you") -> "I'm Luna, your artificial intelligence cyber navigator from 2088. Here to guide, explore, and chat!"
                lower.contains("how are") -> "My quantum neural nets are operating at 99.8% efficiency! Feeling energized and happy to talk with you."
                else -> "That's fascinating! The computational streams align with what you just shared. Tell me more about that."
            }
            "arya_bollywood" -> when {
                language == SupportedLanguage.HINDI -> "अरे वाह! यह तो एकदम फिल्मी बात हुई। जिंदगी में थोड़ा ड्रामा और रोमांस तो होना ही चाहिए!"
                language == SupportedLanguage.HINGLISH -> "Arre waah yaar! Ekdum blockbuster dialogue mara aapne! Life mein thoda drama aur entertainment hona zaruri hai na?"
                lower.contains("hello") || lower.contains("hi") -> "Namaste ji! Picture abhi baaki hai mere dost! Tell me what excitement is going on?"
                else -> "Arey superhit! Your voice has pure cinema energy. Is episode mein aage kya hone wala hai, mujhe jaldi batao!"
            }
            "prof_barnaby" -> when {
                language == SupportedLanguage.HINDI -> "अद्भुत विचार! विज्ञान की यही तो खूबसूरती है कि हर सवाल एक नई खोज को जन्म देता है।"
                lower.contains("invention") || lower.contains("steam") -> "Indeed! I recently calibrated a steam-driven pocket automaton. The cogwheels spin with utter perfection!"
                else -> "Fascinating deduction, my dear friend! My brass gauges are fluttering with intrigue. Do elaborate on that premise!"
            }
            "detective_sterling" -> when {
                language == SupportedLanguage.HINDI -> "बात में कुछ राज छुपा लगता है। मैं इस सुराग पर अपनी नज़र रखूँगा।"
                language == SupportedLanguage.HINGLISH -> "Case interesting lag raha hai. Clues ko dhyaan se jodna padega. Kucch aur yaad aaya?"
                lower.contains("who") || lower.contains("killer") || lower.contains("case") -> "In this city, everyone's hiding something under their trench coat. Keep your eyes sharp."
                else -> "Noted in my case log. The shadow in that story doesn't quite match the angle. Give me the rest of the details."
            }
            "zen_kai" -> when {
                language == SupportedLanguage.HINDI -> "शांत रहें। जैसे जल अपनी राह बना लेता है, वैसे ही हर समस्या का समाधान निकल आता है।"
                language == SupportedLanguage.HINGLISH -> "Saans andar lo, shanti mehsoos karo. Har problem ka solution tabhi milta hai jab mann shant ho."
                else -> "Listen to the quiet space between thoughts. The answer you seek is already blooming within your calm heart."
            }
            else -> "That is genuinely intriguing! I'm glad we're connected on this voice frequency."
        }
    }

    fun toggleMute() {
        val current = _callSession.value ?: return
        val newMute = !current.isMuted
        _callSession.value = current.copy(isMuted = newMute)
        if (newMute) {
            stopListening()
            updateStatus(CallStatus.CONNECTED_IDLE, "Microphone muted")
        } else {
            updateStatus(CallStatus.CONNECTED_IDLE, "Microphone active")
            startListening()
        }
    }

    fun toggleSpeaker() {
        val current = _callSession.value ?: return
        val newSpeaker = !current.isSpeakerOn
        _callSession.value = current.copy(isSpeakerOn = newSpeaker)
        if (!newSpeaker) {
            textToSpeech?.stop()
        }
    }

    fun toggleTranscripts() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(showTranscripts = !current.showTranscripts)
    }

    fun setLanguage(language: SupportedLanguage) {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(language = language)
    }

    fun endCall(saveHistory: Boolean = true) {
        timerJob?.cancel()
        timerJob = null
        stopWaveformSimulation()
        stopListening()
        try {
            textToSpeech?.stop()
        } catch (ignored: Exception) {}

        val session = _callSession.value
        if (session != null && saveHistory && session.durationSeconds > 1) {
            val historyItem = CallHistoryItem(
                id = UUID.randomUUID().toString(),
                characterId = session.character.id,
                characterName = session.character.name,
                direction = session.direction,
                durationSeconds = session.durationSeconds,
                timestamp = System.currentTimeMillis(),
                language = session.language,
                transcriptSnippet = session.transcripts.lastOrNull { !it.isUser }?.text
                    ?: session.transcripts.lastOrNull()?.text
                    ?: "Call completed",
                transcripts = session.transcripts
            )
            _callHistory.value = listOf(historyItem) + _callHistory.value
        }

        _callSession.value = session?.copy(
            status = CallStatus.CALL_ENDED,
            statusNote = "Call ended"
        )
    }

    fun resetCallSession() {
        _callSession.value = null
    }

    private fun updateStatus(status: CallStatus, note: String) {
        _callSession.value = _callSession.value?.copy(
            status = status,
            statusNote = note
        )
    }

    private fun startWaveformSimulation(intensity: Float) {
        waveformJob?.cancel()
        waveformJob = scope.launch {
            while (isActive) {
                // Organic sinusoidal oscillation simulating speech cadence
                val base = 0.2f * intensity
                val peak = 0.8f * intensity
                val simAmp = (base + (Math.sin(System.currentTimeMillis() / 150.0).toFloat() * 0.5f + 0.5f) * (peak - base))
                    .coerceIn(0.05f, 1.0f)
                _callSession.value = _callSession.value?.copy(liveAmplitude = simAmp)
                delay(60)
            }
        }
    }

    private fun stopWaveformSimulation() {
        waveformJob?.cancel()
        waveformJob = null
        _callSession.value = _callSession.value?.copy(liveAmplitude = 0.05f)
    }

    fun release() {
        endCall(saveHistory = false)
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
        try {
            textToSpeech?.shutdown()
        } catch (ignored: Exception) {}
        textToSpeech = null
    }
}
