package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    isActive: Boolean = true,
    barCount: Int = 32,
    gradientColors: List<Color> = listOf(NeonCyan, NeonPurple, NeonPink)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_idle")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = (canvasWidth / barCount) * 0.55f
        val spacing = (canvasWidth / barCount) * 0.45f
        val centerY = canvasHeight / 2f

        val gradient = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(0f, 0f),
            end = Offset(canvasWidth, canvasHeight)
        )

        for (i in 0 until barCount) {
            val amp = if (amplitudes.isNotEmpty() && isActive) {
                val index = (i * amplitudes.size / barCount).coerceIn(0, amplitudes.size - 1)
                amplitudes[index]
            } else if (isActive) {
                // Subtle gentle sine wave when active but silence
                (0.15f + (sin(idlePhase + (i * 0.35f)) * 0.08f)).coerceIn(0.06f, 0.4f)
            } else {
                0.05f
            }

            val barHeight = (canvasHeight * amp).coerceIn(4f, canvasHeight * 0.95f)
            val left = i * (barWidth + spacing) + (spacing / 2)
            val top = centerY - (barHeight / 2)

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
