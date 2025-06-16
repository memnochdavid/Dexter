package com.david.pokedex_api.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
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
    pokemonApiService: PokeApiService,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    itemCardColor: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    onFormClick: (pokemonName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var regionalForms by remember { mutableStateOf<List<SpecialForm>>(emptyList()) } // Usamos SpecialForm por ahora
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val knownRegions = listOf("alola", "galar", "hisui", "paldea") // Añade más regiones según sea necesario

    LaunchedEffect(pokemonSpeciesUrl, basePokemonName) {
        if (pokemonSpeciesUrl == null) {
            isLoading = false
            error = "No species data available for regional forms."
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        regionalForms = emptyList() // Limpiar formas anteriores

        coroutineScope.launch {
            try {
                val speciesResponse = pokemonApiService.getSpeciesDetailsByUrl(pokemonSpeciesUrl)
                if (speciesResponse.isSuccessful && speciesResponse.body() != null) {
                    val speciesDetail = speciesResponse.body()!!
                    val formsFound = mutableListOf<SpecialForm>()

                    speciesDetail.varieties.forEach { variety ->
                        if (!variety.isDefault) {
                            val formApiName = variety.pokemon.name // ej: "rattata-alola", "tauros-paldea-aqua-breed"
                            var displayName = ""
                            var identifiedRegion: String? = null

                            // Intentar identificar la región
                            for (region in knownRegions) {
                                if (formApiName.contains("-$region")) {
                                    identifiedRegion = region
                                    break
                                }
                            }

                            if (identifiedRegion != null) {
                                // Construir el nombre a mostrar
                                // Queremos algo como "Alolan Rattata" o "Paldean Tauros (Aqua)"
                                val pokemonNamePart = basePokemonName.replaceFirstChar { it.titlecase() }
                                val regionTitleCase = identifiedRegion.replaceFirstChar { it.titlecase() }
                                displayName = "$regionTitleCase $pokemonNamePart"

                                // Manejar sub-formas o "breeds" como las de Paldea
                                if (identifiedRegion == "paldea" && formApiName.contains("-breed")) {
                                    val breed = formApiName.substringAfterLast("-").takeIf { it != "breed" }?.replaceFirstChar { it.titlecase() }
                                    if (breed != null) {
                                        displayName += " ($breed Breed)"
                                    }
                                } else if (formApiName.endsWith("-$identifiedRegion")) {
                                    // Forma regional simple, displayName ya está bien
                                } else {
                                    // Casos más complejos donde el nombre base no está al principio
                                    // ej. "meowth-galar", el pokemonNamePart es "Meowth", el identifiedRegion es "Galar" -> "Galar Meowth"
                                    // Esta lógica es un poco simplista y podría necesitar ajustes para todos los casos.
                                    // El objetivo es tener un nombre legible.
                                    // Si el formApiName es "raticate-alola-totem", queremos "Alolan Raticate (Totem)"
                                    // Podrías necesitar refinar esto según los patrones exactos de la API.
                                    // Si la forma es algo como "tauros-paldea-combat-breed", pokemonNamePart es "Tauros"
                                    // identifiedRegion es "paldea", displayName = "Paldean Tauros"
                                    // luego el "-combat-breed" necesita ser parseado
                                    val suffix = formApiName.substringAfter("-$identifiedRegion").replace("-", " ")
                                    if (suffix.isNotBlank() && suffix != " breed") { // Evitar añadir " Breed" dos veces
                                        displayName += suffix.split(" ")
                                            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                                            .let { " ($it)"} // Ej: (Combat Breed), (Totem)
                                    }
                                }


                                // Obtener el sprite para esta forma
                                val formDetailsResponse = pokemonApiService.getPokemonDetailsByNameForSprite(formApiName)
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
                e.printStackTrace() // Imprimir stack trace para depuración
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
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