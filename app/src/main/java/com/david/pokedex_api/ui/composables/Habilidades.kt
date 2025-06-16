package com.david.pokedex_api.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.AbilitySlot
import com.david.pokedex_api.api.service.PokeApiService // Asumo que este es el nombre
import com.david.pokedex_api.ui.theme.CardBorder // Asumo que tienes este color

@Composable
fun PokemonAbilitiesList(
    abilities: List<AbilitySlot>,
    backgroundColor: Color, // Color de fondo principal para la "tarjeta"
    textColor: Color = CardBorder,
    pokemonApiService: PokeApiService, // Para obtener detalles de la habilidad si es necesario
    modifier: Modifier = Modifier
) {
    if (abilities.isEmpty()) {
        Text(
            text = "Este Pokémon no tiene habilidades registradas.", // O un mensaje similar
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        return
    }

    Column(
        modifier = modifier // Este modifier debería venir con .fillMaxHeight() desde DetallesDesplegables
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
//            .padding(vertical = 8.dp) // Padding interno
    ) {
        Text(
            text = "Habilidades",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
//                .weight(1f) // Para que tome el espacio restante en la Column padre
                .padding(horizontal = 14.dp),
//            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(abilities, key = { it.ability.name }) { abilitySlot ->
                AbilityRow(
                    abilitySlot = abilitySlot,
                    textColor = textColor,
                    pokemonApiService = pokemonApiService
                    // Podrías pasar un color de fondo específico para la fila si quisieras
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
@Composable
fun AbilityRow(
    abilitySlot: AbilitySlot,
    textColor: Color,
    pokemonApiService: PokeApiService,
    // Puedes pasar un color de fondo para la fila si quieres que sea dinámico,
    // o definirlo directamente aquí.
    rowBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
) {
    var displayedAbilityName by remember {
        mutableStateOf(
            // Capitaliza y reemplaza guiones para el nombre por defecto
            abilitySlot.ability.name.replace("-", " ").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        )
    }
    // Estado para el efecto/descripción corta de la habilidad (opcional)
    var abilityShortEffect by remember { mutableStateOf<String?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    // --- Opcional: Cargar detalles de la habilidad (nombre traducido, efecto) ---
    // Descomenta y adapta este LaunchedEffect si quieres hacer una llamada API
    // para obtener más detalles de cada habilidad.
    LaunchedEffect(key1 = abilitySlot.ability.url) {
        // Para evitar llamadas repetidas si ya tienes la info o no la quieres, puedes añadir condiciones
        // if (displayedAbilityName == abilitySlot.ability.name.replace("-", " ").replaceFirstChar{...} && abilityShortEffect == null) {
        isLoadingDetails = true
        try {
            val response = pokemonApiService.getAbilityDetails(abilitySlot.ability.url)
            if (response.isSuccessful) {
                val details = response.body()

                // Actualizar el nombre mostrado si se encuentra una traducción
                details?.localizedNames?.find { it.language.name == "es" }?.name?.let {
                    displayedAbilityName = it
                }

                // Obtener el efecto corto en español
                details?.effectEntries?.find { it.language.name == "es" }?.shortEffect?.let {
                    abilityShortEffect = it.replace("\n", " ") // Reemplaza saltos de línea
                }
                // Si no hay efecto corto, podrías buscar un flavor text
                if (abilityShortEffect == null) {
                    details?.flavorTextEntries?.find { it.language.name == "es" }?.flavorText?.let {
                        abilityShortEffect = it.replace("\n", " ")
                    }
                }

            } else {
                // Manejar error de la API (ej: log, mostrar mensaje por defecto)
                println("Error fetching ability details: ${response.code()} for ${abilitySlot.ability.url}")
            }
        } catch (e: Exception) {
            // Manejar excepción de red u otras
            println("Exception fetching ability details: ${e.message} for ${abilitySlot.ability.url}")
        }
        isLoadingDetails = false
        // }
    }
    // --- Fin de la carga opcional de detalles ---

    Column( // Usamos Column para poder poner el nombre y debajo su efecto/descripción
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = displayedAbilityName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f), // Para que el nombre tome el espacio disponible
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (abilitySlot.isHidden) {
                Text(
                    text = "Oculta",
                    style = MaterialTheme.typography.labelSmall, // Un estilo más pequeño para "Oculta"
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary, // Un color distintivo para "Oculta"
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Mostrar el efecto corto si está disponible y no se está cargando
        if (isLoadingDetails) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                strokeWidth = 2.dp
            )
        } else {
            abilityShortEffect?.let { effect ->
                Text(
                    text = effect,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(top = 4.dp) // Espacio entre el nombre y la descripción
                        .fillMaxWidth(),
                    maxLines = 3, // Permite más líneas para la descripción
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
