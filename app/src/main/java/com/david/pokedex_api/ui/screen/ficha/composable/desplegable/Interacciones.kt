package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.david.pokedex_api.R
import com.david.pokedex_api.util.Lottie
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
import com.david.pokedex_api.api.model.TypeResponseSlot
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChipSmall
import com.david.pokedex_api.ui.theme.CardBorder // Asumo que tienes este color
import com.david.pokedex_api.ui.theme.color_progress_bar
import com.david.pokedex_api.util.TypeInteraction
import com.david.pokedex_api.util.getCombinedDefensiveInteractions

// Data class para la interacción (ya definida en el paso anterior)
// data class TypeInteraction(val attackingType: String, val multiplier: Double)

// Lista de todos los tipos (ya definida en el paso anterior)
// val ALL_POKEMON_TYPES = listOf(...)

@Composable
fun PokemonTypeInteractionsTable(
    // Ya no recibe 'interactions' directamente
    pokemonTypes: List<TypeResponseSlot>, // Los tipos del Pokémon actual
    pokemonApiService: PokeApiService, // Para hacer las llamadas API necesarias
    tableBackgroundColor: Color,
    textColor: Color = CardBorder,
    modifier: Modifier = Modifier
) {
    // Estado para las interacciones calculadas
    var calculatedInteractions by remember { mutableStateOf<List<TypeInteraction>>(emptyList()) }
    // Estado para la carga
    var isLoading by remember { mutableStateOf(false) }
    // Para recordar si ya intentamos cargar (evitar recargas en recomposiciones innecesarias)
    var hasAttemptedLoad by remember { mutableStateOf(false) }

    // Efecto para cargar las interacciones cuando los tipos del Pokémon cambian
    LaunchedEffect(key1 = pokemonTypes) {
        if (pokemonTypes.isNotEmpty()) {
            isLoading = true
            hasAttemptedLoad = true // Marcamos que el intento de carga ha comenzado
            // Es importante que getCombinedDefensiveInteractions sea una suspend fun
            // o que maneje sus propias corrutinas si no lo es.
            // Asumiendo que es una suspend fun:
            calculatedInteractions = getCombinedDefensiveInteractions(pokemonTypes, pokemonApiService)
            isLoading = false
        } else {
            calculatedInteractions = emptyList() // Limpiar si no hay tipos
            hasAttemptedLoad = true // También marcamos como intento si no hay tipos
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
        }
    } else if (calculatedInteractions.isNotEmpty()) {
        // El resto del Composable es igual que antes, mostrando la tabla
        val interactionsByMultiplier = calculatedInteractions.groupBy { it.multiplier }
            .toSortedMap(compareByDescending { it })

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(tableBackgroundColor)
                .scrollable(rememberScrollState(), orientation = Orientation.Vertical) // Hacer que la tabla sea scrollable
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            item{
                val typeNamesDisplay = pokemonTypes.joinToString(" / ") { it.type.name }
                Text(
//                    text = "Tipo ${pokemonTypeNameTranslator(typeNamesDisplay)}",
                    text = "Resistencias y debilidades",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
            item{

                interactionsByMultiplier.forEach { (multiplier, types) ->
                    if (types.isNotEmpty()) {
                        TypeInteractionRow(
                            multiplier = multiplier,
                            attackingTypes = types.map { it.attackingType },
                            textColor = textColor,
                            colorPokemonActual = pokemonTypes[0].type.name
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

        }
    } else if (hasAttemptedLoad && pokemonTypes.isNotEmpty()) {
        // Se intentó cargar pero no hay interacciones (podría ser un error en la API o tipos sin interacciones especiales)
        Text(
            text = "No se encontraron datos de interacciones para estos tipos.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    } else if (pokemonTypes.isEmpty() && hasAttemptedLoad) {
        Text(
            text = "El Pokémon no tiene tipos para calcular interacciones.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
    // No se muestra nada si hasAttemptedLoad es false y no está cargando (estado inicial)
}

@Composable
private fun TypeInteractionRow(
    multiplier: Double,
    attackingTypes: List<String>,
    textColor: Color,
    colorPokemonActual: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatMultiplier(multiplier), // Ej: "Recibe x2 de:"
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp), // Ajusta minSize según tus necesidades
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp), // Limita la altura si hay muchos tipos
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(2.dp)
        ) {
            items(attackingTypes) { typeName ->
                val colorClaro = typeName == colorPokemonActual
                PokemonTypeChipSmall(
                    // Un chip más pequeño para esta tabla
                    typeName = typeName,
                    colorClaro = colorClaro,
                    // El color del texto dentro del chip generalmente es blanco o negro,
                    // podrías necesitar lógica para determinarlo basado en el color del tipo.
                )
            }
        }
    }
}




// Función auxiliar para formatear el multiplicador
private fun formatMultiplier(multiplier: Double): String {
    return when (multiplier) {
        0.0 -> "Inmune:"
        0.25 -> "Resiste x4:"
        0.5 -> "Resiste x2:"
        1.0 -> "Daño x1:" // Podrías omitir mostrar los x1 si la lista es muy larga
        2.0 -> "Débil x2:"
        4.0 -> "Débil x4:"
        else -> "Multiplicador x${multiplier}:" // Para casos inesperados
    }
}