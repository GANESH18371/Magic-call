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

import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.core.call.SupportedLanguage
import com.example.ui.components.CharacterAvatar
import com.example.ui.components.TechnicalDisclaimerNotice
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricBlue
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

@Composable
fun AiCallScreen(
    onStartOutgoingCall: (AiCharacter, SupportedLanguage, Boolean) -> Unit,
    onSimulateIncomingCall: (AiCharacter, SupportedLanguage, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLanguage by remember { mutableStateOf(SupportedLanguage.ENGLISH) }
    var isDemoMode by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Magic Call",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    // Fictional AI notice pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B1536))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Fictional",
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Fictional AI",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Live conversational speech with original synthetic characters",
                    color = TextSecondary,
                    fontSize = 12.5.sp
                )
            }
        }

        // Language & Demo Mode Controller Deck
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conversation Language",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Demo Mode Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDemoMode) "Demo Engine" else "Gemini Live",
                                color = if (isDemoMode) NeonPurple else NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = isDemoMode,
                                onCheckedChange = { isDemoMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPurple,
                                    checkedTrackColor = Color(0xFF381E66)
                                ),
                                modifier = Modifier.testTag("demo_mode_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Language Filter Chips: English, Hindi, Hinglish
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SupportedLanguage.entries.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLanguage = lang },
                                label = {
                                    Text(
                                        text = lang.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = SurfaceDark,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) NeonCyan else Color(0xFF2C2554)
                                ),
                                modifier = Modifier.testTag("lang_chip_${lang.code}")
                            )
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "SELECT AN AI CHARACTER TO CALL",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Characters Gallery
        items(CharacterRepository.characters) { character ->
            AiCharacterCallingCard(
                character = character,
                selectedLanguage = selectedLanguage,
                isDemoMode = isDemoMode,
                onCall = { onStartOutgoingCall(character, selectedLanguage, isDemoMode) },
                onSimulateIncoming = { onSimulateIncomingCall(character, selectedLanguage, isDemoMode) }
            )
        }

        // Ethical Guidelines & Technical Transparency
        item {
            TechnicalDisclaimerNotice(
                title = "AI Voice Ethics & Calling Limitations",
                shortDescription = "All characters are purely fictional AI personalities with synthetic voices. Calls are simulated app sessions; no actual cellular dialer or real persons are involved."
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun AiCharacterCallingCard(
    character: AiCharacter,
    selectedLanguage: SupportedLanguage,
    isDemoMode: Boolean,
    onCall: () -> Unit,
    onSimulateIncoming: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_card_${character.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(
                    character.primaryColor.copy(alpha = 0.6f),
                    character.secondaryColor.copy(alpha = 0.25f),
                    Color(0xFF211A42)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CharacterAvatar(
                    character = character,
                    size = 54.dp,
                    showAiBadge = true
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = character.name,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(character.primaryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = character.personalityStyle,
                                color = character.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = character.title,
                        color = character.secondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = character.tagline,
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Greeting preview in selected language
            val greeting = when (selectedLanguage) {
                SupportedLanguage.HINDI -> character.greetingHindi
                SupportedLanguage.HINGLISH -> character.greetingHinglish
                SupportedLanguage.ENGLISH -> character.greetingEnglish
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1736))
                    .padding(12.dp)
            ) {
                Text(
                    text = "\"$greeting\"",
                    color = Color(0xFFD6D0F5),
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Outgoing Call Button
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_call_${character.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = character.primaryColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call ${character.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Simulate Incoming Call Button
                OutlinedButton(
                    onClick = onSimulateIncoming,
                    modifier = Modifier.testTag("incoming_call_${character.id}"),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(NeonCyan, ElectricBlue))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Simulate Incoming",
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Incoming Ring",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
