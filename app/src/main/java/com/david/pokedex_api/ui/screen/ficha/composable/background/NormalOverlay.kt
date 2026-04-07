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

@Composable
fun NormalOverlay(modifier: Modifier = Modifier) {
    val motes = remember {
        List(18) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                vx = (Random.nextFloat() - 0.5f) * 0.0003f,
                vy = (Random.nextFloat() - 0.5f) * 0.0003f,
                alpha = 0.08f + Random.nextFloat() * 0.1f,
                size = 0.01f + Random.nextFloat() * 0.015f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "normal")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "normalTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Respiración suave del fondo
        val breathe = sin(time * Math.PI.toFloat() * 2f) * 0.5f + 0.5f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = breathe * 0.03f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 0.5f
            ),
            size = size
        )

        // Motas flotantes sin dirección fija
        motes.forEach { m ->
            m.x += m.vx + sin((time + m.seed) * 4f) * 0.0002f
            m.y += m.vy + sin((time + m.seed * 2f) * 3.5f) * 0.0002f

            // Wrap around suave
            if (m.x < -0.05f) m.x = 1.05f
            if (m.x > 1.05f) m.x = -0.05f
            if (m.y < -0.05f) m.y = 1.05f
            if (m.y > 1.05f) m.y = -0.05f

            // Pulso lento de opacidad
            val pulse = sin((time * 1.5f + m.seed * 6f) * Math.PI.toFloat() * 2f) * 0.5f + 0.5f
            val alpha = m.alpha * (0.4f + pulse * 0.6f)

            val cx = m.x * w
            val cy = m.y * h
            val radius = m.size * h

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color.White.copy(alpha = alpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius * 2f
                ),
                radius = radius * 2f,
                center = Offset(cx, cy)
            )
        }
    }
}
