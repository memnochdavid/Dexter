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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.random.Random

private val RingColors = listOf(
    Color(0xFFE91E90), // rosa fuerte
    Color(0xFFFF69B4), // rosa claro
    Color(0xFFDA70D6), // orquídea
    Color(0xFFBA55D3), // violeta medio
)

private data class RingSource(
    val cx: Float,
    val cy: Float,
    val phaseOffset: Float,
    val speed: Float,
    val colorIndex: Int,
    val maxRadius: Float
)

@Composable
fun PsychicOverlay(modifier: Modifier = Modifier) {
    val sources = remember {
        List(5) {
            RingSource(
                cx = 0.15f + Random.nextFloat() * 0.7f,
                cy = 0.15f + Random.nextFloat() * 0.7f,
                phaseOffset = Random.nextFloat(),
                speed = 0.8f + Random.nextFloat() * 0.6f,
                colorIndex = Random.nextInt(RingColors.size),
                maxRadius = 0.25f + Random.nextFloat() * 0.2f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "psychic")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "psychicTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        sources.forEach { src ->
            val color = RingColors[src.colorIndex]
            val center = Offset(src.cx * w, src.cy * h)
            val maxR = src.maxRadius * minDim

            // 3 anillos concéntricos por fuente, desfasados
            for (ring in 0 until 3) {
                val phase = ((time * src.speed + src.phaseOffset + ring * 0.33f) % 1f)
                val radius = phase * maxR
                val alpha = (1f - phase) * 0.2f

                if (alpha > 0.01f) {
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2f + (1f - phase) * 2f)
                    )
                }
            }
        }

        // Pulso central sutil
        val pulseAlpha = (sin(time * Math.PI.toFloat() * 2f) * 0.5f + 0.5f) * 0.04f
        drawCircle(
            color = Color(0xFFFF69B4).copy(alpha = pulseAlpha),
            radius = minDim * 0.4f,
            center = Offset(w * 0.5f, h * 0.5f)
        )
    }
}
