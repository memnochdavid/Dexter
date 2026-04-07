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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

private val FlameColors = listOf(
    Color(0xFFFF4500), // rojo-naranja
    Color(0xFFFF6A00), // naranja
    Color(0xFFFF8C00), // naranja oscuro
    Color(0xFFFFAA00), // ámbar
    Color(0xFFFFCC33), // amarillo-naranja
)

@Composable
fun FireOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        spawnParticles(
            count = 20,
            yRange = 0.3f..1.1f,
            vxRange = -0.001f..0.001f,
            vyRange = -0.006f..-0.002f,
            sizeRange = 0.06f..0.12f
        )
    }

    val transition = rememberInfiniteTransition(label = "fire")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fireTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Avanzar vida
            p.life -= 0.004f
            p.y += p.vy
            p.x += p.vx + sin((time + p.seed) * 20f) * 0.0008f // oscilación lateral

            // Respawn al morir o salir por arriba
            if (p.life <= 0f || p.y < -0.1f) {
                p.y = 0.95f + p.seed * 0.15f
                p.x = p.seed * 0.8f + 0.1f
                p.life = 0.7f + p.seed * 0.3f
                p.alpha = 0.4f + p.seed * 0.4f
            }

            // Fade out según vida
            val fadeAlpha = (p.life.coerceIn(0f, 0.3f) / 0.3f) * p.alpha
            // Escala: más grande al nacer, se encoge al morir
            val scaleFactor = 0.5f + p.life * 0.5f
            val flameSize = p.size * h * scaleFactor

            // Color: amarillo cuando joven, rojo cuando viejo
            val colorIndex = ((1f - p.life) * (FlameColors.size - 1)).toInt()
                .coerceIn(0, FlameColors.size - 1)
            val color = FlameColors[colorIndex].copy(alpha = fadeAlpha)

            // Rotación suave
            val rot = sin((time + p.seed) * 15f) * 12f

            drawFlame(
                cx = p.x * w,
                cy = p.y * h,
                flameHeight = flameSize,
                flameWidth = flameSize * 0.5f,
                rotation = rot,
                color = color
            )
        }
    }
}

private fun DrawScope.drawFlame(
    cx: Float,
    cy: Float,
    flameHeight: Float,
    flameWidth: Float,
    rotation: Float,
    color: Color
) {
    val path = Path().apply {
        // Punta superior (lengua de llama)
        moveTo(cx, cy - flameHeight)
        // Curva derecha
        cubicTo(
            cx + flameWidth * 0.3f, cy - flameHeight * 0.6f,
            cx + flameWidth, cy - flameHeight * 0.2f,
            cx, cy + flameHeight * 0.15f
        )
        // Curva izquierda (simétrica)
        cubicTo(
            cx - flameWidth, cy - flameHeight * 0.2f,
            cx - flameWidth * 0.3f, cy - flameHeight * 0.6f,
            cx, cy - flameHeight
        )
        close()
    }

    rotate(degrees = rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
        drawPath(path, color)
    }
}
