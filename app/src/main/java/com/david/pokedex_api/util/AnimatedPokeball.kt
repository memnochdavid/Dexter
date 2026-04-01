package com.david.pokedex_api.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate

// === Colores exteriores ===
private val RedBright = Color(0xFFEE1515)
private val RedDark = Color(0xFFAA0E0E)
private val RedDeep = Color(0xFF7A0808)

// Mitad inferior: gris metálico/plateado
private val MetalBright = Color(0xFFD8D8D8)
private val MetalMid = Color(0xFFB0B0B0)
private val MetalDark = Color(0xFF808080)
private val MetalDeep = Color(0xFF606060)

// Banda y bordes
private val BandDark = Color(0xFF2A2A2A)
private val BandLight = Color(0xFF6A6A6A)
private val BandHighlight = Color(0xFF8A8A8A)

// Botón
private val ButtonRim = Color(0xFF4A4A4A)
private val ButtonFace = Color(0xFFE8E8E8)
private val ButtonShine = Color(0xFFFFFFFF)
private val ButtonShadow = Color(0xFFAAAAAA)

// Sombra
private val ShadowColor = Color(0x44000000)

/**
 * Pokeball 3D con reflejos especulares y volumen.
 * Apertura vertical: las dos mitades se separan arriba/abajo.
 */
@Composable
fun AnimatedPokeball(
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val openProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(200),
        label = "pokeballOpen"
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val r = diameter / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val border = diameter * 0.035f
        val band = diameter * 0.07f
        val gap = diameter * 0.18f * openProgress

        // --- Sombra proyectada ---
        val shadowOffsetY = r * 0.85f + gap * 0.3f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(ShadowColor, Color.Transparent),
                center = Offset(cx, cy + shadowOffsetY),
                radius = r * 0.7f
            ),
            topLeft = Offset(cx - r * 0.65f, cy + shadowOffsetY - r * 0.12f),
            size = Size(r * 1.3f, r * 0.24f)
        )

        // --- Brillo interior (visible al abrirse) ---
        if (openProgress > 0.05f) {
            val glowAlpha = openProgress
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f * glowAlpha),
                        Color(0xFFFFEB3B).copy(alpha = 0.5f * glowAlpha),
                        Color(0xFFFF6B35).copy(alpha = 0.15f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 0.55f
                ),
                center = Offset(cx, cy),
                radius = r * 0.55f
            )
        }

        // --- Mitad inferior (metálica): se desplaza abajo ---
        val bottomShift = gap * 0.3f
        translate(top = bottomShift) {
            drawBottomHalf(cx, cy, r, diameter, border)
            drawMetallicBand(cx, cy, r, band)
        }

        // --- Mitad superior (roja): se desplaza arriba ---
        val topShift = -gap * 0.7f
        translate(top = topShift) {
            drawTopHalf(cx, cy, r, diameter, border)
            drawMetallicBand(cx, cy, r, band)
        }

        // --- Botón central 3D (siempre centrado) ---
        drawButton3D(cx, cy, r, border)
    }
}

// ========== MITAD INFERIOR (METÁLICA) ==========

private fun DrawScope.drawBottomHalf(
    cx: Float, cy: Float, r: Float, diameter: Float, border: Float
) {
    val clipBottom = Path().apply {
        addOval(Rect(cx - r, cy - r, cx + r, cy + r))
    }

    clipPath(clipBottom) {
        // Base metálica plateada
        drawArc(
            color = MetalBright,
            startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
        )

        // Gradiente de volumen esférico metálico
        drawArc(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.3f to Color.Transparent,
                0.65f to MetalMid.copy(alpha = 0.5f),
                0.85f to MetalDark.copy(alpha = 0.7f),
                1f to MetalDeep.copy(alpha = 0.85f),
                center = Offset(cx + r * 0.15f, cy + r * 0.1f),
                radius = r * 1.05f
            ),
            startAngle = 0f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
        )

        // Reflejo especular metálico (derecha)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(cx + r * 0.30f, cy + r * 0.30f),
                radius = r * 0.45f
            ),
            center = Offset(cx + r * 0.30f, cy + r * 0.30f),
            radius = r * 0.45f
        )
    }

    // Borde exterior inferior
    drawArc(
        color = BandDark,
        startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter),
        style = Stroke(width = border)
    )
}

// ========== MITAD SUPERIOR (ROJA) ==========

private fun DrawScope.drawTopHalf(
    cx: Float, cy: Float, r: Float, diameter: Float, border: Float
) {
    val clipTop = Path().apply {
        addOval(Rect(cx - r, cy - r, cx + r, cy + r))
    }

    clipPath(clipTop) {
        // Base roja
        drawArc(
            color = RedBright,
            startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
        )

        // Gradiente de volumen esférico
        drawArc(
            brush = Brush.radialGradient(
                0f to Color.Transparent,
                0.35f to Color.Transparent,
                0.7f to RedDark.copy(alpha = 0.6f),
                1f to RedDeep.copy(alpha = 0.85f),
                center = Offset(cx + r * 0.1f, cy - r * 0.2f),
                radius = r * 1.1f
            ),
            startAngle = 180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
        )

        // Reflejo especular principal (brillo blanco, orientado a la derecha)
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.85f),
                0.3f to Color.White.copy(alpha = 0.35f),
                0.6f to Color.White.copy(alpha = 0.05f),
                1f to Color.Transparent,
                center = Offset(cx + r * 0.30f, cy - r * 0.45f),
                radius = r * 0.38f
            ),
            center = Offset(cx + r * 0.30f, cy - r * 0.45f),
            radius = r * 0.38f
        )

        // Segundo reflejo más sutil
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                center = Offset(cx + r * 0.55f, cy - r * 0.60f),
                radius = r * 0.18f
            ),
            center = Offset(cx + r * 0.55f, cy - r * 0.60f),
            radius = r * 0.18f
        )
    }

    // Borde exterior superior
    drawArc(
        color = BandDark,
        startAngle = 180f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter),
        style = Stroke(width = border)
    )
}

// ========== BANDA METÁLICA ==========

private fun DrawScope.drawMetallicBand(cx: Float, cy: Float, r: Float, band: Float) {
    val bandY = cy
    drawLine(
        brush = Brush.verticalGradient(
            colors = listOf(BandHighlight, BandLight, BandDark, BandLight.copy(alpha = 0.5f)),
            startY = bandY - band / 2f,
            endY = bandY + band / 2f
        ),
        start = Offset(cx - r, bandY),
        end = Offset(cx + r, bandY),
        strokeWidth = band
    )
}

// ========== BOTÓN CENTRAL 3D ==========

private fun DrawScope.drawButton3D(cx: Float, cy: Float, r: Float, border: Float) {
    val buttonR = r * 0.20f
    val innerR = r * 0.13f

    // Sombra del botón
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = buttonR * 1.1f,
        center = Offset(cx, cy + r * 0.015f)
    )

    // Aro exterior del botón (metálico)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(BandLight, ButtonRim, BandDark),
            center = Offset(cx + buttonR * 0.15f, cy - buttonR * 0.15f),
            radius = buttonR * 1.2f
        ),
        radius = buttonR,
        center = Offset(cx, cy)
    )

    // Cara del botón (blanco con volumen)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(ButtonShine, ButtonFace, ButtonShadow),
            center = Offset(cx + innerR * 0.25f, cy - innerR * 0.3f),
            radius = innerR * 1.3f
        ),
        radius = innerR,
        center = Offset(cx, cy)
    )

    // Reflejo especular en el botón
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color.White.copy(alpha = 0.0f)
            ),
            center = Offset(cx + innerR * 0.2f, cy - innerR * 0.25f),
            radius = innerR * 0.45f
        ),
        center = Offset(cx + innerR * 0.2f, cy - innerR * 0.25f),
        radius = innerR * 0.45f
    )

    // Borde fino del botón
    drawCircle(
        color = BandDark.copy(alpha = 0.6f),
        radius = innerR,
        center = Offset(cx, cy),
        style = Stroke(width = border * 0.4f)
    )
}
/*

package com.david.pokedex_api.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Pokeball dibujada con Canvas que se abre/cierra animadamente.
 * - isOpen = true: la mitad superior se desplaza hacia arriba, se ve brillo interior
 * - isOpen = false: pokeball cerrada normal
 */
@Composable
fun AnimatedPokeball(
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val openProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(200),
        label = "pokeballOpen"
    )

    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val r = diameter / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val border = diameter * 0.045f
        val band = diameter * 0.065f
        val gap = diameter * 0.18f * openProgress // separacion entre mitades

        // --- Mitad inferior (blanca): se desplaza levemente abajo ---
        val bottomShift = gap * 0.3f
        translate(top = bottomShift) {
            drawArc(
                color = Color.White,
                startAngle = 0f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
            )
            drawArc(
                color = Color(0xFF2B2B2B),
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter),
                style = Stroke(width = border)
            )
            drawLine(
                color = Color(0xFF2B2B2B),
                start = Offset(cx - r, cy), end = Offset(cx + r, cy),
                strokeWidth = band
            )
        }

        // --- Brillo interior (visible al abrirse) ---
        if (openProgress > 0.05f) {
            val glowAlpha = openProgress
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f * glowAlpha),
                        Color(0xFFFFEB3B).copy(alpha = 0.5f * glowAlpha),
                        Color(0xFFFF6B35).copy(alpha = 0.15f * glowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = r * 0.55f
                ),
                center = Offset(cx, cy),
                radius = r * 0.55f
            )
        }

        // --- Mitad superior (roja): se desplaza hacia arriba ---
        val topShift = -gap * 0.7f
        translate(top = topShift) {
            drawArc(
                color = Color(0xFFEE1515),
                startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter)
            )
            drawArc(
                color = Color(0xFF2B2B2B),
                startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(cx - r, cy - r), size = Size(diameter, diameter),
                style = Stroke(width = border)
            )
            drawLine(
                color = Color(0xFF2B2B2B),
                start = Offset(cx - r, cy), end = Offset(cx + r, cy),
                strokeWidth = band
            )
        }

        // --- Boton central (siempre centrado, encima de todo) ---
        val buttonR = r * 0.19f
        val innerR = r * 0.12f
        drawCircle(color = Color(0xFF2B2B2B), radius = buttonR, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = innerR, center = Offset(cx, cy))
        drawCircle(
            color = Color(0xFF2B2B2B), radius = innerR, center = Offset(cx, cy),
            style = Stroke(width = border * 0.6f)
        )
    }
}


* */