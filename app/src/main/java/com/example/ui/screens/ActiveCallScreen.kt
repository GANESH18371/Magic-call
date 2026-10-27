package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.call.ActiveCallSession
import com.example.core.call.AiCallEngine
import com.example.core.call.CallStatus
import com.example.core.call.SupportedLanguage
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ActiveCallScreen(
    callEngine: AiCallEngine,
    session: ActiveCallSession,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customMessageInput by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            callEngine.startListening()
        }
    }

    // Quick conversation prompts based on character & language
    val quickPrompts = remember(session.character.id, session.language) {
        when {
            session.language == SupportedLanguage.HINDI -> listOf(
                "आप कैसे हैं?",
                "मुझे कोई मजेदार बात बताएं",
                "क्या आप इंसान हैं या एआई?",
                "आज का दिन कैसा रहेगा?"
            )
            session.language == SupportedLanguage.HINGLISH -> listOf(
                "Arre dost, kya chal raha hai?",
                "Kuch blockbuster dialogue bolo",
                "Are you a real person or AI?",
                "Ek achi advice do na!"
            )
            else -> listOf(
                "How are you today?",
                "Tell me an intriguing secret",
                "Are you human or AI?",
                "What's your best advice?"
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status & Call Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                // Fictional AI disclaimer badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1738))
                        .border(1.dp, NeonAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AI SIMULATED CALL • FICTIONAL CHARACTER",
                        color = NeonAmber,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = session.character.name,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = session.character.title,
                    color = session.character.primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Call timer or dialing status
                val timerDisplay = if (session.status == CallStatus.OUTGOING_DIALING) {
                    "Dialing..."
                } else if (session.status == CallStatus.CONNECTING) {
                    "Connecting..."
                } else {
                    formatCallDuration(session.durationSeconds)
                }

                Text(
                    text = timerDisplay,
                    color = if (session.status == CallStatus.OUTGOING_DIALING) NeonAmber else NeonGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Center: Pulsing Character Avatar & Waveform Visualizer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                CharacterAvatar(
                    character = session.character,
                    size = 110.dp,
                    isPulsing = session.status == CallStatus.AI_SPEAKING || session.status == CallStatus.LISTENING_USER || session.status == CallStatus.OUTGOING_DIALING,
                    pulseAmplitude = session.liveAmplitude,
                    showAiBadge = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Audio Waveform
                WaveformVisualizer(
                    amplitudes = List(32) { session.liveAmplitude },
                    isActive = session.status == CallStatus.AI_SPEAKING || session.status == CallStatus.LISTENING_USER,
                    height = 54.dp,
                    gradientColors = listOf(session.character.primaryColor, session.character.secondaryColor, NeonCyan)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status note caption
                Text(
                    text = session.statusNote,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current or latest spoken subtitle bubble
                val lastAiMessage = session.transcripts.lastOrNull { !it.isUser }?.text
                val lastUserMessage = session.transcripts.lastOrNull { it.isUser }?.text
                val activeSnippet = when {
                    session.status == CallStatus.LISTENING_USER && session.currentSpeechText.isNotBlank() ->
                        "You: \"${session.currentSpeechText}\""
                    session.status == CallStatus.AI_SPEAKING && !lastAiMessage.isNullOrBlank() ->
                        "${session.character.name}: \"$lastAiMessage\""
                    !lastAiMessage.isNullOrBlank() ->
                        "${session.character.name}: \"$lastAiMessage\""
                    else -> "Say hello or choose a prompt below to chat!"
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF16122C))
                        .border(1.dp, Color(0xFF2B2252), RoundedCornerShape(16.dp)),
                    color = Color.Transparent
                ) {
                    Text(
                        text = activeSnippet,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // Quick Prompts & Manual Speech Input (Ensures 100% testability on emulators without microphone!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick prompt suggestions
                Text(
                    text = "QUICK PROMPTS (TAP TO SPEAK)",
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPrompts) { prompt ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, session.character.primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    callEngine.onUserSpoke(prompt)
                                }
                                .testTag("quick_prompt_${prompt.take(6)}"),
                            color = SurfaceDark
                        ) {
                            Text(
                                text = prompt,
                                color = session.character.primaryColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom message input row for silent or emulator environments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMessageInput,
                        onValueChange = { customMessageInput = it },
                        placeholder = { Text("Type to speak to ${session.character.name}...", fontSize = 12.sp, color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("call_text_input"),
                        shape = RoundedCornerShape(20.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = session.character.primaryColor,
                            unfocusedIndicatorColor = Color(0xFF2D2556)
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (customMessageInput.isNotBlank()) {
                                callEngine.onUserSpoke(customMessageInput.trim())
                                customMessageInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(session.character.primaryColor)
                            .testTag("send_call_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // In-Call Controls Row: Mute, Speaker, Transcripts, End Call
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute Mic Button
                    CallControlButton(
                        icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (session.isMuted) "Unmute" else "Mute",
                        isActive = session.isMuted,
                        activeColor = CrimsonRed,
                        onClick = { callEngine.toggleMute() },
                        testTag = "call_mute_button"
                    )

                    // Push-to-Talk / Listen Button
                    CallControlButton(
                        icon = Icons.Default.Mic,
                        label = if (session.status == CallStatus.LISTENING_USER) "Listening..." else "Speak",
                        isActive = session.status == CallStatus.LISTENING_USER,
                        activeColor = NeonGreen,
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                callEngine.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        testTag = "call_listen_button"
                    )

                    // Speakerphone Toggle
                    CallControlButton(
                        icon = if (session.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (session.isSpeakerOn) "Speaker" else "Muted Audio",
                        isActive = session.isSpeakerOn,
                        activeColor = NeonCyan,
                        onClick = { callEngine.toggleSpeaker() },
                        testTag = "call_speaker_button"
                    )

                    // Transcripts Toggle
                    CallControlButton(
                        icon = Icons.Default.Description,
                        label = "Transcript",
                        isActive = session.showTranscripts,
                        activeColor = NeonPurple,
                        onClick = { callEngine.toggleTranscripts() },
                        testTag = "call_transcripts_button"
                    )

                    // End Call (Big Red Button)
                    IconButton(
                        onClick = {
                            callEngine.endCall(saveHistory = true)
                            onEndCall()
                        },
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Optional Transcripts Overlay Drawer
        AnimatedVisibility(
            visible = session.showTranscripts,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Call Transcript (${session.transcripts.size} turns)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Close",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { callEngine.toggleTranscripts() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(session.transcripts) { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (msg.isUser) Color(0xFF281E48) else Color(0xFF191433))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = msg.senderName,
                                        color = if (msg.isUser) NeonPink else session.character.primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = msg.text,
                                        color = TextPrimary,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor.copy(alpha = 0.25f) else Color(0xFF1F1B3B))
                .border(1.dp, if (isActive) activeColor else Color(0xFF2E2657), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else TextMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatCallDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}
