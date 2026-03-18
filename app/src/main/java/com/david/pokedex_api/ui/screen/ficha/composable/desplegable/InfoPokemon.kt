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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse

@Composable
fun InfoPokemon(
    pokemon: PokemonDetailResponse,
    species: PokemonSpeciesResponse?,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorFondo)
    ) {
        // Titulo
        Text(
            text = "Info",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = colorTexto,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Experiencia base
            item {
                InfoRow("Exp. Base", pokemon.baseExperience?.toString() ?: "-", colorTexto)
            }

            // Ratio de género
            item {
                val genderRate = species?.genderRate
                if (genderRate != null && genderRate >= 0) {
                    val femalePercent = (genderRate / 8f) * 100f
                    val malePercent = 100f - femalePercent
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InfoRow("Género", "", colorTexto)
                        Spacer(Modifier.height(4.dp))
                        GenderBar(malePercent, femalePercent, colorTexto)
                    }
                } else if (genderRate == -1) {
                    InfoRow("Género", "Sin género", colorTexto)
                }
            }

            // Tasa de captura
            item {
                val captureRate = species?.captureRate
                if (captureRate != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InfoRow("Captura", "$captureRate / 255", colorTexto)
                        Spacer(Modifier.height(4.dp))
                        CaptureBar(captureRate / 255f, colorTexto)
                    }
                }
            }

            // Felicidad base
            item {
                InfoRow("Felicidad Base", species?.baseHappiness?.toString() ?: "-", colorTexto)
            }

            // Grupos huevo
            item {
                val eggGroups = species?.eggGroups
                    ?.map { translateEggGroup(it.name) }
                    ?.joinToString(", ")
                    ?: "-"
                InfoRow("Grupo Huevo", eggGroups, colorTexto)
            }

            // Pasos para eclosionar
            item {
                val steps = species?.hatchCounter?.let { (it + 1) * 255 }
                InfoRow("Pasos Eclosión", steps?.let { "~$it" } ?: "-", colorTexto)
            }

            // Ritmo de crecimiento
            item {
                InfoRow("Crecimiento", translateGrowthRate(species?.growthRate?.name ?: "-"), colorTexto)
            }

            // Hábitat
            item {
                InfoRow("Hábitat", translateHabitat(species?.habitat?.name ?: "-"), colorTexto)
            }

            // Legendario / Mítico
            item {
                val status = when {
                    species?.isMythical == true -> "Mítico"
                    species?.isLegendary == true -> "Legendario"
                    else -> null
                }
                if (status != null) {
                    InfoRow("Categoría", status, colorTexto)
                }
            }

            // EVs que otorga
            item {
                val evs = pokemon.stats
                    .filter { it.effort > 0 }
                    .joinToString(", ") { "${it.effort} ${formatStatName(it.stat.name)}" }
                if (evs.isNotEmpty()) {
                    InfoRow("EVs", evs, colorTexto)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, colorTexto: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorTexto.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorTexto,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun GenderBar(malePercent: Float, femalePercent: Float, colorTexto: Color) {
    var animStarted by remember { mutableStateOf(false) }
    val animatedMale by animateFloatAsState(
        targetValue = if (animStarted) malePercent / 100f else 0f,
        animationSpec = tween(800), label = "male"
    )
    val animatedFemale by animateFloatAsState(
        targetValue = if (animStarted) femalePercent / 100f else 0f,
        animationSpec = tween(800), label = "female"
    )
    LaunchedEffect(Unit) { animStarted = true }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFFF6B81).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedMale)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF5B9BF5))
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("♂ ${"%.1f".format(malePercent)}%", fontSize = 11.sp, color = Color(0xFF5B9BF5), fontWeight = FontWeight.SemiBold)
            Text("♀ ${"%.1f".format(femalePercent)}%", fontSize = 11.sp, color = Color(0xFFFF6B81), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CaptureBar(ratio: Float, colorTexto: Color) {
    var animStarted by remember { mutableStateOf(false) }
    val animatedRatio by animateFloatAsState(
        targetValue = if (animStarted) ratio else 0f,
        animationSpec = tween(800), label = "capture"
    )
    LaunchedEffect(Unit) { animStarted = true }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(colorTexto.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedRatio)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    when {
                        ratio > 0.6f -> Color(0xFF4CAF50)
                        ratio > 0.3f -> Color(0xFFF6BD00)
                        else -> Color(0xFFE53935)
                    }
                )
        )
    }
}

private fun translateEggGroup(name: String): String = when (name.lowercase()) {
    "monster" -> "Monstruo"
    "water1" -> "Agua 1"
    "water2" -> "Agua 2"
    "water3" -> "Agua 3"
    "bug" -> "Bicho"
    "flying" -> "Volador"
    "ground" -> "Campo"
    "fairy" -> "Hada"
    "plant" -> "Planta"
    "humanshape", "human-like" -> "Humanoide"
    "mineral" -> "Mineral"
    "amorphous", "indeterminate" -> "Amorfo"
    "ditto" -> "Ditto"
    "dragon" -> "Dragón"
    "no-eggs" -> "Sin huevos"
    else -> name.replaceFirstChar { it.titlecase() }
}

private fun translateGrowthRate(name: String): String = when (name.lowercase()) {
    "slow" -> "Lento"
    "medium" -> "Medio"
    "fast" -> "Rápido"
    "medium-slow" -> "Medio-Lento"
    "slow-then-very-fast" -> "Errático"
    "fast-then-very-slow" -> "Fluctuante"
    "-" -> "-"
    else -> name.replaceFirstChar { it.titlecase() }
}

private fun translateHabitat(name: String): String = when (name.lowercase()) {
    "cave" -> "Cueva"
    "forest" -> "Bosque"
    "grassland" -> "Pradera"
    "mountain" -> "Montaña"
    "rare" -> "Raro"
    "rough-terrain" -> "Terreno Escarpado"
    "sea" -> "Mar"
    "urban" -> "Urbano"
    "waters-edge" -> "Orilla del Agua"
    "-" -> "-"
    else -> name.replaceFirstChar { it.titlecase() }
}
