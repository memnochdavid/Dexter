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

private val OrbColors = listOf(
    Color(0xFF7B1FA2), // púrpura oscuro
    Color(0xFF9C27B0), // púrpura
    Color(0xFF6A1B9A), // violeta profundo
    Color(0xFFAB47BC), // púrpura claro
    Color(0xFF4A148C), // índigo oscuro
)

@Composable
fun GhostOverlay(modifier: Modifier = Modifier) {
    val orbs = remember {
        List(12) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.0008f,
                vy = (Random.nextFloat() - 0.5f) * 0.0006f,
                alpha = 0.15f + Random.nextFloat() * 0.2f,
                size = 0.06f + Random.nextFloat() * 0.08f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ghost")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ghostTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Niebla: franjas horizontales que pulsan
        for (i in 0 until 4) {
            val fogY = h * (0.1f + i * 0.25f)
            val fogAlpha = (sin((time + i * 0.3f) * Math.PI.toFloat() * 2f) * 0.5f + 0.5f) * 0.06f
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF7B1FA2).copy(alpha = fogAlpha),
                        Color.Transparent
                    ),
                    startY = fogY - h * 0.08f,
                    endY = fogY + h * 0.08f
                ),
                size = size
            )
        }

        // Orbes flotantes
        orbs.forEach { orb ->
            // Movimiento errático lento
            orb.x += orb.vx + sin((time + orb.seed) * 5f) * 0.0005f
            orb.y += orb.vy + sin((time + orb.seed * 2f) * 4f) * 0.0004f

            // Wrap around
            if (orb.x < -0.1f) orb.x = 1.1f
            if (orb.x > 1.1f) orb.x = -0.1f
            if (orb.y < -0.1f) orb.y = 1.1f
            if (orb.y > 1.1f) orb.y = -0.1f

            // Pulsación de opacidad
            val pulse = sin((time + orb.seed) * Math.PI.toFloat() * 2f * 1.5f) * 0.5f + 0.5f
            val alpha = orb.alpha * (0.4f + pulse * 0.6f)
            val radius = orb.size * h * (0.85f + pulse * 0.15f)

            val colorIndex = (orb.seed * OrbColors.size).toInt().coerceIn(0, OrbColors.lastIndex)
            val color = OrbColors[colorIndex]

            val center = Offset(orb.x * w, orb.y * h)

            // Glow exterior
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha * 0.6f),
                        color.copy(alpha = alpha * 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 2f
                ),
                radius = radius * 2f,
                center = center
            )

            // Núcleo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = alpha),
                        color.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}
