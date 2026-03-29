package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.david.pokedex_api.R
import com.david.pokedex_api.util.Lottie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonFormDetailResponse
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.SpecialFormItemView
import com.david.pokedex_api.ui.theme.color_progress_bar
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.Locale

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

                    // Filtrar variedades regionales y cargarlas en PARALELO
                    val regionalVarieties = speciesDetail.varieties.filter { variety ->
                        !variety.isDefault && knownRegions.any { region ->
                            variety.pokemon.name.contains("-$region")
                        }
                    }

                    val formsFound = regionalVarieties.map { variety ->
                        async {
                            val formApiName = variety.pokemon.name
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
                                var displayName = "$regionTitleCase $pokemonNamePart"

                                if (identifiedRegion == "paldea" && formApiName.contains("-breed")) {
                                    val breed = formApiName.substringAfterLast("-").takeIf { it != "breed" }?.replaceFirstChar { it.titlecase(
                                        java.util.Locale.getDefault()) }
                                    if (breed != null) {
                                        displayName += " ($breed Breed)"
                                    }
                                } else if (!formApiName.endsWith("-$identifiedRegion")) {
                                    val suffix = formApiName.substringAfter("-$identifiedRegion").replace("-", " ")
                                    if (suffix.isNotBlank() && suffix != " breed") {
                                        displayName += suffix.split(" ")
                                            .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
                                            .let { " ($it)"}
                                    }
                                }

                                try {
                                    val formDetailsResponse = pokemonApiService.getPokemonDetails(formApiName)
                                    val sprite = if (formDetailsResponse.isSuccessful) {
                                        formDetailsResponse.body()?.sprites?.other?.officialArtwork?.frontDefault
                                            ?: formDetailsResponse.body()?.sprites?.frontDefault
                                    } else null
                                    SpecialForm(formApiName, displayName.trim(), sprite)
                                } catch (e: Exception) { null }
                            } else null
                        }
                    }.awaitAll().filterNotNull()

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
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                regionalForms.forEach { form ->
                    SpecialFormItemView(
                        specialForm = form,
                        backgroundColor = itemCardColor,
                        onClick = { onFormClick(form.formName) },
                        colorTexto = colorTexto,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonFormsView(
    pokemonIdOrNameToFetch: String, // ID or name of the base Pokémon to fetch (e.g., "arceus" or "493")
    basePokemonNameForDisplay: String, // Name of the base Pokémon for display titles (e.g., "Arceus")
    pokemonApiService: PokeApiService,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    itemCardColor: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    onFormClick: (pokemonName: String, formApiName: String) -> Unit, // Base name of clicked Pokemon, API name of the form
    modifier: Modifier = Modifier
) {
    var formsList by remember { mutableStateOf<List<SpecialForm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pokemonIdOrNameToFetch) {
        if (pokemonIdOrNameToFetch.isBlank()) {
            isLoading = false
            error = "Pokémon ID or name is required."
            formsList = emptyList()
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        formsList = emptyList()

        coroutineScope.launch {
            try {
                // 1. Fetch the base Pokémon details to get the list of form URLs/names
                val pokemonDetailsResponse: Response<PokemonDetailResponse> =
                    pokemonApiService.getPokemonDetails(pokemonIdOrNameToFetch.lowercase(java.util.Locale.ROOT))

                if (pokemonDetailsResponse.isSuccessful && pokemonDetailsResponse.body() != null) {
                    val pokemonDetail = pokemonDetailsResponse.body()!!
                    val apiForms = pokemonDetail.forms // This is List<NamedApiResource>

                    if (apiForms.isNullOrEmpty()) {
                        isLoading = false
                        // No distinct other forms listed, or only its default form.
                        // Display nothing or a specific message if needed.
                        return@launch
                    }

                    // 2. Fetch details for each form to get sprites and localized names
                    val detailedFormsDeferred = apiForms.mapNotNull { formResource ->
                        // Don't fetch the default form again if it's the same as the base one.
                        // The `formResource.name` usually matches `pokemonDetail.name` for the default.
                        // However, some Pokémon list only their default form here.
                        // We will filter out later if it's the absolute base form.
                        // The primary goal is to show forms that are *different*.

                        async {
                            try {
                                // Use the full URL from formResource.url for fetching form details
                                val formDetailResponse: Response<PokemonFormDetailResponse> =
                                    pokemonApiService.getPokemonFormDetailsByUrl(formResource.url)

                                if (formDetailResponse.isSuccessful && formDetailResponse.body() != null) {
                                    val formDetail = formDetailResponse.body()!!

                                    // Skip the absolute default form of the Pokémon itself,
                                    // unless it's the *only* form listed (e.g. some Pokemon).
                                    // A simple check could be if formDetail.name is the same as pokemonDetail.name.
                                    // However, for something like Arceus, "arceus-normal" is its default form,
                                    // but we still want to show it among "arceus-fire", etc.
                                    // A better check is if `formDetail.isDefault` is true AND its `formDetail.pokemon.name`
                                    // matches the initially fetched `pokemonIdOrNameToFetch` (or `pokemonDetail.name`).
                                    // We'll let `SpecialFormItemView` handle not showing if sprite is null.
                                    // For now, let's process all forms and the UI can decide if one is "too default" to show.

                                    val lang = java.util.Locale.getDefault().language
                                    var displayName = ""

                                    // Try to get a specific localized form name (e.g., "Alola Form", "Sunny Form")
                                    val localizedFormVariantName = formDetail.localizedFormNames
                                        .firstOrNull { it.language.name == lang }?.name?.takeIf { it.isNotBlank() }
                                        ?: formDetail.localizedFormNames
                                            .firstOrNull { it.language.name == "en" }?.name?.takeIf { it.isNotBlank() }


                                    // Try to get the localized Pokémon name for this specific form
                                    val localizedPokemonNameInForm = formDetail.localizedPokemonNames
                                        .firstOrNull { it.language.name == lang }?.name
                                        ?: formDetail.localizedPokemonNames
                                            .firstOrNull { it.language.name == "en" }?.name
                                        ?: basePokemonNameForDisplay // Fallback to base display name

                                    if (!localizedFormVariantName.isNullOrBlank()) {
                                        // If we have a specific variant name like "Alolan" or "Sunshine Form"
                                        displayName = "$localizedPokemonNameInForm ($localizedFormVariantName)"
                                    } else {
                                        // If no specific variant name, construct from formDetail.name
                                        // e.g. "arceus-fire" -> "Arceus (Fire)"
                                        // or if formDetail.pokemon.name is different from basePokemonNameForDisplay
                                        val formSuffix = formDetail.name.removePrefix(formDetail.pokemon.name + "-").takeIf { it != formDetail.name }
                                        displayName = if (!formSuffix.isNullOrBlank()) {
                                            "$localizedPokemonNameInForm (${formSuffix.replaceFirstChar { it.titlecase(java.util.Locale.getDefault()) }})"
                                        } else {
                                            localizedPokemonNameInForm // Just "Arceus" for "arceus-normal"
                                        }
                                    }
                                    val spriteUrl = formDetail.sprites.frontDefault
                                        ?: pokemonDetail.sprites.other?.officialArtwork?.frontDefault // Fallback to base official art
                                        ?: pokemonDetail.sprites.frontDefault // Fallback to base default sprite

                                    // Special case for forms that are effectively different Pokémon (like Megas, some regional variants if API lists them here)
                                    // The `formDetail.pokemon.name` is the actual Pokémon name for this form (e.g., "charizard-mega-x")
                                    // The `formDetail.name` is the form's own unique name (e.g. "charizard-mega-x")
                                    // For click, we often want the `formDetail.pokemon.name` if it refers to a distinct Pokémon entry.
                                    // However, for consistency with onFormClick: (pokemonName: String, formApiName: String)
                                    // We will pass the base pokemon name (pokemonDetail.name) and the specific form's api name (formDetail.name)
                                    SpecialForm(
                                        formName = formDetail.name, // API name of the form itself e.g. "arceus-fire"
                                        displayName = displayName.trim(),
                                        spriteUrl = spriteUrl
                                    )
                                } else {
                                    null // Error fetching this specific form detail
                                }
                            } catch (e: Exception) {
                                // Log or handle individual form fetch error
                                null
                            }
                        }
                    }

                    val results = detailedFormsDeferred.awaitAll().filterNotNull()

                    // Filter out the absolute default form if other forms exist and it's not unique
                    // (e.g. if "Pikachu" (form) is listed alongside "Pikachu Rock Star")
                    // A simple way: if a form's name is the same as the base Pokémon's name AND it's marked as default by the API,
                    // AND there's more than one form, we might not need to show it explicitly if its sprite and name are identical to the base.
                    // However, for Pokémon like Arceus, "arceus-normal" is `isDefault=true` for its form, but we want to show it.
                    // The crucial part is usually handled by the `formDetail.name` and the constructed `displayName`.
                    // If `pokemonIdOrNameToFetch` is "arceus" and one form is "arceus-normal" (default),
                    // its `displayName` would likely become "Arceus (Normal)" or just "Arceus".

                    // Let's filter if a form's pokemon name is exactly the base name and it's default,
                    // ONLY IF there are other non-default forms to show.
                    // This is tricky because `formDetail.pokemon.name` might be "arceus" for all Arceus forms.
                    // `formDetail.name` ("arceus-normal", "arceus-fire") is more distinguishing.

                    // For now, let's show all fetched forms. The user can click and see.
                    // If a form is truly identical (same name, same sprite as the main display), it might be redundant,
                    // but the naming convention (e.g. "Arceus (Normal)") should usually differentiate it.

                    formsList = results.filter {
                        // Example: if pokemonIdOrNameToFetch is "unown", its default form is "unown" (which is also "unown-a")
                        // The `pokemonDetail.forms` for "unown" lists many like "unown-b", "unown-c" etc.
                        // and also one that might resolve to "unown" or "unown-a".
                        // We want to avoid showing the base "unown" if it's identical to the main page display AND other forms exist.
                        // This is a heuristic.
                        if (results.size > 1 && it.formName.equals(pokemonDetail.name, ignoreCase = true) &&
                            pokemonDetail.forms.any { f -> f.name.equals(it.formName, ignoreCase = true) &&
                                    pokemonApiService.getPokemonFormDetailsByUrl(f.url).body()?.isDefault == true }
                        ) {
                            // Potentially the default form that's redundant if others exist.
                            // Let's keep it for now as differentiation might be in the (Suffix).
                            true // For now, include all successfully fetched forms
                        } else {
                            true
                        }
                    }

                } else {
                    error = "Failed to load base Pokémon details for forms: ${pokemonDetailsResponse.message()}"
                }
            } catch (e: Exception) {
                error = "Error fetching Pokémon forms: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (formsList.isEmpty()) {
        // Optionally, display a message if no forms were found or all were filtered out.
        // Text("No additional forms found for $basePokemonNameForDisplay.", textAlign = TextAlign.Center, modifier = modifier.padding(16.dp))
        return
    }

    // Only show the card if there's more than one form, or if the single form is meaningfully different.
    // For a Pokémon like Arceus, it has many forms, including "arceus-normal".
    // For a Pokémon like Pikachu, it might only list its default form.
    // A simple heuristic: if there's only one form and its `formName` is the same as the base `pokemonIdOrNameToFetch`,
    // it's likely the default form and not worth showing in a separate "Forms" section.
    if (formsList.size == 1 && formsList.first().formName.equals(pokemonIdOrNameToFetch, ignoreCase = true)) {
        // This is likely the default form, same as the one being displayed on the main page.
        // You might decide not to show this "Forms" card at all.
        // For example, if you fetch "pikachu", and `formsList` only contains "pikachu" (default form), hide this section.
        // However, if you fetch "arceus", `formsList` has "arceus-normal", "arceus-fire" etc. "arceus-normal" IS a distinct form to list.
        // This check needs to be robust. `formName` in `SpecialForm` is the API name of the form.
        val defaultFormDetails = formsList.first()
        if (defaultFormDetails.displayName.equals(basePokemonNameForDisplay, ignoreCase = true) ||
            defaultFormDetails.formName.equals(basePokemonNameForDisplay.lowercase(java.util.Locale.ROOT))) {
            // It's the default and the display name matches the base Pokémon name without any suffix,
            // so no need to show a redundant "Forms" card.
            return
        }
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
                    text = "Otras Formas", // Title: "Other Forms"
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface // Or use colorTexto
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                items(formsList) { form ->
                    // Reutilizamos SpecialFormItemView, ya que la estructura es similar
                    SpecialFormItemView( // Asegúrate de que SpecialFormItemView esté disponible/importado
                        specialForm = form,
                        backgroundColor = itemCardColor,
                        onClick = {
                            // The `form.formName` is the specific API name for this form (e.g., "arceus-fire")
                            // The `pokemonIdOrNameToFetch` is the name/ID of the base Pokémon (e.g., "arceus")
                            // However, the `formDetail.pokemon.name` inside the loop (if you were to pass it)
                            // would be the true Pokémon this form represents (e.g., "charizard-mega-x" for a Mega Charizard X form).
                            // For the onFormClick lambda, we want to pass the base Pokemon name (used for fetching)
                            // and the specific API name of the form that was clicked.
                            onFormClick(pokemonDetail.name, form.formName)
                        },
                        colorTexto = colorTexto
                    )
                }
            }
        }
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonFormsView(
    pokemon : PokemonDetailResponse,
    pokemonApiService: PokeApiService,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    itemCardColor: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    onFormClick: (pokemonName: String, formApiName: String) -> Unit, // Base name of clicked Pokemon, API name of the form
    modifier: Modifier = Modifier
) {
    var formsList by remember { mutableStateOf<List<SpecialForm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Store the fetched base Pokemon name to be accessible by onClick
    var fetchedBasePokemonName by remember { mutableStateOf(pokemon.id.toString())}

    LaunchedEffect(pokemon.id.toString()) {
        if (pokemon.id.toString().isBlank()) {
            isLoading = false
            error = "Pokémon ID or name is required."
            formsList = emptyList()
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        formsList = emptyList() // Clear previous forms

        coroutineScope.launch {
            try {
                // 1. Fetch the base Pokémon details to get the list of form URLs/names
                val pokemonDetailsResponse: Response<PokemonDetailResponse> =
                    pokemonApiService.getPokemonDetails(pokemon.id.toString().lowercase(java.util.Locale.ROOT))

                if (pokemonDetailsResponse.isSuccessful && pokemonDetailsResponse.body() != null) {
                    val pokemonDetail = pokemonDetailsResponse.body()!!
                    fetchedBasePokemonName = pokemonDetail.name // Update with the actual name from API response
                    val apiForms = pokemonDetail.forms

                    if (apiForms.isNullOrEmpty()) {
                        isLoading = false
                        return@launch
                    }

                    // 2. Fetch details for each form
                    val detailedFormsDeferred = apiForms.map { formResource ->
                        async {
                            try {
                                val formDetailResponse: Response<PokemonFormDetailResponse> =
                                    pokemonApiService.getPokemonFormDetailsByUrl(formResource.url)

                                if (formDetailResponse.isSuccessful && formDetailResponse.body() != null) {
                                    val formDetail = formDetailResponse.body()!!
                                    val lang = java.util.Locale.getDefault().language
                                    var displayName = ""

                                    val localizedFormVariantName = formDetail.localizedFormNames
                                        .firstOrNull { it.language.name == lang }?.name?.takeIf { it.isNotBlank() }
                                        ?: formDetail.localizedFormNames
                                            .firstOrNull { it.language.name == "en" }?.name?.takeIf { it.isNotBlank() }

                                    val localizedPokemonNameInForm = formDetail.localizedPokemonNames
                                        .firstOrNull { it.language.name == lang }?.name
                                        ?: formDetail.localizedPokemonNames
                                            .firstOrNull { it.language.name == "en" }?.name
                                        ?: pokemon.name // Fallback to base display name parameter

                                    if (!localizedFormVariantName.isNullOrBlank()) {
                                        displayName = "$localizedPokemonNameInForm ($localizedFormVariantName)"
                                    } else {
                                        val formSuffix = formDetail.name.removePrefix(formDetail.pokemon.name + "-").takeIf { it != formDetail.name }
                                        displayName = if (!formSuffix.isNullOrBlank()) {
                                            "$localizedPokemonNameInForm (${formSuffix.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                                                java.util.Locale.getDefault()) else it.toString() }})"
                                        } else {
                                            // If the form name is the same as the pokemon name (e.g. "pikachu" for form "pikachu")
                                            // and it's the default form, just use the localized pokemon name.
                                            // Otherwise, it might be something like "Arceus (Normal)" where formDetail.name is "arceus-normal"
                                            if (formDetail.name.equals(formDetail.pokemon.name, ignoreCase = true) && formDetail.isDefault) {
                                                localizedPokemonNameInForm
                                            } else {
                                                // Construct a name if it's different but not a standard suffix like "-mega"
                                                // For example, if formDetail.name is "deoxys-attack" and pokemon.name is "deoxys"
                                                val autoSuffix = formDetail.name.removePrefix(pokemonDetail.name + "-")
                                                if (autoSuffix.isNotEmpty() && autoSuffix != formDetail.name) {
                                                    "$localizedPokemonNameInForm (${autoSuffix.replaceFirstChar { it.titlecase(
                                                        java.util.Locale.getDefault()) }})"
                                                } else {
                                                    localizedPokemonNameInForm // Fallback
                                                }
                                            }
                                        }
                                    }

                                    val spriteUrl = pokemonDetail.sprites.other?.officialArtwork?.frontDefault
                                        ?: formDetail.sprites.frontDefault
                                        ?: pokemonDetail.sprites.frontDefault

                                    SpecialForm(
                                        formName = formDetail.name, // API name of the form itself e.g. "arceus-fire"
                                        displayName = displayName.trim().capitalize(Locale.ROOT),
                                        spriteUrl = spriteUrl
                                    )
                                } else {
                                    null // Error fetching this specific form detail
                                }
                            } catch (e: Exception) {
                                null // Error during individual form processing
                            }
                        }
                    }

                    val results = detailedFormsDeferred.awaitAll().filterNotNull()
                    formsList = results

                } else {
                    error = "Failed to load base Pokémon details for forms: ${pokemonDetailsResponse.message()}"
                }
            } catch (e: Exception) {
                error = "Error fetching Pokémon forms: ${e.localizedMessage}"
                // e.printStackTrace() // Consider logging instead of printStackTrace in production
            } finally {
                isLoading = false
            }
        }
    }
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text(
                "Error: $error",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Heuristic: If there's only one form and its name matches the original fetched name/ID,
    // and its display name is the same as the base display name, it's likely redundant.
    if (formsList.size == 1) {
        val singleForm = formsList.first()
        // Check if the form's API name matches the originally fetched name/ID (could be numeric ID or name)
        val formNameMatchesFetched = singleForm.formName.equals(pokemon.name, ignoreCase = true)
        // Check if the form's display name is the same as the base display name for the Pokémon
        val displayNameMatchesBase = singleForm.displayName.equals(pokemon.name, ignoreCase = true)

        if (formNameMatchesFetched && displayNameMatchesBase) {
            // This is likely the default form, identical to what's already shown.
            return
        }
        // Additional check for cases like "arceus" and its default form "arceus-normal"
        // which has a display name like "Arceus (Normal)" or "Arceus"
        // If formName is pokemonIdOrNameToFetch + "-normal" and it's the only one, still potentially hide
        // This logic can get complex depending on desired behavior for all edge cases
        if (singleForm.formName.equals("${pokemon.name}-normal", ignoreCase = true) && displayNameMatchesBase) {
            // e.g. fetched "arceus", form is "arceus-normal", display name became "Arceus"
            // This is still essentially the default, hide the "Other Forms" card.
            return
        }
    }


    if (formsList.isEmpty()) {
        // No forms to display, or all were filtered out.
        // Optionally, display a message if no forms were found.
        // Text("No additional forms found for $basePokemonNameForDisplay.", textAlign = TextAlign.Center, modifier = modifier.padding(16.dp))
        return
    }


    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent), // Background color provided by the inner Column
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor) // Main background color for the card's content
        ) {
            Row( // Header
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Otras Formas", // Title: "Other Forms"
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface // Or use your colorTexto
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                formsList.forEach { form ->
                    SpecialFormItemView(
                        specialForm = form,
                        backgroundColor = itemCardColor,
                        onClick = {
                            onFormClick(fetchedBasePokemonName, form.formName)
                        },
                        colorTexto = colorTexto,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
