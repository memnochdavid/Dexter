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

private val FireflyColors = listOf(
    Color(0xFFCDDC39), // lima
    Color(0xFFAEEA00), // verde lima brillante
    Color(0xFF76FF03), // verde neón
    Color(0xFFB2FF59), // verde claro
)

@Composable
fun BugOverlay(modifier: Modifier = Modifier) {
    val fireflies = remember {
        List(15) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.001f,
                vy = (Random.nextFloat() - 0.5f) * 0.001f,
                alpha = 0f,
                size = 0.008f + Random.nextFloat() * 0.01f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "bug")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bugTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fireflies.forEach { f ->
            // Movimiento errático (cambia de dirección suavemente)
            f.x += f.vx + sin((time + f.seed) * 11f) * 0.0008f
            f.y += f.vy + sin((time + f.seed * 2f) * 9f) * 0.0006f

            // Wrap around
            if (f.x < -0.05f) f.x = 1.05f
            if (f.x > 1.05f) f.x = -0.05f
            if (f.y < -0.05f) f.y = 1.05f
            if (f.y > 1.05f) f.y = -0.05f

            // Pulso de brillo: encender, mantener, apagar
            f.life += 0.004f + f.seed * 0.003f
            if (f.life > 1f) f.life = 0f

            val alpha = when {
                f.life < 0.1f -> f.life / 0.1f
                f.life < 0.3f -> 1f
                f.life < 0.45f -> 1f - (f.life - 0.3f) / 0.15f
                f.life < 0.7f -> 0f
                f.life < 0.8f -> (f.life - 0.7f) / 0.1f * 0.6f
                f.life < 0.88f -> 0.6f
                f.life < 0.95f -> 0.6f * (1f - (f.life - 0.88f) / 0.07f)
                else -> 0f
            } * 0.75f

            if (alpha > 0.01f) {
                val colorIndex = (f.seed * FireflyColors.size).toInt().coerceIn(0, FireflyColors.lastIndex)
                val color = FireflyColors[colorIndex]
                val cx = f.x * w
                val cy = f.y * h
                val radius = f.size * h

                // Glow amplio
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 0.35f),
                            color.copy(alpha = alpha * 0.1f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius * 5f
                    ),
                    radius = radius * 5f,
                    center = Offset(cx, cy)
                )

                // Núcleo brillante
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha * 0.9f),
                            color.copy(alpha = alpha * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius * 1.5f
                    ),
                    radius = radius * 1.5f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
