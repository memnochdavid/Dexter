package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.core.animateFloatAsState
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
        "hp" -> color_fuego_light
        "attack" -> color_electrico_light
        "defense" -> verde40
        "special-attack" -> color_agua_light
        "special-defense" -> color_bicho_card
        "speed" -> color_psiquico_light
        else -> MaterialTheme.colorScheme.primary // Color por defecto
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
            horizontalArrangement = Arrangement.SpaceEvenly, // Center para centrar el texto si es el único elemento
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estadísticas Base",
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto, // Asegúrate que CardBorder esté definido en tu tema o sea un Color
                modifier = Modifier
                    // .wrapContentHeight() // No es necesario si el Row tiene altura fija o se ajusta al contenido
                    .padding(vertical = 10.dp, horizontal = 10.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // Aumenté un poco el padding horizontal para el contenido
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            stats.forEach { statSlot ->
                val maxStatValue = 255f
                // El progreso objetivo real
                val targetProgress = statSlot.baseStat / maxStatValue

                // Estado para controlar cuándo iniciar la animación (por ejemplo, cuando el composable aparece)
                var animationStarted by remember { mutableStateOf(false) }

                // Anima el valor del progreso
                val animatedProgress by animateFloatAsState(
                    targetValue = if (animationStarted) targetProgress else 0f, // Anima a targetProgress o inicia en 0
                    animationSpec = tween(
                        durationMillis = 1000, // Duración de la animación en milisegundos
                        delayMillis = 200 // Retraso antes de que comience la animación
                    ),
                    label = "StatProgressAnimation" // Etiqueta para herramientas de depuración
                )

                // Inicia la animación cuando el composable entra en la composición
                LaunchedEffect(Unit) {
                    animationStarted = true
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), // Ajustado el padding vertical
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
                        text = statSlot.baseStat.toString(),
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorTexto,
                        modifier = Modifier
                            .weight(1.5f)
                            .padding(horizontal = 8.dp), // Ajustado padding
                        textAlign = TextAlign.End
                    )

                    Box(
                        modifier = Modifier
                            .weight(6.0f)
                            .height(12.dp) // Ligeramente más alta para mejor visualización
                            .clip(RoundedCornerShape(8.dp)) // Bordes un poco más redondeados
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress }, // Usa el valor animado aquí
                            modifier = Modifier.fillMaxSize(),
                            color = getStatColor(statSlot.stat.name),
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Butt
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
