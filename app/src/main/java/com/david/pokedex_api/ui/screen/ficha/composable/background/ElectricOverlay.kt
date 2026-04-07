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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.floor
import kotlin.random.Random

private val BoltColors = listOf(
    Color(0xFFFFE600), // amarillo brillante
    Color(0xFFFFD700), // dorado
    Color(0xFFFFF176), // amarillo claro
)

private data class BoltDef(
    val x1: Float, val y1: Float,  // punto inicio (normalizado 0..1)
    val x2: Float, val y2: Float,  // punto fin
    val segments: Int,             // zigzag segments
    val width: Float,              // grosor
    val colorIndex: Int,
    val phaseOffset: Float,        // desfase temporal para que no aparezcan todos a la vez
    val duration: Float            // duración visible (0..1)
)

@Composable
fun ElectricOverlay(modifier: Modifier = Modifier) {
    val bolts = remember {
        List(12) {
            val startX = Random.nextFloat()
            val startY = Random.nextFloat() * 0.3f
            val endX = startX + Random.nextFloat() * 0.4f - 0.2f
            val endY = startY + 0.3f + Random.nextFloat() * 0.5f
            BoltDef(
                x1 = startX, y1 = startY,
                x2 = endX.coerceIn(0.05f, 0.95f), y2 = endY.coerceIn(0.4f, 0.95f),
                segments = 5 + Random.nextInt(4),
                width = 2f + Random.nextFloat() * 3f,
                colorIndex = Random.nextInt(BoltColors.size),
                phaseOffset = Random.nextFloat(),
                duration = 0.08f + Random.nextFloat() * 0.1f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "electric")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "electricTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Flash de fondo sutil cuando hay rayos visibles
        var anyVisible = false

        bolts.forEach { bolt ->
            // Cada rayo aparece brevemente en un momento del ciclo
            val cycle = (time + bolt.phaseOffset) % 1f
            // Visible durante bolt.duration, luego invisible
            // Repetir 2-3 veces por ciclo para más actividad
            val flickerCycle = (cycle * 3f) % 1f
            val visible = flickerCycle < bolt.duration

            if (visible) {
                anyVisible = true
                val alpha = (1f - flickerCycle / bolt.duration).coerceIn(0f, 1f) * 0.8f
                val color = BoltColors[bolt.colorIndex].copy(alpha = alpha)

                // Generar zigzag con seed basado en el ciclo para que cambie forma cada vez
                val seed = floor((time + bolt.phaseOffset) * 3f).toLong()
                drawBolt(
                    x1 = bolt.x1 * w, y1 = bolt.y1 * h,
                    x2 = bolt.x2 * w, y2 = bolt.y2 * h,
                    segments = bolt.segments,
                    jitter = w * 0.06f,
                    color = color,
                    strokeWidth = bolt.width * (0.5f + alpha * 0.5f),
                    seed = seed
                )

                // Glow
                drawBolt(
                    x1 = bolt.x1 * w, y1 = bolt.y1 * h,
                    x2 = bolt.x2 * w, y2 = bolt.y2 * h,
                    segments = bolt.segments,
                    jitter = w * 0.06f,
                    color = color.copy(alpha = alpha * 0.3f),
                    strokeWidth = bolt.width * 3f,
                    seed = seed
                )
            }
        }

        // Flash ambiental
        if (anyVisible) {
            drawRect(
                color = Color(0xFFFFE600).copy(alpha = 0.04f),
                size = size
            )
        }
    }
}

private fun DrawScope.drawBolt(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    segments: Int,
    jitter: Float,
    color: Color,
    strokeWidth: Float,
    seed: Long
) {
    val rng = Random(seed)
    val path = Path().apply {
        moveTo(x1, y1)
        val dx = (x2 - x1) / segments
        val dy = (y2 - y1) / segments
        for (i in 1 until segments) {
            val px = x1 + dx * i + (rng.nextFloat() - 0.5f) * jitter * 2f
            val py = y1 + dy * i + (rng.nextFloat() - 0.5f) * jitter * 0.5f
            lineTo(px, py)
        }
        lineTo(x2, y2)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
