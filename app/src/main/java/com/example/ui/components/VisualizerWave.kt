package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
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
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

@Composable
fun VisualizerWave(
    bars: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barCount: Int = 16,
    accentColor: Color = NeonPurple
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val safeBars = if (bars.isEmpty()) List(barCount) { 0.1f } else bars
        val count = minOf(safeBars.size, barCount)
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((totalWidth - totalSpacing) / count).coerceAtLeast(2f)

        val gradient = Brush.verticalGradient(
            colors = listOf(
                NeonPink,
                accentColor,
                CyberCyan
            )
        )

        for (i in 0 until count) {
            val barAmp = safeBars[i].coerceIn(0.08f, 1.0f)
            val barH = (canvasHeight * barAmp).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = canvasHeight - barH

            // Bar background
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )

            // Top peak dot when active
            if (isPlaying && barAmp > 0.25f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = (barWidth / 2.5f).coerceAtMost(3.dp.toPx()),
                    center = Offset(x + barWidth / 2, (y - 3.dp.toPx()).coerceAtLeast(2.dp.toPx()))
                )
            }
        }
    }
}
