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
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val RockColors = listOf(
    Color(0xFF8D6E63), // marrón
    Color(0xFF6D4C41), // marrón oscuro
    Color(0xFFA1887F), // marrón claro
    Color(0xFF795548), // marrón medio
    Color(0xFF5D4037), // marrón profundo
)

private data class RockFragment(
    var x: Float,
    var y: Float,
    val vy: Float,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val vertices: List<Pair<Float, Float>>, // forma angular irregular
    val colorIndex: Int,
    val alpha: Float,
    val seed: Float
)

@Composable
fun RockOverlay(modifier: Modifier = Modifier) {
    val rocks = remember {
        List(15) {
            val rng = Random(it * 31)
            val vertCount = 5 + rng.nextInt(3)
            val vertices = List(vertCount) { v ->
                val angle = (v.toFloat() / vertCount) * Math.PI.toFloat() * 2f
                val r = 0.6f + rng.nextFloat() * 0.4f // radio irregular
                cos(angle) * r to sin(angle) * r
            }
            RockFragment(
                x = Random.nextFloat(),
                y = -Random.nextFloat() * 0.3f,
                vy = 0.0012f + Random.nextFloat() * 0.002f,
                size = 0.02f + Random.nextFloat() * 0.03f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
                vertices = vertices,
                colorIndex = Random.nextInt(RockColors.size),
                alpha = 0.25f + Random.nextFloat() * 0.35f,
                seed = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "rock")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rockTime"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        rocks.forEach { rock ->
            // Caída con gravedad leve (acelera)
            rock.y += rock.vy + rock.y * 0.0005f

            // Respawn arriba
            if (rock.y > 1.15f) {
                rock.y = -0.1f
                rock.x = Random.nextFloat()
            }

            val currentRotation = rock.rotation + time * 360f * rock.rotationSpeed
            val rockSize = rock.size * h
            val cx = rock.x * w + sin((time + rock.seed) * 6f) * w * 0.01f
            val cy = rock.y * h
            val color = RockColors[rock.colorIndex]

            drawRockFragment(
                cx = cx,
                cy = cy,
                rockSize = rockSize,
                rotation = currentRotation,
                vertices = rock.vertices,
                color = color.copy(alpha = rock.alpha)
            )
        }
    }
}

private fun DrawScope.drawRockFragment(
    cx: Float,
    cy: Float,
    rockSize: Float,
    rotation: Float,
    vertices: List<Pair<Float, Float>>,
    color: Color
) {
    val path = Path().apply {
        vertices.forEachIndexed { i, (vx, vy) ->
            val px = cx + vx * rockSize
            val py = cy + vy * rockSize
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }

    rotate(degrees = rotation, pivot = Offset(cx, cy)) {
        // Sombra
        drawPath(
            path,
            Color.Black.copy(alpha = color.alpha * 0.3f)
        )
        // Roca (ligeramente desplazada para dar volumen)
        val litPath = Path().apply {
            vertices.forEachIndexed { i, (vx, vy) ->
                val px = cx + vx * rockSize - rockSize * 0.05f
                val py = cy + vy * rockSize - rockSize * 0.05f
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(litPath, color)

        // Borde claro (highlight)
        val highlightPath = Path().apply {
            val first = vertices.first()
            val second = vertices.getOrElse(1) { first }
            moveTo(cx + first.first * rockSize - rockSize * 0.05f, cy + first.second * rockSize - rockSize * 0.05f)
            lineTo(cx + second.first * rockSize - rockSize * 0.05f, cy + second.second * rockSize - rockSize * 0.05f)
        }
        drawPath(
            highlightPath,
            Color.White.copy(alpha = color.alpha * 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
        )
    }
}
