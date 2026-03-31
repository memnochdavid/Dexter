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
