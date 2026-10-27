package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.audio.AudioEffectPlayer
import com.example.core.audio.AudioFileManager
import com.example.core.audio.SavedAudioItem
import com.example.core.call.AiCallEngine
import com.example.core.call.CallHistoryItem
import com.example.core.call.CharacterRepository
import com.example.ui.components.CharacterAvatar
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    audioFileManager: AudioFileManager,
    audioPlayer: AudioEffectPlayer,
    callEngine: AiCallEngine,
    onNavigateToChanger: () -> Unit,
    onNavigateToAiCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recordings, 1: AI Calls

    val savedRecordings by audioFileManager.savedRecordings.collectAsState()
    val callHistory by callEngine.callHistory.collectAsState()
    val playbackState by audioPlayer.playbackState.collectAsState()

    var activeTranscriptModal by remember { mutableStateOf<CallHistoryItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = "History & Logs",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Saved voice recordings and simulated AI call logs",
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        }

        // Tabs Row: Recordings vs AI Calls
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = if (selectedTab == 0) NeonPurple else NeonCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Voice Recordings (${savedRecordings.size})",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) NeonPurple else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_recordings")
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = "AI Call Logs (${callHistory.size})",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) NeonCyan else TextSecondary
                    )
                },
                modifier = Modifier.testTag("tab_calls")
            )
        }

        // Content
        if (selectedTab == 0) {
            // Recordings List
            if (savedRecordings.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.GraphicEq,
                    title = "No Saved Audio Recordings",
                    description = "Record your voice and apply fun effects like Cyber Robot, Deep Voice, or Helium in Voice Changer.",
                    actionLabel = "Open Voice Changer",
                    onAction = onNavigateToChanger
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedRecordings, key = { it.id }) { item ->
                        val isCurrentPlaying = playbackState.activeAudioFile?.absolutePath == item.filePath && playbackState.isPlaying

                        RecordingItemCard(
                            item = item,
                            isPlaying = isCurrentPlaying,
                            onPlayToggle = {
                                if (isCurrentPlaying) {
                                    audioPlayer.pause()
                                } else {
                                    val file = File(item.filePath)
                                    audioPlayer.loadAudio(file)
                                    audioPlayer.setEffect(item.effectType)
                                    audioPlayer.play()
                                }
                            },
                            onShare = { audioFileManager.shareRecording(item) },
                            onDelete = {
                                scope.launch { audioFileManager.deleteRecording(item) }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        } else {
            // AI Call Logs List
            if (callHistory.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Call,
                    title = "No AI Call Logs Yet",
                    description = "Have a real-time simulated voice call with Luna, Arya, or Professor Barnaby in English, Hindi, or Hinglish.",
                    actionLabel = "Start an AI Call",
                    onAction = onNavigateToAiCall
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(callHistory, key = { it.id }) { call ->
                        CallLogCard(
                            call = call,
                            onViewTranscripts = { activeTranscriptModal = call }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // Transcript Modal Dialog
    activeTranscriptModal?.let { call ->
        AlertDialog(
            onDismissRequest = { activeTranscriptModal = null },
            title = {
                Text(
                    text = "Call with ${call.characterName}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    Text(
                        text = "Duration: ${call.durationSeconds}s • Language: ${call.language.displayName}",
                        color = NeonCyan,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(call.transcripts) { t ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (t.isUser) Color(0xFF281E48) else Color(0xFF181333))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = t.senderName,
                                        color = if (t.isUser) NeonPink else NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = t.text,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeTranscriptModal = null }) {
                    Text("Close", color = NeonCyan)
                }
            },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
private fun RecordingItemCard(
    item: SavedAudioItem,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recording_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayToggle,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) NeonPink else NeonPurple)
                    .testTag("play_recording_${item.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF231B47))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.effectType.title,
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier.size(36.dp).testTag("share_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp).testTag("delete_item_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = CrimsonRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CallLogCard(
    call: CallHistoryItem,
    onViewTranscripts: () -> Unit
) {
    val character = CharacterRepository.getCharacter(call.characterId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewTranscripts)
            .testTag("call_log_${call.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterAvatar(
                character = character,
                size = 44.dp,
                showAiBadge = false
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = call.characterName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(call.timestamp)),
                        color = TextMuted,
                        fontSize = 10.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${call.durationSeconds}s duration • ${call.language.displayName}",
                    color = character.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "\"${call.transcriptSnippet}\"",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Transcripts",
                tint = NeonPurple,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E173C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onAction,
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(
                    text = actionLabel,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
