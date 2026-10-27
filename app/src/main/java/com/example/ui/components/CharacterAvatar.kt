package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.call.AiCharacter
import com.example.ui.theme.NeonGreen

@Composable
fun CharacterAvatar(
    character: AiCharacter,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isPulsing: Boolean = false,
    pulseAmplitude: Float = 0.1f,
    showAiBadge: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f + (pulseAmplitude * 0.25f),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(size + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ripple
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size + 20.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        character.primaryColor.copy(alpha = 0.20f)
                    )
            )
            Box(
                modifier = Modifier
                    .size(size + 10.dp)
                    .scale(1.0f + (pulseAmplitude * 0.12f))
                    .clip(CircleShape)
                    .background(
                        character.secondaryColor.copy(alpha = 0.35f)
                    )
            )
        }

        // Main circular avatar
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            character.primaryColor,
                            character.secondaryColor,
                            Color(0xFF0F0B24)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(character.primaryColor, character.secondaryColor)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val initials = character.name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")

            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // AI badge indicator
        if (showAiBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0C24))
                    .border(1.5.dp, NeonGreen, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AI",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
