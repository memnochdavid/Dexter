package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.SpecialFormItemView
import com.david.pokedex_api.ui.theme.color_progress_bar
import kotlinx.coroutines.launch

// Reutilizamos la data class SpecialForm, o puedes crear una específica si prefieres
// data class RegionalForm(
//     val formName: String, // ej: "rattata-alola"
//     val displayName: String, // ej: "Alolan Rattata"
//     val regionName: String, // ej: "Alola"
//     val spriteUrl: String?
// )
// Por simplicidad, reutilizaré SpecialForm, pero adaptando la lógica de displayName.
// Si necesitas más campos específicos para formas regionales, crea una nueva data class.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonRegionalFormsView(
    pokemonSpeciesUrl: String?, // URL de la especie del Pokémon actual
    basePokemonName: String, // Nombre del Pokémon base para referencia en el título y displayNames
    pokemonApiService: PokeApiService, // Asegúrate que es tu interfaz de servicio actualizada
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    itemCardColor: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    onFormClick: (pokemonName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var regionalForms by remember { mutableStateOf<List<SpecialForm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val knownRegions = listOf("alola", "galar", "hisui", "paldea")

    LaunchedEffect(pokemonSpeciesUrl, basePokemonName) {
        if (pokemonSpeciesUrl == null) {
            isLoading = false
            error = "No species data available for regional forms."
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        regionalForms = emptyList()

        coroutineScope.launch {
            try {
                // CAMBIO 1: Usa el nuevo nombre de la función del servicio
                val speciesResponse = pokemonApiService.getPokemonSpeciesDetailsByUrl(pokemonSpeciesUrl)
                //                                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^

                if (speciesResponse.isSuccessful && speciesResponse.body() != null) {
                    val speciesDetail = speciesResponse.body()!!
                    val formsFound = mutableListOf<SpecialForm>()

                    speciesDetail.varieties.forEach { variety ->
                        if (!variety.isDefault) {
                            val formApiName = variety.pokemon.name
                            var displayName = ""
                            var identifiedRegion: String? = null

                            for (region in knownRegions) {
                                if (formApiName.contains("-$region")) {
                                    identifiedRegion = region
                                    break
                                }
                            }

                            if (identifiedRegion != null) {
                                val pokemonNamePart = basePokemonName.replaceFirstChar { it.titlecase(
                                    java.util.Locale.getDefault()) }
                                val regionTitleCase = identifiedRegion.replaceFirstChar { it.titlecase(
                                    java.util.Locale.getDefault()) }
                                displayName = "$regionTitleCase $pokemonNamePart"

                                if (identifiedRegion == "paldea" && formApiName.contains("-breed")) {
                                    val breed = formApiName.substringAfterLast("-").takeIf { it != "breed" }?.replaceFirstChar { it.titlecase(
                                        java.util.Locale.getDefault()) }
                                    if (breed != null) {
                                        displayName += " ($breed Breed)"
                                    }
                                } else if (formApiName.endsWith("-$identifiedRegion")) {
                                    // Simple regional form
                                } else {
                                    val suffix = formApiName.substringAfter("-$identifiedRegion").replace("-", " ")
                                    if (suffix.isNotBlank() && suffix != " breed") {
                                        displayName += suffix.split(" ")
                                            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                                            .let { " ($it)"}
                                    }
                                }

                                // CAMBIO 2: Usa la función estándar para obtener detalles del Pokémon por nombre
                                val formDetailsResponse = pokemonApiService.getPokemonDetails(formApiName)
                                //                                             ^^^^^^^^^^^^^^^^^

                                val sprite = if (formDetailsResponse.isSuccessful) {
                                    formDetailsResponse.body()?.sprites?.other?.officialArtwork?.frontDefault
                                        ?: formDetailsResponse.body()?.sprites?.frontDefault
                                } else {
                                    null
                                }
                                formsFound.add(SpecialForm(formApiName, displayName.trim(), sprite))
                            }
                        }
                    }
                    regionalForms = formsFound
                } else {
                    error = "Failed to load species details for regional forms: ${speciesResponse.message()}"
                }
            } catch (e: Exception) {
                error = "Error fetching regional forms: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = color_progress_bar)
        }
        return
    }

    if (error != null) {
        Box(modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        return
    }

    if (regionalForms.isEmpty()) {
        // No renderizar la tarjeta si no hay formas regionales encontradas
        // Puedes opcionalmente mostrar un texto aquí si lo prefieres
        // Text("No regional forms found for $basePokemonName.", textAlign = TextAlign.Center)
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent), // El color de fondo lo da el Column interior
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor) // Color de fondo principal de la tarjeta
        ) {
            Row( // Encabezado
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
//                Icon(
////                    imageVector = Icons.Filled.Public, // Icono para formas regionales
//                    contentDescription = "Regional Forms",
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                    modifier = Modifier.size(20.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Formas Regionales",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                items(regionalForms) { form ->
                    // Reutilizamos SpecialFormItemView, ya que la estructura es similar
                    SpecialFormItemView( // Asegúrate de que SpecialFormItemView esté disponible/importado
                        specialForm = form,
                        backgroundColor = itemCardColor,
                        onClick = { onFormClick(form.formName) },
                        colorTexto = colorTexto
                    )
                }
            }
        }
    }
}