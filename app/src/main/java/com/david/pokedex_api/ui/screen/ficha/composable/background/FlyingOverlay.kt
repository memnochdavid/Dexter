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

private data class Cloud(
    var x: Float,
    val y: Float,
    val speed: Float,
    val alpha: Float,
    val width: Float,
    val height: Float,
    val blobs: List<CloudBlob>
)

private data class CloudBlob(
    val dx: Float,
    val dy: Float,
    val scale: Float
)

@Composable
fun FlyingOverlay(modifier: Modifier = Modifier) {
    val clouds = remember {
        List(8) {
            val blobCount = 5 + Random.nextInt(4)
            Cloud(
                x = Random.nextFloat() * 1.4f - 0.2f,
                y = 0.05f + Random.nextFloat() * 0.8f,
                speed = 0.0005f + Random.nextFloat() * 0.001f,
                alpha = 0.25f + Random.nextFloat() * 0.15f,
                width = 0.14f + Random.nextFloat() * 0.12f,
                height = 0.06f + Random.nextFloat() * 0.04f,
                blobs = List(blobCount) {
                    CloudBlob(
                        dx = (Random.nextFloat() - 0.5f) * 1.6f,
                        dy = (Random.nextFloat() - 0.5f) * 0.8f,
                        scale = 0.5f + Random.nextFloat() * 0.6f
                    )
                }
            )
        }
    }

    // Corrientes de viento
    val windStreaks = remember {
        List(10) {
            Particle(
                x = Random.nextFloat() * 1.4f - 0.2f,
                y = Random.nextFloat(),
                vx = 0.003f + Random.nextFloat() * 0.004f,
                vy = 0f,
                alpha = 0.06f + Random.nextFloat() * 0.08f,
                size = 0.08f + Random.nextFloat() * 0.12f,
                life = Random.nextFloat(),
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "flying")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flyingTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Corrientes de viento (líneas horizontales sutiles)
        windStreaks.forEach { streak ->
            streak.x += streak.vx

            if (streak.x > 1.3f) {
                streak.x = -0.3f
                streak.y = Random.nextFloat()
            }

            val sx = streak.x * w
            val sy = streak.y * h + sin((time + streak.seed) * Math.PI.toFloat() * 2f) * h * 0.01f
            val streakLen = streak.size * w

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = streak.alpha),
                        Color.White.copy(alpha = streak.alpha),
                        Color.Transparent
                    ),
                    startX = sx,
                    endX = sx + streakLen
                ),
                start = Offset(sx, sy),
                end = Offset(sx + streakLen, sy),
                strokeWidth = 1.5f
            )
        }

        // Nubes
        clouds.forEach { cloud ->
            cloud.x += cloud.speed

            if (cloud.x > 1.3f) {
                cloud.x = -0.3f
            }

            val cx = cloud.x * w
            val cy = cloud.y * h + sin((time + cloud.y) * Math.PI.toFloat() * 2f) * h * 0.008f
            val cloudW = cloud.width * w
            val cloudH = cloud.height * h

            cloud.blobs.forEach { blob ->
                val bx = cx + blob.dx * cloudW
                val by = cy + blob.dy * cloudH
                val rx = cloudW * blob.scale * 0.5f
                val ry = cloudH * blob.scale * 0.5f
                val r = (rx + ry) * 0.5f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = cloud.alpha),
                            Color.White.copy(alpha = cloud.alpha * 0.85f),
                            Color.White.copy(alpha = cloud.alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(bx, by),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(bx, by)
                )
            }
        }
    }
}
