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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.call.AiCharacter
import com.example.core.call.CharacterRepository
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.ModeCard
import com.example.ui.components.TechnicalDisclaimerNotice
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateToVoiceChanger: () -> Unit,
    onNavigateToAiCall: () -> Unit,
    onStartCallWithCharacter: (AiCharacter) -> Unit,
    onSimulateIncomingCallWithCharacter: (AiCharacter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App Header Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI AUDIO ENGINE READY",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Magic Voice AI",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Interactive Demo Mode Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1738))
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Demo Mode",
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dual Mode",
                            color = NeonPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section Title: Select Experience Mode
        item {
            Text(
                text = "CHOOSE APPLICATION MODE",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // MODE 1 CARD: VOICE CHANGER
        item {
            ModeCard(
                title = "1. Voice Changer Studio",
                subtitle = "Record audio or import sound files to apply pitch shift, cartoon, deep monster, cyber robot, helium, and cosmic reverb.",
                badgeText = "AUDIO FX WORKSTATION",
                icon = Icons.Default.GraphicEq,
                primaryColor = NeonPurple,
                secondaryColor = NeonPink,
                featurePills = listOf("Male / Female", "Cyber Robot", "Helium", "A/B Compare", "Audio Waveform"),
                onClick = onNavigateToVoiceChanger,
                testTag = "home_mode_card_voice_changer"
            )
        }

        // MODE 2 CARD: AI MAGIC CALL
        item {
            ModeCard(
                title = "2. AI Magic Call",
                subtitle = "Engage in live two-way voice conversations with original fictional characters like Luna, Arya, and Sterling in English, Hindi & Hinglish.",
                badgeText = "TWO-WAY AI SPEECH",
                icon = Icons.Default.Call,
                primaryColor = NeonCyan,
                secondaryColor = ElectricBlue,
                featurePills = listOf("Gemini Speech", "Hindi & Hinglish", "Simulated Ringing", "Live Transcripts"),
                onClick = onNavigateToAiCall,
                testTag = "home_mode_card_ai_call"
            )
        }

        // Quick Launch Shortcuts Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Record Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2B2456), RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToVoiceChanger)
                        .testTag("quick_record_button"),
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Record Now",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Apply voice FX",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Quick Call Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF2B2456), RoundedCornerShape(16.dp))
                        .clickable(onClick = onNavigateToAiCall)
                        .testTag("quick_call_button"),
                    color = SurfaceDark
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call AI",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Call",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Talk to Luna",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Featured AI Characters Carousel
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FEATURED AI CHARACTERS",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onNavigateToAiCall)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(CharacterRepository.characters) { char ->
                        CharacterQuickCard(
                            character = char,
                            onCall = { onStartCallWithCharacter(char) },
                            onSimulateIncoming = { onSimulateIncomingCallWithCharacter(char) }
                        )
                    }
                }
            }
        }

        // Technical Notice / Ethics & Platform Policies
        item {
            TechnicalDisclaimerNotice(
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Bottom space for navigation bar clearance
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun CharacterQuickCard(
    character: AiCharacter,
    onCall: () -> Unit,
    onSimulateIncoming: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                character.primaryColor.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            ),
        color = SurfaceElevated
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CharacterAvatar(
                    character = character,
                    size = 46.dp,
                    showAiBadge = false
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = character.title,
                        color = character.primaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = character.tagline,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElevatedButton(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_${character.id}_quick_btn"),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = character.primaryColor,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Call",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Incoming simulation test button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF261F4D))
                        .clickable(onClick = onSimulateIncoming)
                        .testTag("incoming_${character.id}_quick_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Simulate Ringing",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
