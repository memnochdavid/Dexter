package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.StatSlot
import com.david.pokedex_api.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun getStatColor(statName: String): Color {
    return when (statName.lowercase()) {
        "hp" -> color_stat_hp
        "attack" -> color_stat_attack
        "defense" -> color_stat_defense
        "special-attack" -> color_stat_sp_attack
        "special-defense" -> color_stat_sp_defense
        "speed" -> color_stat_speed
        "total" -> color_stat_total
        else -> Color.Black
    }
}

fun formatStatName(statName: String): String {
    return when (statName.lowercase()) {
        "hp" -> "HP"
        "attack" -> "ATK"
        "defense" -> "DEF"
        "special-attack" -> "SP.ATK"
        "special-defense" -> "SP.DEF"
        "speed" -> "SPD"
        else -> statName.uppercase().take(5)
    }
}

fun formatStatNameLong(statName: String): String {
    return when (statName.lowercase()) {
        "hp" -> "HP"
        "attack" -> "Ataque"
        "defense" -> "Defensa"
        "special-attack" -> "At. Esp."
        "special-defense" -> "Def. Esp."
        "speed" -> "Velocidad"
        else -> statName
    }
}

// Orden del hexágono: HP arriba, luego clockwise
private val STAT_ORDER = listOf("hp", "attack", "defense", "speed", "special-defense", "special-attack")

@Composable
fun MuestraStatsBase(stats: List<StatSlot>, colorFondo: Color = Color.Black, colorTexto: Color = Color.White) {
    val maxStat = 255f
    val totalBaseStat = remember(stats) { stats.sumOf { it.baseStat } }

    // Ordenar stats según el orden del hexágono
    val orderedStats = remember(stats) {
        STAT_ORDER.mapNotNull { name -> stats.find { it.stat.name.lowercase() == name } }
    }

    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationStarted = true }

    val animSpec = tween<Float>(durationMillis = 1000, delayMillis = 150)
    val animSpecInt = tween<Int>(durationMillis = 1000, delayMillis = 150)

    // Valores animados para cada stat
    val animatedValues = orderedStats.map { statSlot ->
        val progress by animateFloatAsState(
            targetValue = if (animationStarted) (statSlot.baseStat / maxStat).coerceIn(0f, 1f) else 0f,
            animationSpec = animSpec,
            label = "radar_${statSlot.stat.name}"
        )
        progress
    }

    val animatedTotal by animateIntAsState(
        targetValue = if (animationStarted) totalBaseStat else 0,
        animationSpec = animSpecInt,
        label = "total_val"
    )

    // Colores para el fill del radar
    val statColors = orderedStats.map { getStatColor(it.stat.name) }
    val avgColor = remember(statColors) {
        if (statColors.isEmpty()) Color.White
        else Color(
            red = statColors.map { it.red }.average().toFloat(),
            green = statColors.map { it.green }.average().toFloat(),
            blue = statColors.map { it.blue }.average().toFloat(),
            alpha = 1f
        )
    }

    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // false = radar grande, true = barras grandes
    var showBars by remember { mutableStateOf(false) }
    // Ya no usamos weights animados — se pliega/despliega con AnimatedVisibility

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorFondo)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle: toca para cambiar entre radar y barras
        Crossfade(
            targetState = showBars,
            animationSpec = tween(400),
            label = "statsToggle",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showBars = !showBars
                }
        ) { isBars ->
            if (!isBars) {
                // ── Vista Radar ──
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Titulo + Total a la izquierda
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Stats",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTexto
                        )
                        Text(
                            text = "Total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorTexto.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "$animatedTotal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorTexto.copy(alpha = 0.7f)
                        )
                    }

                    // Hexagono
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRadarChart(
                                orderedStats, animatedValues, statColors, avgColor,
                                colorTexto, density, size
                            )
                        }
                    }
                }
            } else {
                // ── Vista Barras ──
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Stats", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colorTexto)
                        Text("  \u2022  ", fontSize = 12.sp, color = colorTexto.copy(alpha = 0.3f))
                        Text("Total $animatedTotal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colorTexto.copy(alpha = 0.6f))
                    }

                    orderedStats.forEachIndexed { index, statSlot ->
                        val animVal by animateIntAsState(
                            targetValue = if (animationStarted) statSlot.baseStat else 0,
                            animationSpec = animSpecInt,
                            label = "bar_${statSlot.stat.name}"
                        )
                        val statColor = statColors.getOrElse(index) { Color.Gray }

                        MiniStatBar(
                            label = formatStatNameLong(statSlot.stat.name),
                            value = animVal,
                            progress = animatedValues.getOrElse(index) { 0f },
                            color = statColor,
                            colorTexto = colorTexto
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawRadarChart(
    orderedStats: List<StatSlot>,
    animatedValues: List<Float>,
    statColors: List<Color>,
    avgColor: Color,
    colorTexto: Color,
    density: androidx.compose.ui.unit.Density,
    canvasSize: Size
) {
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f
    val radius = canvasSize.minDimension * 0.30f
    val labelRadius = canvasSize.minDimension * 0.43f
    val numSides = 6
    val angleOffset = -PI / 2.0

    fun hexPoint(index: Int, r: Float): Offset {
        val angle = angleOffset + (2 * PI * index / numSides)
        return Offset(
            x = centerX + (r * cos(angle)).toFloat(),
            y = centerY + (r * sin(angle)).toFloat()
        )
    }

    // Anillos de referencia
    listOf(0.33f, 0.66f, 1f).forEach { level ->
        val ringPath = Path().apply {
            for (i in 0 until numSides) {
                val p = hexPoint(i, radius * level)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(ringPath, colorTexto.copy(alpha = if (level == 1f) 0.15f else 0.07f), style = Stroke(width = 1.dp.toPx()))
    }

    // Lineas centro → vertices
    for (i in 0 until numSides) {
        drawLine(colorTexto.copy(alpha = 0.07f), Offset(centerX, centerY), hexPoint(i, radius), strokeWidth = 1.dp.toPx())
    }

    // Poligono de stats
    if (animatedValues.size == numSides) {
        val statsPath = Path().apply {
            for (i in 0 until numSides) {
                val p = hexPoint(i, radius * animatedValues[i])
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(statsPath, brush = Brush.radialGradient(
            listOf(avgColor.copy(alpha = 0.45f), avgColor.copy(alpha = 0.15f)),
            center = Offset(centerX, centerY), radius = radius
        ))
        drawPath(statsPath, avgColor.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        for (i in 0 until numSides) {
            val p = hexPoint(i, radius * animatedValues[i])
            drawCircle(statColors[i], 4.dp.toPx(), p)
            drawCircle(Color.White, 2.dp.toPx(), p)
        }
    }

    // Labels
    val scaleFactor = (canvasSize.minDimension / with(density) { 300.dp.toPx() }).coerceIn(0.4f, 1f)
    val labelFontSize = with(density) { 11.sp.toPx() } * scaleFactor
    val valueFontSize = with(density) { 13.sp.toPx() } * scaleFactor
    val labelGap = with(density) { 13.dp.toPx() } * scaleFactor

    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true; textSize = labelFontSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val valuePaint = android.graphics.Paint().apply {
        isAntiAlias = true; textSize = valueFontSize
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    for (i in orderedStats.indices) {
        val lp = hexPoint(i, labelRadius)
        textPaint.color = colorTexto.copy(alpha = 0.5f).hashCode()
        drawContext.canvas.nativeCanvas.drawText(
            formatStatName(orderedStats[i].stat.name), lp.x,
            lp.y - with(density) { 2.dp.toPx() } * scaleFactor, textPaint
        )
        valuePaint.color = statColors[i].hashCode()
        drawContext.canvas.nativeCanvas.drawText(
            orderedStats[i].baseStat.toString(), lp.x, lp.y + labelGap, valuePaint
        )
    }
}

@Composable
private fun MiniStatBar(
    label: String,
    value: Int,
    progress: Float,
    color: Color,
    colorTexto: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorTexto.copy(alpha = 0.85f),
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerPx = size.height / 2
                drawRoundRect(
                    color = colorTexto.copy(alpha = 0.06f),
                    size = size,
                    cornerRadius = CornerRadius(cornerPx)
                )
                val barWidth = size.width * progress.coerceIn(0f, 1f)
                if (barWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.6f))
                        ),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(cornerPx)
                    )
                }
            }
        }
    }
}
