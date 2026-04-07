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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val SnowflakeColors = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFE0F0FF),
    Color(0xFFB0D4F1),
    Color(0xFFC8E6FF),
)

@Composable
fun IceOverlay(modifier: Modifier = Modifier) {
    val particles = remember {
        List(25) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 1.2f - 0.2f,
                vx = Random.nextFloat() * 0.001f - 0.0005f,
                vy = 0.001f + Random.nextFloat() * 0.002f,
                alpha = 0.3f + Random.nextFloat() * 0.5f,
                size = 0.02f + Random.nextFloat() * 0.035f,
                life = Random.nextFloat(),
                rotation = Random.nextFloat() * 360f,
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ice")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iceTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            p.y += p.vy
            p.x += p.vx + sin((time + p.seed) * 10f) * 0.0006f
            p.rotation += 0.3f + p.seed * 0.5f

            // Respawn al salir por abajo
            if (p.y > 1.1f) {
                p.y = -0.05f
                p.x = Random.nextFloat()
                p.alpha = 0.3f + p.seed * 0.5f
            }

            val colorIndex = (p.seed * SnowflakeColors.size).toInt().coerceIn(0, SnowflakeColors.lastIndex)
            val color = SnowflakeColors[colorIndex].copy(alpha = p.alpha)
            val crystalSize = p.size * h

            drawSnowflake(
                cx = p.x * w,
                cy = p.y * h,
                radius = crystalSize,
                rotation = p.rotation,
                color = color
            )
        }
    }
}

private fun DrawScope.drawSnowflake(
    cx: Float,
    cy: Float,
    radius: Float,
    rotation: Float,
    color: Color
) {
    rotate(degrees = rotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
        // 6 brazos del cristal
        for (i in 0 until 6) {
            val angle = Math.toRadians(i * 60.0).toFloat()
            val endX = cx + cos(angle) * radius
            val endY = cy + sin(angle) * radius

            // Brazo principal
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(endX, endY)
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f))

            // Ramitas laterales a 2/3 del brazo
            val branchStart = 0.6f
            val branchLen = radius * 0.35f
            val bx = cx + cos(angle) * radius * branchStart
            val by = cy + sin(angle) * radius * branchStart

            for (side in listOf(-1f, 1f)) {
                val branchAngle = angle + side * Math.toRadians(45.0).toFloat()
                val bex = bx + cos(branchAngle) * branchLen
                val bey = by + sin(branchAngle) * branchLen
                val branchPath = Path().apply {
                    moveTo(bx, by)
                    lineTo(bex, bey)
                }
                drawPath(branchPath, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
            }
        }
    }
}
