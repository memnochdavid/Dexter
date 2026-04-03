package com.david.pokedex_api.ui.screen.ficha.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.david.pokedex_api.R
import com.david.pokedex_api.util.Lottie
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
// import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.color_boton_busqueda
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.LiveSprites
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.MuestraDesc
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.MuestraStatsBase
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.InfoPokemon
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonAbilitiesList
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.PokemonEvolutionChainView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonMovesList
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonRegionalFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.PokemonSpecialFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonTypeInteractionsTable
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorDark
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorSurface
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorTypeChip
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonEncountersView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.descripcionesMegasGigas

enum class SectionPage(val label: String) {
    DESC("Descripción"),
    EVOS("Evolución"),
    FORM("Formas"),
    STATS("Stats"),
    ABILITY("Habilidades"),
    MOVES("Movimientos"),
    INTER("Tipos"),
    INFO("Info"),
    ENCOUNTERS("Ubicaciones"),
    SPRITES("Sprites"),
}

@Composable
fun DetallesDesplegables(
    pokemon: PokemonDetailResponse,
    evolutionChainDetailResponse: EvolutionChainDetailResponse?,
    isLoadingEvolutionChain: Boolean,
    onEvolutionPokemonClick: (pokemonName: String) -> Unit,
    description: String?,
    pokemonApiService: PokeApiService,
    moveDetailsMap: Map<String, MoveDetailResponse> = emptyMap(),
    pokemonSpecies: PokemonSpeciesResponse? = null,
    wikiDexFlavorTexts: Map<String, String> = emptyMap(),
    wikiDexLocations: Map<String, String> = emptyMap(),
    encounters: List<com.david.pokedex_api.api.model.GameEncounterGroup> = emptyList(),
    isLoadingEncounters: Boolean = false,
    selectedSection: String = SectionPage.DESC.name,
    onSectionSelected: (String) -> Unit = {},
    onAvailableSectionsChanged: (List<String>) -> Unit = {},
    onFormSwap: (resourceName: String, displayName: String, types: List<String>?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val typeName = pokemon.types[0].type.name
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val selectorWeight = if (isLandscape) 0.20f else 0.35f
    val contentWeight = if (isLandscape) 0.80f else 0.65f

    // Paleta neutra + acentos del tipo
    val colorTexto = Color(0xFF1A1A1A)                                  // texto principal
    val colorTextoSecundario = Color(0xFF5A5A5A)                        // texto secundario
    val colorDark = getPokemonTypeColorDark(typeName)                   // acento fuerte (barra secciones, headers)
    val colorDarker = Color(
        colorDark.red * 0.75f,
        colorDark.green * 0.75f,
        colorDark.blue * 0.75f,
        colorDark.alpha
    )
    val colorAccent = getPokemonTypeColorTypeChip(typeName)             // acento (bordes seleccion, chips)
    val colorCard = Color.White.copy(alpha = 0.65f)                     // cards principales
    val colorCardInner = Color.White.copy(alpha = 0.40f)                // cards internas / dropdowns
    val colorComponentBg = background_app                               // fondo general secciones

    // Aliases para compatibilidad con secciones existentes
    val colorSoft = colorCard
    val colorSurface = colorCardInner
    val colorDropdown = colorCardInner

    val currentSection = try { SectionPage.valueOf(selectedSection) } catch (_: Exception) { SectionPage.DESC }

    // Filtrar secciones que tienen contenido (sin remember para reaccionar a cambios async)
    val availableSections = SectionPage.entries.filter { section ->
        when (section) {
            SectionPage.DESC -> true
            SectionPage.SPRITES -> true
            SectionPage.STATS -> pokemon.stats.isNotEmpty()
            SectionPage.EVOS -> evolutionChainDetailResponse != null || isLoadingEvolutionChain ||
                pokemonSpecies?.varieties?.any { v ->
                    val name = v.pokemon.name
                    !v.isDefault && (name.contains("-mega") || name.contains("-gmax"))
                } == true
            SectionPage.MOVES -> pokemon.moves.isNotEmpty()
            SectionPage.ABILITY -> pokemon.abilities.isNotEmpty()
            SectionPage.INTER -> pokemon.types.isNotEmpty()
            SectionPage.FORM -> {
                val baseName = pokemon.name
                val knownRegions = listOf("alola", "galar", "hisui", "paldea")
                val hasRegionals = pokemonSpecies?.varieties?.any { v ->
                    if (v.isDefault) return@any false
                    val name = v.pokemon.name
                    knownRegions.any { region ->
                        name == "$baseName-$region" ||
                            (region == "paldea" && name.startsWith("$baseName-$region-"))
                    }
                } == true
                val hasOtherVarieties = pokemonSpecies?.varieties?.any { v ->
                    if (v.isDefault) return@any false
                    val name = v.pokemon.name
                    if (name.contains("-mega") || name.contains("-gmax")) return@any false
                    val isRegional = knownRegions.any { region ->
                        name == "$baseName-$region" ||
                            (region == "paldea" && name.startsWith("$baseName-$region-"))
                    }
                    !isRegional
                } == true
                hasRegionals || hasOtherVarieties || pokemon.forms.size > 1
            }
            SectionPage.INFO -> pokemonSpecies != null
            SectionPage.ENCOUNTERS -> true // se carga async, mostrar siempre
        }
    }

    LaunchedEffect(availableSections) {
        onAvailableSectionsChanged(availableSections.map { it.name })
    }

    val haptic = LocalHapticFeedback.current

    // Consume scroll horizontal sobrante para que no pase al HorizontalPager padre
    val consumeHorizontalScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return Offset(available.x, 0f)
            }
        }
    }

    Column(modifier = modifier) {
        // Barra horizontal de secciones — chips/pills con texto
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(consumeHorizontalScroll)
                .background(colorComponentBg)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            items(availableSections.size) { index ->
                val section = availableSections[index]
                val isSelected = section == currentSection
                Text(
                    text = section.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else colorTexto.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) colorDark
                            else Color(0xFFE8E8E8)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSectionSelected(section.name)
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // Contenido de la seccion activa
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorComponentBg)
        ) {
            when (currentSection) {
                SectionPage.DESC -> {
                    // Formas regionales: PokeAPI solo tiene textos de la species base, no de la forma.
                    // WikiDex SI tiene textos especificos. Prioridad cambia segun si es regional.
                    val isRegionalForm = remember(pokemon.name) {
                        listOf("-alola", "-galar", "-hisui", "-paldea").any { pokemon.name.contains(it) }
                    }

                    val flavorEntries = remember(pokemonSpecies, wikiDexFlavorTexts, isRegionalForm) {
                        val entries = pokemonSpecies?.flavorTextEntries ?: emptyList()
                        // Para regionales, los textos de PokeAPI son del base y no corresponden
                        val spanishByVersion = if (!isRegionalForm) {
                            entries.filter { it.language.name == "es" && !it.flavorText.isNullOrBlank() }
                                .associateBy { it.version.name }
                        } else emptyMap()
                        val englishByVersion = if (!isRegionalForm) {
                            entries.filter { it.language.name == "en" && !it.flavorText.isNullOrBlank() }
                                .associateBy { it.version.name }
                        } else emptyMap()

                        // Merge: WikiDex (prioridad para regionales) + PokeAPI ES + PokeAPI EN
                        val allVersions = (wikiDexFlavorTexts.keys + spanishByVersion.keys + englishByVersion.keys).distinct()
                        allVersions.mapNotNull { version ->
                            val text = wikiDexFlavorTexts[version]
                                ?: spanishByVersion[version]?.flavorText
                                ?: englishByVersion[version]?.flavorText
                                ?: return@mapNotNull null
                            val cleaned = text.replace("\n", " ").replace("\u000c", " ").replace("POKéMON", "Pokémon")
                            version to cleaned
                        }.sortedBy { getGameReleaseOrder(it.first) }
                    }

                    if (flavorEntries.isEmpty()) {
                        val fallback = descripcionesMegasGigas.find { it.pokeId == pokemon.id }?.desc
                            ?: "No hay descripcion disponible."
                        Box(
                            modifier = Modifier.fillMaxSize().background(colorSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = fallback, style = MaterialTheme.typography.bodyLarge, color = colorTexto, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        var selectedVersion by rememberSaveable { mutableStateOf(flavorEntries.first().first) }
                        // Si la version guardada no existe en las entradas actuales, seleccionar la primera
                        if (flavorEntries.none { it.first == selectedVersion }) {
                            selectedVersion = flavorEntries.first().first
                        }
                        val selectedText = flavorEntries.firstOrNull { it.first == selectedVersion }?.second ?: ""

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorSoft)
                                .padding(start = 10.dp, end = 10.dp, top = 10.dp)
                        ) {
                            // Titulo: Descripcion + nombre del juego
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Descripción",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorTexto
                                )
                                Text(
                                    text = "  \u2022  ",
                                    fontSize = 14.sp,
                                    color = colorTexto.copy(alpha = 0.3f)
                                )
                                Text(
                                    text = translateGameVersion(selectedVersion),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorAccent
                                )
                            }

                        Row(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Columna izquierda: caratulas de juego (selector)
                            LazyColumn(
                                modifier = Modifier
                                    .weight(selectorWeight)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                items(flavorEntries, key = { it.first }) { (version, _) ->
                                    val isSelected = version == selectedVersion
                                    val coverRes = getGameCoverResId(version)
                                    val haptic = LocalHapticFeedback.current

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) colorDropdown.copy(alpha = 0.7f)
                                                else colorDropdown.copy(alpha = 0.15f)
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(
                                                    2.dp,
                                                    colorAccent,
                                                    RoundedCornerShape(10.dp)
                                                ) else Modifier
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                selectedVersion = version
                                            }
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (coverRes != 0) {
                                            Image(
                                                painter = painterResource(id = coverRes),
                                                contentDescription = translateGameVersion(version),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.FillWidth
                                            )
                                        }
                                        Text(
                                            text = translateGameVersion(version),
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = colorTexto.copy(alpha = if (isSelected) 1f else 0.55f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            // Columna derecha: descripcion del juego seleccionado
                            Column(
                                modifier = Modifier
                                    .weight(contentWeight)
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Top
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorDropdown.copy(alpha = 0.4f))
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "\u201C",
                                        fontSize = 28.sp,
                                        lineHeight = 28.sp,
                                        color = colorAccent.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedText,
                                        fontSize = 18.sp,
                                        lineHeight = 27.sp,
                                        color = colorTexto,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Text(
                                        text = "\u201D",
                                        fontSize = 28.sp,
                                        lineHeight = 28.sp,
                                        color = colorAccent.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                SectionPage.SPRITES -> {
                    LiveSprites(
                        pokemon = pokemon,
                        colorTexto = colorTexto,
                        nombreSpanish = pokemon.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.STATS -> {
                    MuestraStatsBase(
                        stats = pokemon.stats,
                        colorFondo = colorSoft,
                        colorTexto = colorTexto
                    )
                }

                SectionPage.EVOS -> {
                    val hasMegasGigas = pokemonSpecies?.varieties?.any { v ->
                        val name = v.pokemon.name
                        !v.isDefault && (name.contains("-mega") || name.contains("-gmax"))
                    } == true

                    var showSpecials by remember { mutableStateOf(false) }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-selector (solo si hay megas/gigas)
                        if (hasMegasGigas) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(false to "Línea Evolutiva", true to "Especiales").forEach { (isSpecial, label) ->
                                    val isSelected = showSpecials == isSpecial
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else colorTexto.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) colorDark else Color(0xFFE8E8E8))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                showSpecials = isSpecial
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Contenido
                        if (!showSpecials) {
                            if (isLoadingEvolutionChain && evolutionChainDetailResponse == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
                                }
                            } else if (evolutionChainDetailResponse != null) {
                                PokemonEvolutionChainView(
                                    evolutionChainResponse = evolutionChainDetailResponse,
                                    onPokemonClick = onEvolutionPokemonClick,
                                    color1 = colorSoft,
                                    color2 = colorSurface,
                                    colorTexto = colorTexto,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No se pudieron cargar los datos de evolución.", color = colorTexto)
                                }
                            }
                        } else {
                            PokemonSpecialFormsView(
                                pokemonSpeciesUrl = pokemon.species.url,
                                pokemonApiService = pokemonApiService,
                                onFormClick = { onEvolutionPokemonClick(it) },
                                cardColor = colorSoft,
                                itemCardColor = colorSurface,
                                colorTexto = colorTexto,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                SectionPage.MOVES -> {
                    PokemonMovesList(
                        moves = pokemon.moves,
                        cardBackgroundColor = colorSoft,
                        pokemonApiService = pokemonApiService,
                        moveDetailsMap = moveDetailsMap,
                        textColor = colorTexto,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.ABILITY -> {
                    PokemonAbilitiesList(
                        abilities = pokemon.abilities,
                        backgroundColor = colorSoft,
                        textColor = colorTexto,
                        pokemonApiService = pokemonApiService,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.INTER -> {
                    PokemonTypeInteractionsTable(
                        pokemonTypes = pokemon.types,
                        pokemonApiService = pokemonApiService,
                        tableBackgroundColor = colorSoft,
                        textColor = colorTexto
                    )
                }

                SectionPage.FORM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PokemonRegionalFormsView(
                            pokemonSpeciesUrl = pokemon.species.url,
                            basePokemonName = pokemon.name,
                            pokemonApiService = pokemonApiService,
                            onFormClick = { onEvolutionPokemonClick(it) },
                            cardColor = colorSoft,
                            colorTexto = colorTexto,
                            itemCardColor = colorSoft,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PokemonFormsView(
                            pokemon = pokemon,
                            pokemonSpeciesUrl = pokemon.species.url,
                            pokemonApiService = pokemonApiService,
                            onFormClick = { _, formApiName -> onEvolutionPokemonClick(formApiName) },
                            onFormSwap = onFormSwap,
                            cardColor = colorSoft,
                            itemCardColor = colorSoft,
                            colorTexto = colorTexto,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                SectionPage.INFO -> {
                    InfoPokemon(
                        pokemon = pokemon,
                        species = pokemonSpecies,
                        colorFondo = colorSoft,
                        colorTexto = colorTexto
                    )
                }

                SectionPage.ENCOUNTERS -> {
                    PokemonEncountersView(
                        encounters = encounters,
                        wikiDexLocations = wikiDexLocations,
                        isLoading = isLoadingEncounters,
                        colorFondo = colorSoft,
                        colorTexto = colorTexto,
                        colorDropdown = colorDropdown,
                        colorAccent = colorAccent
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameVersionSelector(
    versions: List<String>,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
    colorFondo: Color,
    colorTexto: Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = translateGameVersion(selectedVersion),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = colorFondo.copy(alpha = 0.5f),
                unfocusedContainerColor = colorFondo.copy(alpha = 0.3f),
                focusedBorderColor = colorTexto.copy(alpha = 0.3f),
                unfocusedBorderColor = colorTexto.copy(alpha = 0.15f),
                focusedTextColor = colorTexto,
                unfocusedTextColor = colorTexto,
                focusedTrailingIconColor = colorTexto,
                unfocusedTrailingIconColor = colorTexto.copy(alpha = 0.6f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colorFondo)
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = translateGameVersion(version),
                            fontWeight = if (version == selectedVersion) FontWeight.Bold else FontWeight.Normal,
                            color = colorTexto
                        )
                    },
                    onClick = {
                        onVersionSelected(version)
                        expanded = false
                    }
                )
            }
        }
    }
}

internal fun getGameReleaseOrder(version: String): Int = when (version.lowercase()) {
    "red", "rojo" -> 1
    "blue", "azul" -> 2
    "yellow", "amarillo" -> 3
    "gold", "oro" -> 4
    "silver", "plata" -> 5
    "crystal", "cristal" -> 6
    "ruby", "rubí", "rubi" -> 7
    "sapphire", "zafiro" -> 8
    "firered", "rojo fuego" -> 9
    "leafgreen", "verde hoja" -> 10
    "emerald", "esmeralda" -> 11
    "diamond", "diamante" -> 12
    "pearl", "perla" -> 13
    "platinum", "platino" -> 14
    "heartgold", "oro heartgold" -> 15
    "soulsilver", "plata soulsilver" -> 16
    "black", "negro" -> 17
    "white", "blanco" -> 18
    "black-2", "negro 2" -> 19
    "white-2", "blanco 2" -> 20
    "x" -> 21
    "y" -> 22
    "omega-ruby", "rubí omega", "rubi omega" -> 23
    "alpha-sapphire", "zafiro alfa" -> 24
    "sun", "sol" -> 25
    "moon", "luna" -> 26
    "ultra-sun", "ultra sol" -> 27
    "ultra-moon", "ultra luna" -> 28
    "lets-go-pikachu", "let's go pikachu" -> 29
    "lets-go-eevee", "let's go eevee" -> 30
    "sword", "espada" -> 31
    "shield", "escudo" -> 32
    "brilliant-diamond", "diamante brillante" -> 33
    "shining-pearl", "perla reluciente" -> 34
    "legends-arceus", "leyendas arceus" -> 35
    "scarlet", "escarlata" -> 36
    "violet", "violeta", "púrpura", "purpura" -> 37
    "legends-za", "leyendas z-a" -> 38
    else -> 999
}

internal fun translateGameVersion(version: String): String = when (version.lowercase()) {
    "red" -> "Rojo"
    "blue" -> "Azul"
    "yellow" -> "Amarillo"
    "gold" -> "Oro"
    "silver" -> "Plata"
    "crystal" -> "Cristal"
    "ruby" -> "Rubi"
    "sapphire" -> "Zafiro"
    "emerald" -> "Esmeralda"
    "firered" -> "Rojo Fuego"
    "leafgreen" -> "Verde Hoja"
    "diamond" -> "Diamante"
    "pearl" -> "Perla"
    "platinum" -> "Platino"
    "heartgold" -> "Oro HeartGold"
    "soulsilver" -> "Plata SoulSilver"
    "black" -> "Negro"
    "white" -> "Blanco"
    "black-2" -> "Negro 2"
    "white-2" -> "Blanco 2"
    "x" -> "X"
    "y" -> "Y"
    "omega-ruby" -> "Rubi Omega"
    "alpha-sapphire" -> "Zafiro Alfa"
    "sun" -> "Sol"
    "moon" -> "Luna"
    "ultra-sun" -> "Ultra Sol"
    "ultra-moon" -> "Ultra Luna"
    "lets-go-pikachu" -> "Let's Go Pikachu"
    "lets-go-eevee" -> "Let's Go Eevee"
    "sword" -> "Espada"
    "shield" -> "Escudo"
    "brilliant-diamond" -> "Diamante Brillante"
    "shining-pearl" -> "Perla Reluciente"
    "legends-arceus" -> "Leyendas Arceus"
    "scarlet" -> "Escarlata"
    "violet" -> "Violeta"
    else -> version.split("-").joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
}

internal fun getGameCoverResId(version: String): Int {
    // Quitar sufijo "japan" si lo tiene (ej: "red japan" → "red")
    val cleaned = version.lowercase().replace(" japan", "").replace("-japan", "").trim()
    return when (cleaned) {
        // Nombres API (inglés)
        "red" -> R.drawable.game_red
        "blue" -> R.drawable.game_blue
        "yellow" -> R.drawable.game_yellow
        "gold" -> R.drawable.game_gold
        "silver" -> R.drawable.game_silver
        "crystal" -> R.drawable.game_crystal
        "ruby" -> R.drawable.game_ruby
        "sapphire" -> R.drawable.game_sapphire
        "emerald" -> R.drawable.game_emerald
        "firered" -> R.drawable.game_red_fire
        "leafgreen" -> R.drawable.game_green_leaf
        "diamond" -> R.drawable.game_diamond
        "pearl" -> R.drawable.game_pearl
        "platinum" -> R.drawable.game_platinum
        "heartgold" -> R.drawable.game_heart_gold
        "soulsilver" -> R.drawable.game_soul_silver
        "black" -> R.drawable.game_black
        "white" -> R.drawable.game_white
        "black-2" -> R.drawable.game_black_2
        "white-2" -> R.drawable.game_white_2
        "x" -> R.drawable.game_x
        "y" -> R.drawable.game_y
        "omega-ruby" -> R.drawable.game_ruby_omega
        "alpha-sapphire" -> R.drawable.game_sapphire_alpha
        "sun" -> R.drawable.game_sun
        "moon" -> R.drawable.game_moon
        "ultra-sun" -> R.drawable.game_ultra_sun
        "ultra-moon" -> R.drawable.game_ultra_moon
        "lets-go-pikachu" -> R.drawable.game_letsgo_pikachu
        "lets-go-eevee" -> R.drawable.game_letsgo_eevee
        "sword" -> R.drawable.game_sword
        "shield" -> R.drawable.game_shield
        "brilliant-diamond" -> R.drawable.game_brilliant_diamond
        "shining-pearl" -> R.drawable.game_shining_pearl
        "legends-arceus" -> R.drawable.game_arceus
        "scarlet" -> R.drawable.game_scarlet
        "violet" -> R.drawable.game_violet
        "legends-za" -> R.drawable.game_za
        // Nombres traducidos (español)
        "rojo" -> R.drawable.game_red
        "azul" -> R.drawable.game_blue
        "amarillo" -> R.drawable.game_yellow
        "oro" -> R.drawable.game_gold
        "plata" -> R.drawable.game_silver
        "cristal" -> R.drawable.game_crystal
        "rubí", "rubi" -> R.drawable.game_ruby
        "zafiro" -> R.drawable.game_sapphire
        "esmeralda" -> R.drawable.game_emerald
        "rojo fuego" -> R.drawable.game_red_fire
        "verde hoja" -> R.drawable.game_green_leaf
        "diamante" -> R.drawable.game_diamond
        "perla" -> R.drawable.game_pearl
        "platino" -> R.drawable.game_platinum
        "oro heartgold" -> R.drawable.game_heart_gold
        "plata soulsilver" -> R.drawable.game_soul_silver
        "negro" -> R.drawable.game_black
        "blanco" -> R.drawable.game_white
        "negro 2" -> R.drawable.game_black_2
        "blanco 2" -> R.drawable.game_white_2
        "rubí omega", "rubi omega" -> R.drawable.game_ruby_omega
        "zafiro alfa" -> R.drawable.game_sapphire_alpha
        "sol" -> R.drawable.game_sun
        "luna" -> R.drawable.game_moon
        "ultra sol" -> R.drawable.game_ultra_sun
        "ultra luna" -> R.drawable.game_ultra_moon
        "let's go pikachu" -> R.drawable.game_letsgo_pikachu
        "let's go eevee" -> R.drawable.game_letsgo_eevee
        "espada" -> R.drawable.game_sword
        "escudo" -> R.drawable.game_shield
        "diamante brillante" -> R.drawable.game_brilliant_diamond
        "perla reluciente" -> R.drawable.game_shining_pearl
        "leyendas arceus" -> R.drawable.game_arceus
        "escarlata" -> R.drawable.game_scarlet
        "púrpura", "purpura", "violeta" -> R.drawable.game_violet
        "leyendas z-a" -> R.drawable.game_za
        else -> 0
    }
}
