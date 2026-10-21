package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VinylDisc(
    isPlaying: Boolean,
    labelColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .rotate(currentRotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val radius = this.size.minDimension / 2

            // Outer Vinyl Disc body
            drawCircle(
                color = Color(0xFF110E1B),
                radius = radius,
                center = center
            )

            // Vinyl Sheen sweep
            val sheenBrush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF1A162B),
                    Color(0xFF2E264E),
                    Color(0xFF1A162B),
                    Color(0xFF382F5F),
                    Color(0xFF1A162B)
                ),
                center = center
            )
            drawCircle(
                brush = sheenBrush,
                radius = radius * 0.96f,
                center = center
            )

            // Concentric Grooves
            val grooveCount = 7
            for (i in 1..grooveCount) {
                val grooveRadius = radius * (0.42f + (i.toFloat() / grooveCount) * 0.52f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f + (i % 2) * 0.03f),
                    radius = grooveRadius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Center Label (Track Tint)
            drawCircle(
                color = labelColor,
                radius = radius * 0.36f,
                center = center
            )

            // Inner Ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius * 0.32f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Center Spindle Hole
            drawCircle(
                color = Color(0xFF09070F),
                radius = radius * 0.08f,
                center = center
            )
        }

        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.16f)
        )
    }
}
