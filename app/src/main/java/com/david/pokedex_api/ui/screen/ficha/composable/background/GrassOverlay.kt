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
import kotlin.random.Random

private val LeafColors = listOf(
    Color(0xFF4CAF50), // verde
    Color(0xFF66BB6A), // verde claro
    Color(0xFF2E7D32), // verde oscuro
    Color(0xFF81C784), // verde pastel
    Color(0xFFA5D6A7), // verde menta
)

@Composable
fun GrassOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(20) {
            Particle(
                x = Random.nextFloat() * 1.2f - 0.1f,
                y = Random.nextFloat() * 1.2f - 0.2f,
                vx = 0.0008f + Random.nextFloat() * 0.001f,
                vy = 0.001f + Random.nextFloat() * 0.0015f,
                alpha = 0.3f + Random.nextFloat() * 0.45f,
                size = 0.04f + Random.nextFloat() * 0.04f,
                life = Random.nextFloat(),
                rotation = Random.nextFloat() * 360f,
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "grass")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grassTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            // Caída diagonal con balanceo pendular
            p.y += p.vy
            p.x += p.vx + sin((time + p.seed) * 8f) * 0.001f
            p.rotation += sin((time + p.seed) * 12f) * 1.5f

            // Respawn al salir por abajo o por la derecha
            if (p.y > 1.1f || p.x > 1.15f) {
                p.y = -0.05f
                p.x = Random.nextFloat() * 0.8f - 0.1f
                p.alpha = 0.3f + p.seed * 0.45f
            }

            val colorIndex = (p.seed * LeafColors.size).toInt().coerceIn(0, LeafColors.lastIndex)
            val color = LeafColors[colorIndex].copy(alpha = p.alpha)
            val leafSize = p.size * h

            drawLeaf(
                cx = p.x * w,
                cy = p.y * h,
                leafLength = leafSize,
                leafWidth = leafSize * 0.45f,
                rotation = p.rotation,
                color = color
            )
        }
    }
}

private fun DrawScope.drawLeaf(
    cx: Float,
    cy: Float,
    leafLength: Float,
    leafWidth: Float,
    rotation: Float,
    color: Color
) {
    val path = Path().apply {
        // Punta superior
        moveTo(cx, cy - leafLength * 0.5f)
        // Curva derecha (forma de hoja ovalada con punta)
        cubicTo(
            cx + leafWidth * 0.6f, cy - leafLength * 0.25f,
            cx + leafWidth, cy + leafLength * 0.1f,
            cx, cy + leafLength * 0.5f
        )
        // Curva izquierda (simétrica)
        cubicTo(
            cx - leafWidth, cy + leafLength * 0.1f,
            cx - leafWidth * 0.6f, cy - leafLength * 0.25f,
            cx, cy - leafLength * 0.5f
        )
        close()
    }

    rotate(degrees = rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
        drawPath(path, color)

        // Nervio central
        val nervePath = Path().apply {
            moveTo(cx, cy - leafLength * 0.4f)
            lineTo(cx, cy + leafLength * 0.4f)
        }
        drawPath(
            nervePath,
            color.copy(alpha = color.alpha * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }
}
