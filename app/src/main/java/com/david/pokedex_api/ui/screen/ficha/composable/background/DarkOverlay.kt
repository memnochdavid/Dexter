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
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DarkOverlay(modifier: Modifier = Modifier) {
    val tendrils = remember {
        List(8) {
            Particle(
                x = if (Random.nextBoolean()) 0f else 1f,
                y = Random.nextFloat(),
                vx = 0f, vy = 0f,
                alpha = 0.2f + Random.nextFloat() * 0.15f,
                size = 0.3f + Random.nextFloat() * 0.25f,
                life = 0f,
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "dark")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "darkTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Oscuridad que pulsa desde los bordes
        val vignetteAlpha = 0.15f + sin(time * Math.PI.toFloat() * 2f) * 0.05f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = vignetteAlpha * 0.3f),
                    Color.Black.copy(alpha = vignetteAlpha)
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.7f
            ),
            size = size
        )

        // Zarcillos de oscuridad reptando desde los bordes
        tendrils.forEach { t ->
            val fromLeft = t.x < 0.5f
            val baseX = if (fromLeft) 0f else w
            val reach = t.size * w * (0.7f + sin((time * 1.2f + t.seed) * Math.PI.toFloat() * 2f) * 0.3f)
            val cy = t.y * h + sin((time + t.seed * 3f) * Math.PI.toFloat() * 2f) * h * 0.05f
            val tipX = if (fromLeft) reach else w - reach

            val thickness = h * 0.07f * t.alpha
            val path = Path().apply {
                moveTo(baseX, cy - thickness)
                val mid1x = if (fromLeft) reach * 0.3f else w - reach * 0.3f
                val mid2x = if (fromLeft) reach * 0.65f else w - reach * 0.65f
                val wave1 = sin((time + t.seed) * Math.PI.toFloat() * 4f) * h * 0.03f
                val wave2 = sin((time + t.seed + 0.5f) * Math.PI.toFloat() * 3f) * h * 0.02f

                cubicTo(mid1x, cy - thickness + wave1, mid2x, cy + wave2, tipX, cy)
                cubicTo(mid2x, cy + wave2, mid1x, cy + thickness + wave1, baseX, cy + thickness)
                close()
            }

            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    colors = if (fromLeft) listOf(
                        Color.Black.copy(alpha = t.alpha),
                        Color(0xFF0D001A).copy(alpha = t.alpha * 0.6f),
                        Color.Transparent
                    ) else listOf(
                        Color.Transparent,
                        Color(0xFF0D001A).copy(alpha = t.alpha * 0.6f),
                        Color.Black.copy(alpha = t.alpha)
                    )
                )
            )
        }
    }
}
