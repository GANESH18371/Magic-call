package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.audio.AudioEffectPlayer
import com.example.core.audio.AudioEffectType
import com.example.core.audio.AudioFileManager
import com.example.core.audio.AudioRecorder
import com.example.core.audio.RecordState
import com.example.ui.components.TechnicalDisclaimerNotice
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
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerScreen(
    audioRecorder: AudioRecorder,
    audioPlayer: AudioEffectPlayer,
    audioFileManager: AudioFileManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recordingInfo by audioRecorder.recordingInfo.collectAsState()
    val playbackState by audioPlayer.playbackState.collectAsState()

    var showPermissionRationale by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val imported = audioFileManager.importAudio(uri)
                if (imported != null) {
                    audioPlayer.loadAudio(imported)
                    audioPlayer.play()
                }
            }
        }
    }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            audioRecorder.startRecording()
        } else {
            showPermissionRationale = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Changer",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Audio Import Button
                    OutlinedButton(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        modifier = Modifier.testTag("import_audio_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Audio", color = NeonCyan, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Record or import sound to transform with pitch, speed & vocal filters",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            }
        }

        // Recording Studio Deck
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        if (recordingInfo.state == RecordState.RECORDING) CrimsonRed else Color(0xFF28204D),
                        RoundedCornerShape(20.dp)
                    ),
                color = SurfaceElevated
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Recording Timer & Status
                    val timerText = formatDuration(
                        if (recordingInfo.state == RecordState.RECORDING || recordingInfo.state == RecordState.PAUSED) {
                            recordingInfo.durationMs
                        } else {
                            playbackState.durationMs.toLong()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (recordingInfo.state) {
                                            RecordState.RECORDING -> CrimsonRed
                                            RecordState.PAUSED -> NeonAmber
                                            else -> NeonGreen
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (recordingInfo.state) {
                                    RecordState.RECORDING -> "RECORDING LIVE"
                                    RecordState.PAUSED -> "RECORDING PAUSED"
                                    RecordState.FINISHED -> "AUDIO LOADED"
                                    else -> "STUDIO READY"
                                },
                                color = when (recordingInfo.state) {
                                    RecordState.RECORDING -> CrimsonRed
                                    RecordState.PAUSED -> NeonAmber
                                    else -> NeonGreen
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Text(
                            text = timerText,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Waveform Visualizer
                    WaveformVisualizer(
                        amplitudes = recordingInfo.amplitudeHistory,
                        isActive = recordingInfo.state == RecordState.RECORDING || playbackState.isPlaying,
                        height = 68.dp,
                        gradientColors = if (recordingInfo.state == RecordState.RECORDING) {
                            listOf(CrimsonRed, NeonAmber, NeonPink)
                        } else {
                            listOf(NeonCyan, NeonPurple, NeonPink)
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Record Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (recordingInfo.state == RecordState.RECORDING) {
                            // Stop recording button
                            IconButton(
                                onClick = {
                                    val file = audioRecorder.stopRecording()
                                    if (file != null && file.exists()) {
                                        audioPlayer.loadAudio(file)
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonRed)
                                    .testTag("stop_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        } else {
                            // Start recording button
                            IconButton(
                                onClick = {
                                    val hasMicPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasMicPermission) {
                                        audioPlayer.stop()
                                        audioRecorder.startRecording()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NeonPurple, NeonPink))
                                    )
                                    .testTag("start_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (recordingInfo.state == RecordState.RECORDING) "Tap square to finish recording" else "Tap microphone to record voice",
                        color = TextMuted,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // Voice Effects Gallery
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT VOICE EFFECT",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Before / After A/B Compare Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (playbackState.isComparingOriginal) NeonAmber.copy(alpha = 0.25f)
                                else Color(0xFF221C42)
                            )
                            .border(
                                1.dp,
                                if (playbackState.isComparingOriginal) NeonAmber else Color(0xFF382F66),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                audioPlayer.toggleCompareOriginal(!playbackState.isComparingOriginal)
                            }
                            .testTag("ab_compare_toggle_button"),
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare",
                                tint = if (playbackState.isComparingOriginal) NeonAmber else NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (playbackState.isComparingOriginal) "Playing: ORIGINAL" else "A/B Compare",
                                color = if (playbackState.isComparingOriginal) NeonAmber else TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AudioEffectType.entries) { effect ->
                        val isSelected = playbackState.currentEffect == effect
                        EffectSelectorChip(
                            effect = effect,
                            isSelected = isSelected,
                            onClick = {
                                audioPlayer.setEffect(effect)
                                if (!playbackState.isPlaying && playbackState.activeAudioFile != null) {
                                    audioPlayer.play()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Fine-Tuning Controls (Pitch, Speed, Volume)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(Color(0xFF2B2252), Color(0xFF1D173A)))
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "VOICE MODULATION CONTROLS",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pitch Shift", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                String.format(Locale.US, "%.2fx", playbackState.pitch),
                                color = NeonPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = playbackState.pitch,
                            onValueChange = { audioPlayer.setPitch(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple
                            ),
                            modifier = Modifier.testTag("pitch_slider")
                        )
                    }

                    // Speed / Tempo Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tempo / Speed", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                String.format(Locale.US, "%.2fx", playbackState.speed),
                                color = NeonCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = playbackState.speed,
                            onValueChange = { audioPlayer.setSpeed(it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("speed_slider")
                        )
                    }

                    // Output Volume Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Output Volume", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${(playbackState.volume * 100).toInt()}%",
                                color = NeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = playbackState.volume,
                            onValueChange = { audioPlayer.setVolume(it) },
                            valueRange = 0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonGreen,
                                activeTrackColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("volume_slider")
                        )
                    }
                }
            }
        }

        // Playback Bar and Save / Share Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Playback progress slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(playbackState.currentPositionMs.toLong()),
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                        Slider(
                            value = if (playbackState.durationMs > 0) {
                                playbackState.currentPositionMs.toFloat() / playbackState.durationMs
                            } else 0f,
                            onValueChange = { ratio ->
                                val target = (ratio * playbackState.durationMs).toInt()
                                audioPlayer.seekTo(target)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                                .testTag("playback_seek_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = NeonPurple,
                                activeTrackColor = NeonPurple
                            )
                        )
                        Text(
                            text = formatDuration(playbackState.durationMs.toLong()),
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buttons Row: Play/Pause, Save, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play / Pause Button
                        Button(
                            onClick = { audioPlayer.togglePlayPause() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("play_pause_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (playbackState.isPlaying) "Pause" else "Play Effect",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Save Button
                        OutlinedButton(
                            onClick = {
                                saveTitleInput = "${playbackState.currentEffect.title} Voice"
                                showSaveDialog = true
                            },
                            enabled = playbackState.activeAudioFile != null,
                            modifier = Modifier.testTag("save_audio_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", color = NeonCyan)
                        }

                        // Share Button
                        OutlinedButton(
                            onClick = {
                                playbackState.activeAudioFile?.let { file ->
                                    scope.launch {
                                        val saved = audioFileManager.saveRecording(
                                            sourceFile = file,
                                            customTitle = "${playbackState.currentEffect.title} Audio",
                                            effectType = playbackState.currentEffect,
                                            durationMs = playbackState.durationMs.toLong()
                                        )
                                        if (saved != null) {
                                            audioFileManager.shareRecording(saved)
                                        }
                                    }
                                }
                            },
                            enabled = playbackState.activeAudioFile != null,
                            modifier = Modifier.testTag("share_audio_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = NeonPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", color = NeonPink)
                        }
                    }

                    // Success Message banner
                    saveSuccessMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = msg, color = NeonGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Technical Notice banner
        item {
            TechnicalDisclaimerNotice(
                shortDescription = "Effects apply to recorded audio. Cellular live phone call voice modification is prohibited by Android OS sandbox policies."
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Save Recording Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Processed Audio", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Choose a name for your modified voice clip:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("Recording Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = playbackState.activeAudioFile
                        if (file != null) {
                            scope.launch {
                                audioFileManager.saveRecording(
                                    sourceFile = file,
                                    customTitle = saveTitleInput,
                                    effectType = playbackState.currentEffect,
                                    durationMs = playbackState.durationMs.toLong()
                                )
                                saveSuccessMessage = "Saved '$saveTitleInput' to History!"
                                showSaveDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Save Recording", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevated
        )
    }

    // Permission Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Microphone Permission Required", color = TextPrimary) },
            text = {
                Text(
                    text = "Magic Voice AI needs microphone access to record your voice so you can apply filters and sound effects. Your recordings remain strictly on your device.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(onClick = {
                    showPermissionRationale = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
private fun EffectSelectorChip(
    effect: AudioEffectType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                brush = if (isSelected) Brush.linearGradient(listOf(NeonPurple, NeonPink))
                else Brush.linearGradient(listOf(Color(0xFF28204D), Color(0xFF1E1738))),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("effect_chip_${effect.id}"),
        color = if (isSelected) Color(0xFF241A4A) else SurfaceDark
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) NeonPurple.copy(alpha = 0.35f) else Color(0xFF1B1636)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = effect.icon,
                    contentDescription = effect.title,
                    tint = if (isSelected) NeonPink else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = effect.title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = effect.tag,
                color = if (isSelected) NeonCyan else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val millis = (ms % 1000) / 100
    return String.format(Locale.US, "%02d:%02d.%d", min, sec, millis)
}
