package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder

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
    pokemonApiService: PokeApiService, // Asegúrate que este es el tipo de tu interfaz de servicio actualizada
    rowBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
) {
    var displayedAbilityName by remember {
        mutableStateOf(
            abilitySlot.ability.name.replace("-", " ").replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        )
    }
    var abilityShortEffect by remember { mutableStateOf<String?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = abilitySlot.ability.url) {
        isLoadingDetails = true
        try {
            // CAMBIO AQUÍ: Usa el nuevo nombre de la función del servicio
            val response = pokemonApiService.getAbilityDetailsByUrl(abilitySlot.ability.url)
            //                                  ^^^^^^^^^^^^^^^^^^^^^^

            if (response.isSuccessful) {
                val details = response.body()

                details?.localizedNames?.find { it.language.name == "es" }?.name?.let {
                    displayedAbilityName = it
                }

                details?.effectEntries?.find { it.language.name == "es" }?.shortEffect?.let {
                    abilityShortEffect = it.replace("\n", " ")
                }
                if (abilityShortEffect == null) {
                    details?.flavorTextEntries?.find { it.language.name == "es" }?.flavorText?.let {
                        abilityShortEffect = it.replace("\n", " ")
                    }
                }

            } else {
                println("Error fetching ability details: ${response.code()} for ${abilitySlot.ability.url}")
            }
        } catch (e: Exception) {
            println("Exception fetching ability details: ${e.message} for ${abilitySlot.ability.url}")
        }
        isLoadingDetails = false
    }

    Column(
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
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (abilitySlot.isHidden) {
                Text(
                    text = "Oculta",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (isLoadingDetails) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
                // color = color_progress_bar, // Asegúrate que esta variable de color está definida
                strokeWidth = 2.dp
            )
        } else {
            abilityShortEffect?.let { effect ->
                Text(
                    text = effect,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
