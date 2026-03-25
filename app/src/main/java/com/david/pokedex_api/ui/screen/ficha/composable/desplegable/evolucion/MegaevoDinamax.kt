package com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion

import com.david.pokedex_api.api.model.PokemonSpeciesVariety

/*
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AllSpecialFormsSection(
    specialForms: List<Pair<String, PokemonSpeciesVariety>>,
    pokemonApiService: PokeApiService,
    onPokemonClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (specialForms.isNotEmpty()) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = "Formas Especiales en la Línea", // O el título que prefieras
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                specialForms.forEach { (basePokeName, formVariety) ->
                    // Aquí decides qué color de fondo quieres para estas tarjetas.
                    // Podrías pasarlo como parámetro o usar un color fijo.
                    val cardBackgroundColor = MaterialTheme.colorScheme.secondaryContainer
                    SpecialFormDisplay(
                        formVariety = formVariety,
                        apiService = pokemonApiService,
                        onFormClick = onPokemonClick,
                        basePokemonColor = cardBackgroundColor
                    )
                }
            }
        }
    }
}
*/
fun getPokemonSpriteUrlFromSpeciesVariety(variety: PokemonSpeciesVariety): String? {
    val id = variety.pokemon.url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
    return id?.let { "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$it.png" }
}

/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialFormDisplay(
    formVariety: PokemonSpeciesVariety,
    apiService: PokeApiService, // Para obtener el sprite de la forma específica si es necesario
    onFormClick: (String) -> Unit,
    basePokemonColor: Color, // Para usar como fondo de la mini-tarjeta
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var spriteUrl by remember { mutableStateOf<String?>(null) }
    var displayName by remember { mutableStateOf("") }
    var isLoadingSprite by remember { mutableStateOf(true) }

    LaunchedEffect(formVariety.pokemon.url, formVariety.pokemon.name) {
        isLoadingSprite = true

        // 1. Formatear el nombre para mostrar
        val nameParts = formVariety.pokemon.name.split("-")
        val baseNameFromVariety = nameParts.firstOrNull() ?: formVariety.pokemon.name

        // Intenta construir un nombre legible para la forma
        var tempDisplayName = nameParts.drop(1).joinToString(" ") { part ->
            when {
                part.equals("mega", ignoreCase = true) -> "Mega"
                part.equals("gmax", ignoreCase = true) -> "G-Max"
                part.equals("gigantamax", ignoreCase = true) -> "G-Max"
                part.equals("x", ignoreCase = true) -> "X"
                part.equals("y", ignoreCase = true) -> "Y"
                // Añade más casos comunes de formas especiales aquí
                part.equals("alola", ignoreCase = true) -> "Alola"
                part.equals("galar", ignoreCase = true) -> "Galar"
                part.equals("hisui", ignoreCase = true) -> "Hisui"
                part.equals("paldea", ignoreCase = true) -> "Paldea"
                part.equals("own-tempo", ignoreCase = true) -> "Own Tempo" // ej: rockruff-own-tempo
                else -> part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }.trim()

        // Si el nombre resultante está vacío (ej. "charizard-mega" -> "Mega")
        // o si es solo una letra como X/Y, puede que esté bien.
        // Si después de quitar el nombre base no queda nada significativo o es redundante:
        if (tempDisplayName.isEmpty() || tempDisplayName.equals(baseNameFromVariety, ignoreCase = true)) {
            // Intenta tomar la última parte si hay múltiples guiones (ej. "shaymin-sky")
            if (nameParts.size > 1) {
                tempDisplayName = nameParts.last().replaceFirstChar { it.titlecase() }
            } else { // Si solo es "pokemon-mega", usa "Mega"
                tempDisplayName = formVariety.pokemon.name.substringAfter(baseNameFromVariety)
                    .trim('-')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
        displayName = tempDisplayName.ifBlank { formVariety.pokemon.name } // Fallback al nombre completo si todo falla


        // 2. Obtener la URL del sprite
        try {
            // Intenta obtener los detalles del Pokémon específico para la forma (que puede tener mejores sprites)
            val detailResponse = apiService.getFullPokemonDetails(formVariety.pokemon.name)
            if (detailResponse.isSuccessful) {
                val sprites = detailResponse.body()?.sprites
                spriteUrl = sprites?.other?.officialArtwork?.frontDefault
                    ?: sprites?.frontDefault // Fallback a frontDefault si official-artwork no está
            }
            // Si después de la llamada anterior no se obtuvo sprite, usar el helper
            if (spriteUrl == null) {
                spriteUrl = getPokemonSpriteUrlFromSpeciesVariety(formVariety)
            }
        } catch (e: Exception) {
            // Si la llamada falla, usar el helper como fallback principal
            spriteUrl = getPokemonSpriteUrlFromSpeciesVariety(formVariety)
            // println("Error fetching details for ${formVariety.pokemon.name}: ${e.message}, falling back to default sprite logic.")
        }
        isLoadingSprite = false
    }

    if (isLoadingSprite) {
        Box(
            modifier = modifier
                .size(width = 80.dp, height = 100.dp) // Tamaño similar a la tarjeta final
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(30.dp))
        }
    } else if (spriteUrl != null && displayName.isNotBlank()) {
        ElevatedCard(
            onClick = { onFormClick(formVariety.pokemon.name) },
            modifier = modifier
                .widthIn(min = 70.dp, max = 90.dp) // Ancho
                .height(IntrinsicSize.Min), // Para que la altura se ajuste al contenido interno
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween, // Para empujar el texto hacia abajo
                modifier = Modifier
                    .background(basePokemonColor.copy(alpha = 0.9f)) // Color de fondo
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .fillMaxHeight() // Que la columna ocupe la altura de la tarjeta
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(spriteUrl)
                        .crossfade(true)
                        .error(R.drawable.ic_launcher_foreground) // Opcional: un placeholder de error
                        .build(),
                    contentDescription = displayName,
                    modifier = Modifier.size(60.dp), // Tamaño del sprite
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = displayName,
                    fontSize = 10.sp, // Tamaño de fuente del nombre
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp), // Espacio entre imagen y texto
                    maxLines = 2, // Permitir hasta dos líneas para nombres largos
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp // Ajuste de altura de línea
                )
            }
        }
    } else {
        // Opcional: Mostrar algo si no hay sprite o nombre (o si la forma no debe mostrarse)
        // Box(modifier = modifier.size(width = 80.dp, height = 100.dp).padding(4.dp)) { /* Placeholder o nada */ }
    }
}
*/