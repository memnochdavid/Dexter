package com.david.pokedex_api.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Sparkle(
    val angle: Float,       // direccion de salida (radianes)
    val distance: Float,    // distancia maxima (fraccion del radio)
    val size: Float,        // tamano del destello
    val rotationSpeed: Float, // velocidad de giro
    val delay: Float,       // 0..0.3 retraso de aparicion
    val color: Color
)

private val sparkleColors = listOf(
    Color(0xFFFFD700), // oro
    Color(0xFFFFF8DC), // crema
    Color(0xFFFFE4B5), // moccasin
    Color(0xFFFFFFFF), // blanco
    Color(0xFFFAFAD2), // amarillo claro
)

/**
 * Efecto de estrellas/sparkles que se dispara una vez.
 * Las particulas salen del centro, crecen, rotan y se desvanecen.
 */
@Composable
fun ShinySparkleEffect(
    trigger: Boolean, // cuando cambia a true, dispara el efecto
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    // Generar sparkles distribuidas por todo el componente (minimo 75% del area)
    val sparkles = remember(trigger) {
        if (!trigger) emptyList()
        else {
            val rng = Random(System.nanoTime())
            List(18) {
                Sparkle(
                    angle = rng.nextFloat() * 2f * Math.PI.toFloat(),
                    distance = 0.15f + rng.nextFloat() * 0.85f, // cubren del 15% al 100% del radio
                    size = 0.05f + rng.nextFloat() * 0.07f, // estrellas mas grandes
                    rotationSpeed = 90f + rng.nextFloat() * 180f,
                    delay = rng.nextFloat() * 0.3f,
                    color = sparkleColors[rng.nextInt(sparkleColors.size)]
                )
            }
        }
    }

    LaunchedEffect(trigger) {
        if (trigger) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(700, easing = LinearEasing))
        }
    }

    if (sparkles.isNotEmpty() && progress.value > 0f && progress.value < 1f) {
        Canvas(modifier = modifier) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            // Usar la diagonal para que las estrellas cubran todo el componente
            val maxRadius = kotlin.math.hypot(cx, cy) * 0.85f
            val p = progress.value

            sparkles.forEach { sparkle ->
                // Cada sparkle tiene su propio timing con delay
                val localP = ((p - sparkle.delay) / (1f - sparkle.delay)).coerceIn(0f, 1f)
                if (localP <= 0f) return@forEach

                // Posicion: sale del centro
                val dist = sparkle.distance * maxRadius * localP
                val x = cx + cos(sparkle.angle) * dist
                val y = cy + sin(sparkle.angle) * dist

                // Tamano: crece rapido, luego encoge
                val sizeCurve = if (localP < 0.3f) localP / 0.3f else 1f - (localP - 0.3f) / 0.7f
                val starSize = sparkle.size * maxRadius * sizeCurve.coerceIn(0f, 1f)

                // Alpha: aparece rapido, se desvanece lento
                val alpha = if (localP < 0.2f) localP / 0.2f else 1f - (localP - 0.2f) / 0.8f

                // Rotacion
                val rotation = sparkle.rotationSpeed * localP

                if (starSize > 0.5f && alpha > 0.01f) {
                    drawStar(
                        center = Offset(x, y),
                        outerRadius = starSize,
                        innerRadius = starSize * 0.4f,
                        color = sparkle.color.copy(alpha = alpha.coerceIn(0f, 1f)),
                        rotation = rotation,
                        points = 4
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
    rotation: Float,
    points: Int
) {
    rotate(rotation, pivot = center) {
        val path = Path()
        val totalPoints = points * 2
        for (i in 0 until totalPoints) {
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val angle = (Math.PI * 2 * i / totalPoints - Math.PI / 2).toFloat()
            val x = center.x + cos(angle) * r
            val y = center.y + sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}
