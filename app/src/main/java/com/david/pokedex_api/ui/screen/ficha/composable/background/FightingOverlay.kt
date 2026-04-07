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
import kotlin.math.floor
import kotlin.random.Random

private val ImpactColors = listOf(
    Color(0xFFD32F2F), // rojo oscuro
    Color(0xFFE53935), // rojo
    Color(0xFFFF5722), // rojo-naranja
    Color(0xFFBF360C), // marrón-rojo
)

private data class ImpactWave(
    val cx: Float,
    val cy: Float,
    val maxRadius: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
    val speed: Float
)

@Composable
fun FightingOverlay(modifier: Modifier = Modifier) {
    val impacts = remember {
        List(7) {
            ImpactWave(
                cx = 0.1f + Random.nextFloat() * 0.8f,
                cy = 0.1f + Random.nextFloat() * 0.8f,
                maxRadius = 0.15f + Random.nextFloat() * 0.2f,
                phaseOffset = Random.nextFloat(),
                colorIndex = Random.nextInt(ImpactColors.size),
                speed = 0.7f + Random.nextFloat() * 0.8f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "fighting")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fightingTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minDim = minOf(w, h)

        impacts.forEach { impact ->
            val color = ImpactColors[impact.colorIndex]
            val center = Offset(impact.cx * w, impact.cy * h)
            val maxR = impact.maxRadius * minDim

            // Ciclo de impacto: expandir rápido y desvanecer
            val cycle = (time * impact.speed + impact.phaseOffset) % 1f

            // Flash en el punto de impacto al inicio
            if (cycle < 0.1f) {
                val flashAlpha = (1f - cycle / 0.1f) * 0.2f
                drawCircle(
                    color = Color.White.copy(alpha = flashAlpha),
                    radius = maxR * 0.15f,
                    center = center
                )
            }

            // 2 ondas de impacto desfasadas
            for (ring in 0 until 2) {
                val ringPhase = ((cycle + ring * 0.15f)).coerceIn(0f, 1f)
                // Expansión rápida (ease-out)
                val expandProgress = 1f - (1f - ringPhase) * (1f - ringPhase)
                val radius = expandProgress * maxR
                val alpha = (1f - expandProgress) * 0.35f

                if (alpha > 0.01f) {
                    val strokeWidth = (1f - expandProgress) * 4f + 1.5f
                    drawCircle(
                        color = color.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            // Líneas de velocidad radiales en el impacto
            if (cycle < 0.4f) {
                val lineAlpha = (1f - cycle / 0.4f) * 0.25f
                val seed = floor(time * impact.speed + impact.phaseOffset).toLong()
                val rng = Random(seed + (impact.cx * 1000).toLong())
                for (i in 0 until 5) {
                    val angle = rng.nextFloat() * Math.PI.toFloat() * 2f
                    val innerR = maxR * 0.3f * (cycle / 0.4f)
                    val outerR = maxR * (0.5f + cycle * 0.8f)
                    val cos = kotlin.math.cos(angle)
                    val sin = kotlin.math.sin(angle)

                    drawLine(
                        color = color.copy(alpha = lineAlpha),
                        start = Offset(center.x + cos * innerR, center.y + sin * innerR),
                        end = Offset(center.x + cos * outerR, center.y + sin * outerR),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}
