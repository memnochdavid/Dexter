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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val SparkleColors = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFFFB6C1), // rosa claro
    Color(0xFFFFC0CB), // rosa
    Color(0xFFE8B4F8), // lavanda
    Color(0xFFFFF0F5), // blanco rosado
)

@Composable
fun FairyOverlay(modifier: Modifier = Modifier) {
    val sparkles = remember {
        List(20) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.0004f,
                vy = -(0.0003f + Random.nextFloat() * 0.0005f),
                alpha = 0f,
                size = 0.015f + Random.nextFloat() * 0.02f,
                life = Random.nextFloat(),
                rotation = Random.nextFloat() * 360f,
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "fairy")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fairyTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        sparkles.forEach { s ->
            s.life += 0.005f
            s.x += s.vx + sin((time + s.seed) * 8f) * 0.0003f
            s.y += s.vy

            // Ciclo: aparecer, brillar, desaparecer
            if (s.life > 1f) {
                s.life = 0f
                s.x = Random.nextFloat()
                s.y = Random.nextFloat()
            }

            // Alpha: sube rápido, se mantiene, baja
            val alpha = when {
                s.life < 0.15f -> s.life / 0.15f
                s.life < 0.6f -> 1f
                else -> 1f - (s.life - 0.6f) / 0.4f
            } * 0.7f

            // Pulso de brillo
            val pulse = sin((time + s.seed) * Math.PI.toFloat() * 4f) * 0.5f + 0.5f
            val finalAlpha = alpha * (0.5f + pulse * 0.5f)

            val colorIndex = (s.seed * SparkleColors.size).toInt().coerceIn(0, SparkleColors.lastIndex)
            val color = SparkleColors[colorIndex]
            val starSize = s.size * h * (0.7f + pulse * 0.3f)

            val cx = s.x * w
            val cy = s.y * h

            // Glow suave
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = finalAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = starSize * 3f
                ),
                radius = starSize * 3f,
                center = Offset(cx, cy)
            )

            // Estrella de 4 puntas
            s.rotation += 0.3f
            drawStar4(
                cx = cx,
                cy = cy,
                outerRadius = starSize,
                innerRadius = starSize * 0.3f,
                rotation = s.rotation,
                color = color.copy(alpha = finalAlpha)
            )
        }
    }
}

private fun DrawScope.drawStar4(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float,
    rotation: Float,
    color: Color
) {
    val path = Path().apply {
        for (i in 0 until 8) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = Math.toRadians((i * 45.0)).toFloat()
            val px = cx + cos(angle) * r
            val py = cy + sin(angle) * r
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    rotate(degrees = rotation, pivot = Offset(cx, cy)) {
        drawPath(path, color)
    }
}
