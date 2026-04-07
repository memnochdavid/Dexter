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
import kotlin.math.sin
import kotlin.random.Random

private data class ShineStreak(
    val startX: Float,
    val width: Float,
    val speed: Float,
    val phaseOffset: Float,
    val alpha: Float
)

@Composable
fun SteelOverlay(modifier: Modifier = Modifier) {
    val streaks = remember {
        List(6) {
            ShineStreak(
                startX = Random.nextFloat(),
                width = 0.03f + Random.nextFloat() * 0.04f,
                speed = 0.4f + Random.nextFloat() * 0.5f,
                phaseOffset = Random.nextFloat(),
                alpha = 0.08f + Random.nextFloat() * 0.1f
            )
        }
    }

    // Chispas metálicas pequeñas
    val sparks = remember {
        List(12) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = 0f, vy = 0f,
                alpha = 0f,
                size = 0.004f + Random.nextFloat() * 0.006f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "steel")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steelTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val diagonal = w + h

        // Reflejos diagonales que se desplazan (brushed metal)
        streaks.forEach { streak ->
            // Rango extendido para que entre y salga fuera de pantalla sin corte
            val range = diagonal + h
            val progress = ((time * streak.speed + streak.phaseOffset) % 1f) * range - h
            val streakWidth = streak.width * w

            // Fade in/out en los bordes para transición seamless
            val edgeFade = when {
                progress < 0f -> ((progress + h) / h).coerceIn(0f, 1f)
                progress > diagonal - h -> ((diagonal - progress) / h).coerceIn(0f, 1f)
                else -> 1f
            }
            val alpha = streak.alpha * edgeFade

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = alpha * 0.3f),
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    start = Offset(progress - h * 0.2f, 0f),
                    end = Offset(progress + h * 0.2f, h)
                ),
                start = Offset(progress - h, 0f),
                end = Offset(progress, h),
                strokeWidth = streakWidth
            )
        }

        // Pulso metálico sutil general
        val pulseAlpha = (sin(time * Math.PI.toFloat() * 2f) * 0.5f + 0.5f) * 0.03f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = pulseAlpha),
                    Color.Transparent,
                    Color.White.copy(alpha = pulseAlpha * 0.5f)
                )
            ),
            size = size
        )

        // Chispas metálicas que aparecen y desaparecen
        sparks.forEach { spark ->
            spark.life += 0.008f

            if (spark.life > 1f) {
                spark.life = 0f
                spark.x = Random.nextFloat()
                spark.y = Random.nextFloat()
            }

            // Flash rápido
            val alpha = when {
                spark.life < 0.05f -> spark.life / 0.05f
                spark.life < 0.15f -> 1f
                spark.life < 0.25f -> 1f - (spark.life - 0.15f) / 0.1f
                else -> 0f
            } * 0.6f

            if (alpha > 0.01f) {
                val cx = spark.x * w
                val cy = spark.y * h
                val sparkSize = spark.size * h

                // Cruz brillante
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(cx - sparkSize, cy),
                    end = Offset(cx + sparkSize, cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(cx, cy - sparkSize),
                    end = Offset(cx, cy + sparkSize),
                    strokeWidth = 1.5f
                )

                // Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = sparkSize * 3f
                    ),
                    radius = sparkSize * 3f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
