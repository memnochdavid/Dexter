package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.StatSlot
import com.david.pokedex_api.ui.theme.*

// Función para obtener un color basado en el nombre de la estadística (puedes personalizarla)
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
        else -> Color.Black // Color por defecto
    }
}

// Función para formatear el nombre de la estadística
fun formatStatName(statName: String): String {
    return statName.split("-").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.let {
        // Abreviaciones comunes si lo deseas
        when (it.lowercase()) {
            "hp" -> "HP"
            "attack" -> "Ataque"
            "defense" -> "Defensa"
            "special attack" -> "At. Esp."
            "special defense" -> "Def. Esp."
            "speed" -> "Velocidad"
            else -> it
        }
    }
}

@Composable
fun MuestraStatsBase(stats: List<StatSlot>, colorFondo: Color = Color.Black, colorTexto: Color = Color.White) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorFondo)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estadísticas Base",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto,
                modifier = Modifier
                    .padding(vertical = 10.dp, horizontal = 10.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val maxIndividualStatValue = 255f
            // Asumiendo 6 estadísticas principales para el cálculo del total máximo.
            // Si el número de stats puede variar o si `stats` no siempre tiene 6 elementos,
            // este cálculo debería ser más dinámico (ej: maxIndividualStatValue * stats.size)
            // o basarse en un número fijo de estadísticas esperadas para el total.
            val expectedNumberOfStatsForTotal = 6
            val maxTotalStatValue = maxIndividualStatValue * expectedNumberOfStatsForTotal

            val totalBaseStat = remember(stats) { stats.sumOf { it.baseStat } }
            val targetTotalProgress = if (maxTotalStatValue > 0) totalBaseStat / maxTotalStatValue else 0f

            var animationStarted by remember { mutableStateOf(false) }

            // Especificaciones de animación (pueden ser las mismas para todas las animaciones)
            val animationSpecFloat = tween<Float>(durationMillis = 1000, delayMillis = 200)
            val animationSpecInt = tween<Int>(durationMillis = 1000, delayMillis = 200)

            stats.forEach { statSlot ->
                val targetProgress = statSlot.baseStat / maxIndividualStatValue
                val targetNumericStat = statSlot.baseStat

                val animatedProgress by animateFloatAsState(
                    targetValue = if (animationStarted) targetProgress else 0f,
                    animationSpec = animationSpecFloat,
                    label = "StatProgressAnimation_${statSlot.stat.name}"
                )

                val animatedNumericStat by animateIntAsState(
                    targetValue = if (animationStarted) targetNumericStat else 0,
                    animationSpec = animationSpecInt,
                    label = "StatNumericAnimation_${statSlot.stat.name}"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatStatName(statSlot.stat.name),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorTexto,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(2.5f)
                    )

                    Text(
                        text = animatedNumericStat.toString(),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorTexto,
                        modifier = Modifier
                            .weight(1.5f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.End
                    )

                    Box(
                        modifier = Modifier
                            .weight(6.0f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = getStatColor(statSlot.stat.name),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Butt
                        )
                    }
                }
            }

            // --- Fila para el Total ---
            Spacer(modifier = Modifier.height(8.dp)) // Un pequeño espacio antes del total

            val animatedTotalNumericStat by animateIntAsState(
                targetValue = if (animationStarted) totalBaseStat else 0,
                animationSpec = animationSpecInt,
                label = "TotalStatNumericAnimation"
            )

            val animatedTotalProgress by animateFloatAsState(
                targetValue = if (animationStarted) targetTotalProgress else 0f,
                animationSpec = animationSpecFloat,
                label = "TotalStatProgressAnimation"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorTexto,
                    fontWeight = FontWeight.Bold, // Poner en negrita el "Total"
                    modifier = Modifier.weight(2.5f)
                )

                Text(
                    text = animatedTotalNumericStat.toString(),
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto,
                    modifier = Modifier
                        .weight(1.5f)
                        .padding(horizontal = 8.dp),
                    textAlign = TextAlign.End
                )

                Box(
                    modifier = Modifier
                        .weight(6.0f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    LinearProgressIndicator(
                        progress = { animatedTotalProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = getStatColor("total"), // Usa un color específico para el total
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        strokeCap = StrokeCap.Butt
                    )
                }
            }
            // --- Fin Fila para el Total ---

            LaunchedEffect(Unit) { // Mover LaunchedEffect aquí para que se active una sola vez para todas las animaciones
                animationStarted = true
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
