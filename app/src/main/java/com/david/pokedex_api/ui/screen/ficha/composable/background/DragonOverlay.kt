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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val AuraColors = listOf(
    Color(0xFF6A1B9A), // violeta profundo
    Color(0xFF4A148C), // índigo
    Color(0xFF311B92), // azul muy oscuro
    Color(0xFF7C4DFF), // violeta brillante
    Color(0xFF3D5AFE), // azul eléctrico
)

@Composable
fun DragonOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(30) {
            Particle(
                x = Random.nextFloat(),
                y = 0.7f + Random.nextFloat() * 0.4f,
                vx = (Random.nextFloat() - 0.5f) * 0.001f,
                vy = -(0.004f + Random.nextFloat() * 0.006f),
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                size = 0.01f + Random.nextFloat() * 0.02f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "dragon")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dragonTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Aura central pulsante
        val auraAlpha = (sin(time * Math.PI.toFloat() * 2f) * 0.5f + 0.5f) * 0.08f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF7C4DFF).copy(alpha = auraAlpha),
                    Color(0xFF4A148C).copy(alpha = auraAlpha * 0.4f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = h * 0.6f
            ),
            radius = h * 0.6f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // Partículas de energía ascendente con trail
        particles.forEach { p ->
            p.life -= 0.006f
            p.y += p.vy
            p.x += p.vx + sin((time + p.seed) * 18f) * 0.001f

            if (p.life <= 0f || p.y < -0.1f) {
                p.y = 0.85f + p.seed * 0.2f
                p.x = 0.1f + p.seed * 0.8f
                p.life = 0.6f + p.seed * 0.4f
                p.alpha = 0.3f + p.seed * 0.5f
            }

            val fadeAlpha = (p.life.coerceIn(0f, 0.3f) / 0.3f) * p.alpha
            val colorIndex = (p.seed * AuraColors.size).toInt().coerceIn(0, AuraColors.lastIndex)
            val color = AuraColors[colorIndex]
            val particleSize = p.size * h

            val cx = p.x * w
            val cy = p.y * h

            // Trail (estela vertical)
            val trailLength = particleSize * 4f * p.life
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = fadeAlpha * 0.5f),
                        Color.Transparent
                    ),
                    startY = cy,
                    endY = cy + trailLength
                ),
                start = Offset(cx, cy),
                end = Offset(cx, cy + trailLength),
                strokeWidth = particleSize * 1.5f
            )

            // Partícula (rombo)
            drawDiamond(
                cx = cx,
                cy = cy,
                size = particleSize,
                color = color.copy(alpha = fadeAlpha)
            )

            // Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = fadeAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = particleSize * 3f
                ),
                radius = particleSize * 3f,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawDiamond(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size * 0.6f, cy)
        lineTo(cx, cy + size)
        lineTo(cx - size * 0.6f, cy)
        close()
    }
    drawPath(path, color)
}
