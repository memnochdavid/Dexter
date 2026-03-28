package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.TypeResponseSlot
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorTypeChip
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.screen.comun.isDarkColor
import com.david.pokedex_api.ui.screen.comun.pokemonTypeNameTranslator
import com.david.pokedex_api.util.Lottie
import com.david.pokedex_api.util.TypeInteraction
import com.david.pokedex_api.util.getCombinedDefensiveInteractions

// Colores semánticos para cada categoría
private val COLOR_WEAK_4 = Color(0xFFD32F2F)
private val COLOR_WEAK_2 = Color(0xFFE57373)
private val COLOR_NEUTRAL = Color(0xFF9E9E9E)
private val COLOR_RESIST_2 = Color(0xFF66BB6A)
private val COLOR_RESIST_4 = Color(0xFF2E7D32)
private val COLOR_IMMUNE = Color(0xFF42A5F5)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PokemonTypeInteractionsTable(
    pokemonTypes: List<TypeResponseSlot>,
    pokemonApiService: PokeApiService,
    tableBackgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    var calculatedInteractions by remember { mutableStateOf<List<TypeInteraction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasAttemptedLoad by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = pokemonTypes) {
        if (pokemonTypes.isNotEmpty()) {
            isLoading = true
            hasAttemptedLoad = true
            calculatedInteractions = getCombinedDefensiveInteractions(pokemonTypes, pokemonApiService)
            isLoading = false
        } else {
            calculatedInteractions = emptyList()
            hasAttemptedLoad = true
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
        }
    } else if (calculatedInteractions.isNotEmpty()) {
        val interactionsByMultiplier = calculatedInteractions.groupBy { it.multiplier }
            .toSortedMap(compareByDescending { it })

        // Separar en debilidades, resistencias e inmunidades
        val weaknesses = interactionsByMultiplier.filter { it.key > 1.0 }
        val resistances = interactionsByMultiplier.filter { it.key in 0.01..0.99 }
        val immunities = interactionsByMultiplier.filter { it.key == 0.0 }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(tableBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Resistencias y debilidades",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            // Debilidades
            if (weaknesses.isNotEmpty()) {
                CategorySection(
                    title = "Debilidades",
                    emoji = "\u26A0",
                    categoryColor = COLOR_WEAK_2,
                    interactions = weaknesses,
                    textColor = textColor
                )
                Spacer(Modifier.height(14.dp))
            }

            // Resistencias
            if (resistances.isNotEmpty()) {
                CategorySection(
                    title = "Resistencias",
                    emoji = "\u26E8",
                    categoryColor = COLOR_RESIST_2,
                    interactions = resistances,
                    textColor = textColor
                )
                Spacer(Modifier.height(14.dp))
            }

            // Inmunidades
            if (immunities.isNotEmpty()) {
                CategorySection(
                    title = "Inmunidades",
                    emoji = "\u2716",
                    categoryColor = COLOR_IMMUNE,
                    interactions = immunities,
                    textColor = textColor
                )
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(40.dp))
        }
    } else if (hasAttemptedLoad && pokemonTypes.isNotEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No se encontraron datos de interacciones.",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else if (pokemonTypes.isEmpty() && hasAttemptedLoad) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Sin tipos para calcular interacciones.",
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    title: String,
    emoji: String,
    categoryColor: Color,
    interactions: Map<Double, List<TypeInteraction>>,
    textColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header de categoría
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.5.sp
            )
        }

        interactions.forEach { (multiplier, types) ->
            if (types.isNotEmpty()) {
                // Badge del multiplicador
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp, start = 14.dp)
                ) {
                    MultiplierBadge(multiplier = multiplier, categoryColor = categoryColor)
                }

                // Chips de tipo en FlowRow
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { interaction ->
                        TypeChipWithIcon(typeName = interaction.attackingType)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiplierBadge(multiplier: Double, categoryColor: Color) {
    val label = when (multiplier) {
        0.0 -> "x0"
        0.25 -> "x\u00BC"
        0.5 -> "x\u00BD"
        2.0 -> "x2"
        4.0 -> "x4"
        else -> "x$multiplier"
    }

    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = categoryColor,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun TypeChipWithIcon(typeName: String) {
    val typeColor = getPokemonTypeColorTypeChip(typeName)
    val contentColor = if (isDarkColor(typeColor)) Color.White else Color(0xFF1A1A1A)
    val iconRes = getPokemonTypeToIcon(typeName)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(typeColor)
            .padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
    ) {
        // Icono del tipo
        if (iconRes != 0) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = typeName,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(2.dp)
            )
            Spacer(Modifier.width(5.dp))
        }

        Text(
            text = pokemonTypeNameTranslator(typeName).replaceFirstChar { it.titlecase() },
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
