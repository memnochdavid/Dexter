package com.david.pokedex_api.ui.screen.ficha.composable.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

private val WaveColors = listOf(
    Color(0xFF1E90FF).copy(alpha = 0.18f), // azul claro
    Color(0xFF4169E1).copy(alpha = 0.14f), // azul royal
    Color(0xFF87CEEB).copy(alpha = 0.12f), // celeste
    Color(0xFF00BFFF).copy(alpha = 0.16f), // azul profundo
    Color(0xFF5DADE2).copy(alpha = 0.10f), // azul suave
)

@Composable
fun WaterOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "water")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waterTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        WaveColors.forEachIndexed { i, color ->
            val speed = 1f + i * 0.4f
            val amplitude = h * (0.03f + i * 0.008f)
            val yBase = h * (0.15f + i * 0.17f)
            val frequency = 1.5f + i * 0.3f
            val phase = time * speed * Math.PI.toFloat() * 2f

            drawWave(
                w = w,
                h = h,
                yBase = yBase,
                amplitude = amplitude,
                frequency = frequency,
                phase = phase,
                color = color
            )
        }
    }
}

private fun DrawScope.drawWave(
    w: Float,
    h: Float,
    yBase: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    color: Color
) {
    val path = Path().apply {
        val steps = 50
        val dx = w / steps

        moveTo(0f, h)
        lineTo(0f, yBase + sin(phase) * amplitude)

        for (step in 1..steps) {
            val x = step * dx
            val normalizedX = x / w * frequency * Math.PI.toFloat() * 2f
            val y = yBase + sin(normalizedX + phase) * amplitude
            lineTo(x, y)
        }

        lineTo(w, h)
        close()
    }

    drawPath(path, color)
}
