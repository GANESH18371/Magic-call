package com.example.core.audio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

enum class AudioEffectType(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val basePitch: Float,
    val baseSpeed: Float,
    val defaultIntensity: Float,
    val isSimulation: Boolean = false,
    val tag: String
) {
    ORIGINAL(
        id = "original",
        title = "Original Voice",
        subtitle = "Natural unmodified audio",
        icon = Icons.Default.Mic,
        basePitch = 1.0f,
        baseSpeed = 1.0f,
        defaultIntensity = 1.0f,
        isSimulation = false,
        tag = "Natural"
    ),
    DEEP_MALE(
        id = "deep_male",
        title = "Deep Masculine",
        subtitle = "Baritone resonance & gravitas",
        icon = Icons.Default.Person,
        basePitch = 0.72f,
        baseSpeed = 0.95f,
        defaultIntensity = 0.85f,
        isSimulation = false,
        tag = "Pitch Mod"
    ),
    FEMALE_STYLE(
        id = "female_style",
        title = "Female Style",
        subtitle = "Elevated pitch with crisp timbre",
        icon = Icons.Default.Face,
        basePitch = 1.42f,
        baseSpeed = 1.05f,
        defaultIntensity = 0.90f,
        isSimulation = false,
        tag = "Pitch Mod"
    ),
    CYBER_ROBOT(
        id = "cyber_robot",
        title = "Cyber Robot",
        subtitle = "Futuristic metallic synthesis",
        icon = Icons.Default.SmartToy,
        basePitch = 0.88f,
        baseSpeed = 0.98f,
        defaultIntensity = 1.0f,
        isSimulation = false,
        tag = "Synthetic"
    ),
    CARTOON_CHIPMUNK(
        id = "cartoon_chipmunk",
        title = "Cartoon Chipmunk",
        subtitle = "Playful high-speed animated voice",
        icon = Icons.Default.Whatshot,
        basePitch = 1.80f,
        baseSpeed = 1.25f,
        defaultIntensity = 0.95f,
        isSimulation = false,
        tag = "Fun FX"
    ),
    GIANT_DEMON(
        id = "giant_demon",
        title = "Deep Monster",
        subtitle = "Sub-bass subterranean roar",
        icon = Icons.Default.GraphicEq,
        basePitch = 0.52f,
        baseSpeed = 0.82f,
        defaultIntensity = 1.0f,
        isSimulation = false,
        tag = "Extreme"
    ),
    HELIUM_BALLOON(
        id = "helium_balloon",
        title = "Helium Balloon",
        subtitle = "Squeaky gas inhalation effect",
        icon = Icons.Default.RecordVoiceOver,
        basePitch = 1.95f,
        baseSpeed = 1.12f,
        defaultIntensity = 1.0f,
        isSimulation = false,
        tag = "High Pitch"
    ),
    COSMIC_ECHO(
        id = "cosmic_echo",
        title = "Cosmic Reverb",
        subtitle = "Spatial chamber ambience",
        icon = Icons.Default.Waves,
        basePitch = 1.05f,
        baseSpeed = 0.95f,
        defaultIntensity = 0.75f,
        isSimulation = false,
        tag = "Spatial"
    ),
    EXTRATERRESTRIAL(
        id = "extraterrestrial",
        title = "Alien Hive",
        subtitle = "Warped interdimensional frequency",
        icon = Icons.Default.Psychology,
        basePitch = 1.35f,
        baseSpeed = 0.90f,
        defaultIntensity = 0.85f,
        isSimulation = false,
        tag = "Sci-Fi"
    ),
    RETRO_RADIO(
        id = "retro_radio",
        title = "Walkie Talkie",
        subtitle = "Lo-fi crunchy communications",
        icon = Icons.Default.Radio,
        basePitch = 1.18f,
        baseSpeed = 1.02f,
        defaultIntensity = 0.70f,
        isSimulation = false,
        tag = "Lo-Fi"
    );

    companion object {
        fun fromId(id: String): AudioEffectType =
            entries.find { it.id == id } ?: ORIGINAL
    }
}

data class VoiceEffectSettings(
    val effectType: AudioEffectType = AudioEffectType.ORIGINAL,
    val pitchMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f,
    val intensity: Float = 1.0f,
    val volume: Float = 1.0f,
    val isComparingOriginal: Boolean = false // A/B comparison toggle
) {
    val effectivePitch: Float
        get() = if (isComparingOriginal) 1.0f else (effectType.basePitch * pitchMultiplier).coerceIn(0.5f, 2.0f)

    val effectiveSpeed: Float
        get() = if (isComparingOriginal) 1.0f else (effectType.baseSpeed * speedMultiplier).coerceIn(0.5f, 2.0f)
}
