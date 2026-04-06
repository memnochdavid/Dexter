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

private val DustCloudColors = listOf(
    Color(0xFFD2B48C),
    Color(0xFFC4A265),
    Color(0xFFB8956A),
    Color(0xFFA0845C),
)

private data class DustCloud(
    var x: Float,
    val y: Float,
    val speed: Float,
    val baseAlpha: Float,
    val size: Float,
    val seed: Float,
    // Cada nube es un grupo de círculos solapados
    val blobs: List<BlobOffset>
)

private data class BlobOffset(
    val dx: Float, // offset respecto al centro de la nube (normalizado al size)
    val dy: Float,
    val scale: Float, // tamaño relativo al de la nube
    val colorIndex: Int
)

@Composable
fun GroundOverlay(modifier: Modifier = Modifier) {
    val clouds = remember {
        List(8) {
            val blobCount = 4 + Random.nextInt(4)
            DustCloud(
                x = Random.nextFloat() * 1.4f - 0.2f,
                y = 0.35f + Random.nextFloat() * 0.55f,
                speed = 0.0008f + Random.nextFloat() * 0.0015f,
                baseAlpha = 0.3f + Random.nextFloat() * 0.2f,
                size = 0.1f + Random.nextFloat() * 0.12f,
                seed = Random.nextFloat(),
                blobs = List(blobCount) {
                    BlobOffset(
                        dx = (Random.nextFloat() - 0.5f) * 1.6f,
                        dy = (Random.nextFloat() - 0.5f) * 0.8f,
                        scale = 0.5f + Random.nextFloat() * 0.6f,
                        colorIndex = Random.nextInt(DustCloudColors.size)
                    )
                }
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ground")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "groundTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        clouds.forEach { cloud ->
            cloud.x += cloud.speed

            // Respawn al salir por la derecha
            if (cloud.x > 1.3f) {
                cloud.x = -0.3f
            }

            val cx = cloud.x * w
            val cy = cloud.y * h + sin((time + cloud.seed) * Math.PI.toFloat() * 2f) * h * 0.015f
            val cloudRadius = cloud.size * h

            // Pulso sutil de opacidad
            val pulse = sin((time * 1.5f + cloud.seed) * Math.PI.toFloat() * 2f) * 0.3f + 0.7f
            val alpha = cloud.baseAlpha * pulse

            // Dibujar cada blob de la nube
            cloud.blobs.forEach { blob ->
                val bx = cx + blob.dx * cloudRadius
                val by = cy + blob.dy * cloudRadius
                val br = cloudRadius * blob.scale
                val color = DustCloudColors[blob.colorIndex]

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            color.copy(alpha = alpha * 0.85f),
                            color.copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(bx, by),
                        radius = br
                    ),
                    radius = br,
                    center = Offset(bx, by)
                )
            }
        }
    }
}
