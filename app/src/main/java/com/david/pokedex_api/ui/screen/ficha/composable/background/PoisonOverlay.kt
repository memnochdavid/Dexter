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

private val BubbleColors = listOf(
    Color(0xFF9C27B0), // púrpura
    Color(0xFF7B1FA2), // púrpura oscuro
    Color(0xFFAB47BC), // púrpura claro
    Color(0xFF6A1B9A), // violeta
    Color(0xFF8E24AA), // púrpura medio
)

@Composable
fun PoisonOverlay(modifier: Modifier = Modifier) {
    val bubbles = remember {
        List(18) {
            Particle(
                x = Random.nextFloat(),
                y = 0.7f + Random.nextFloat() * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.0006f,
                vy = -(0.001f + Random.nextFloat() * 0.002f),
                alpha = 0.3f + Random.nextFloat() * 0.35f,
                size = 0.02f + Random.nextFloat() * 0.035f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "poison")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "poisonTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Humo tóxico desde abajo
        for (i in 0 until 3) {
            val smokeY = h * (0.65f + i * 0.12f)
            val wave = sin((time * 1.5f + i * 0.4f) * Math.PI.toFloat() * 2f)
            val smokeAlpha = (0.06f + wave * 0.03f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF6A1B9A).copy(alpha = smokeAlpha),
                        Color(0xFF4A148C).copy(alpha = smokeAlpha * 0.7f),
                        Color.Transparent
                    ),
                    startY = smokeY - h * 0.12f,
                    endY = smokeY + h * 0.12f
                ),
                size = size
            )
        }

        // Burbujas tóxicas ascendentes
        bubbles.forEach { b ->
            b.life -= 0.003f
            b.y += b.vy
            b.x += b.vx + sin((time + b.seed) * 12f) * 0.0004f

            // Crecen al subir
            val ageFactor = (1f - b.life).coerceIn(0f, 1f)
            val growScale = 1f + ageFactor * 0.8f

            // Respawn
            if (b.life <= 0f || b.y < -0.05f) {
                b.y = 0.9f + b.seed * 0.15f
                b.x = Random.nextFloat()
                b.life = 0.7f + b.seed * 0.3f
            }

            // Pop: desvanecerse rápido al final
            val fadeAlpha = if (b.life < 0.1f) b.life / 0.1f else 1f
            val alpha = b.alpha * fadeAlpha

            val colorIndex = (b.seed * BubbleColors.size).toInt().coerceIn(0, BubbleColors.lastIndex)
            val color = BubbleColors[colorIndex]
            val radius = b.size * h * growScale

            val cx = b.x * w
            val cy = b.y * h
            val center = Offset(cx, cy)

            // Burbuja: borde más visible que el interior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha * 0.15f),
                        color.copy(alpha = alpha * 0.1f),
                        color.copy(alpha = alpha * 0.5f),
                        color.copy(alpha = alpha * 0.6f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Reflejo de burbuja
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.25f),
                radius = radius * 0.2f,
                center = Offset(cx - radius * 0.3f, cy - radius * 0.3f)
            )
        }
    }
}
