package com.david.pokedex_api.ui.screen.ficha.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
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
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorTypeChip
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonEncountersView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.descripcionesMegasGigas

private enum class SectionPage(val label: String, val iconRes: Int) {
    DESC("Descripción", R.drawable.ic_description),
    SPRITES("Sprites", R.drawable.ic_sprites),
    STATS("Stats", R.drawable.ic_stats),
    EVOS("Evolución", R.drawable.ic_evolution),
    SPECIAL_FORMS("Megas / Gigas", R.drawable.ic_mega),
    MOVES("Movimientos", R.drawable.ic_moves),
    ABILITY("Habilidades", R.drawable.ic_ability),
    INTER("Tipos", R.drawable.ic_types),
    FORM("Formas", R.drawable.ic_forms),
    INFO("Info", R.drawable.ic_info),
    ENCOUNTERS("Ubicaciones", R.drawable.ic_location)
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
    encounters: List<com.david.pokedex_api.api.model.GameEncounterGroup> = emptyList(),
    isLoadingEncounters: Boolean = false,
    modifier: Modifier = Modifier
) {
    val typeName = pokemon.types[0].type.name
    val isDark = esTipoColorOscuro(typeName)
    val colorTexto = if (isDark) Color.White else CardBorder

    // 3 variantes del color del tipo para dar variedad visual
    val colorCard = getPokemonTypeColor(typeName)         // medio - para secciones principales
    val colorLight = getPokemonTypeColorTypeChip(typeName) // intenso - para headers/acentos
    val colorSoft = getPokemonTypeColorClear(typeName)     // suave/pastel - para fondos de secciones

    // Color para dropdowns: segundo tipo soft, o colorCard si mono-tipo
    val type2Name = pokemon.types.getOrNull(1)?.type?.name
    val colorDropdown = if (type2Name != null) getPokemonTypeColorClear(type2Name) else colorCard

    // Fondo del contenedor general de secciones
    val colorComponentBg = colorSoft.copy(alpha = 0.25f)

    var selectedSection by rememberSaveable { mutableStateOf(SectionPage.DESC.name) }
    var menuExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val currentSection = SectionPage.valueOf(selectedSection)

    // Filtrar secciones que tienen contenido (sin remember para reaccionar a cambios async)
    val availableSections = SectionPage.entries.filter { section ->
        when (section) {
            SectionPage.DESC -> true
            SectionPage.SPRITES -> true
            SectionPage.STATS -> pokemon.stats.isNotEmpty()
            SectionPage.EVOS -> evolutionChainDetailResponse != null || isLoadingEvolutionChain
            SectionPage.SPECIAL_FORMS -> true
            SectionPage.MOVES -> pokemon.moves.isNotEmpty()
            SectionPage.ABILITY -> pokemon.abilities.isNotEmpty()
            SectionPage.INTER -> pokemon.types.isNotEmpty()
            SectionPage.FORM -> true
            SectionPage.INFO -> pokemonSpecies != null
            SectionPage.ENCOUNTERS -> true // se carga async, mostrar siempre
        }
    }

    Box(modifier = modifier) {
        // Contenido de la seccion activa - sin separacion ni border radius
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorComponentBg)
        ) {
            when (currentSection) {
                SectionPage.DESC -> {
                    val flavorEntries = remember(pokemonSpecies) {
                        val entries = pokemonSpecies?.flavorTextEntries ?: emptyList()
                        val spanishByVersion = entries
                            .filter { it.language.name == "es" && !it.flavorText.isNullOrBlank() }
                            .associateBy { it.version.name }
                        val englishByVersion = entries
                            .filter { it.language.name == "en" && !it.flavorText.isNullOrBlank() }
                            .associateBy { it.version.name }
                        val allVersions = (spanishByVersion.keys + englishByVersion.keys).distinct()
                        allVersions.mapNotNull { version ->
                            val text = spanishByVersion[version]?.flavorText
                                ?: englishByVersion[version]?.flavorText
                                ?: return@mapNotNull null
                            val cleaned = text.replace("\n", " ").replace("\u000c", " ").replace("POKéMON", "Pokémon")
                            version to cleaned
                        }
                    }

                    if (flavorEntries.isEmpty()) {
                        val fallback = descripcionesMegasGigas.find { it.pokeId == pokemon.id }?.desc
                            ?: "No hay descripcion disponible."
                        Box(
                            modifier = Modifier.fillMaxSize().background(colorLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = fallback, style = MaterialTheme.typography.bodyLarge, color = colorTexto, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        var selectedVersion by rememberSaveable { mutableStateOf(flavorEntries.first().first) }
                        val currentText = remember(selectedVersion, flavorEntries) {
                            flavorEntries.find { it.first == selectedVersion }?.second ?: ""
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorLight)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Titulo
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorTexto,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Selector de juego
                            GameVersionSelector(
                                versions = flavorEntries.map { it.first },
                                selectedVersion = selectedVersion,
                                onVersionSelected = { selectedVersion = it },
                                colorFondo = colorDropdown,
                                colorTexto = colorTexto
                            )

                            // Descripcion centrada
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorTexto,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }

                SectionPage.SPRITES -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorLight)
                    ) {
                        LiveSprites(
                            pokemon = pokemon,
                            colorTexto = colorTexto,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                SectionPage.STATS -> {
                    MuestraStatsBase(
                        stats = pokemon.stats,
                        colorFondo = colorLight,
                        colorTexto = colorTexto
                    )
                }

                SectionPage.EVOS -> {
                    if (isLoadingEvolutionChain && evolutionChainDetailResponse == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
                        }
                    } else if (evolutionChainDetailResponse != null) {
                        PokemonEvolutionChainView(
                            evolutionChainResponse = evolutionChainDetailResponse,
                            onPokemonClick = onEvolutionPokemonClick,
                            color1 = colorLight,
                            color2 = colorSoft,
                            colorTexto = colorTexto,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No se pudieron cargar los datos de evolucion.", color = colorTexto)
                        }
                    }
                }

                SectionPage.SPECIAL_FORMS -> {
                    PokemonSpecialFormsView(
                        pokemonSpeciesUrl = pokemon.species.url,
                        pokemonApiService = pokemonApiService,
                        onFormClick = { onEvolutionPokemonClick(it) },
                        cardColor = colorLight,
                        itemCardColor = colorSoft,
                        colorTexto = colorTexto,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.MOVES -> {
                    PokemonMovesList(
                        moves = pokemon.moves,
                        cardBackgroundColor = colorLight,
                        pokemonApiService = pokemonApiService,
                        moveDetailsMap = moveDetailsMap,
                        textColor = colorTexto,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.ABILITY -> {
                    PokemonAbilitiesList(
                        abilities = pokemon.abilities,
                        backgroundColor = colorLight,
                        textColor = colorTexto,
                        pokemonApiService = pokemonApiService,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SectionPage.INTER -> {
                    PokemonTypeInteractionsTable(
                        pokemonTypes = pokemon.types,
                        pokemonApiService = pokemonApiService,
                        tableBackgroundColor = colorLight,
                        textColor = colorTexto
                    )
                }

                SectionPage.FORM -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PokemonRegionalFormsView(
                            pokemonSpeciesUrl = pokemon.species.url,
                            basePokemonName = pokemon.name,
                            pokemonApiService = pokemonApiService,
                            onFormClick = { onEvolutionPokemonClick(it) },
                            cardColor = colorLight,
                            colorTexto = colorTexto,
                            itemCardColor = colorSoft,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                        PokemonFormsView(
                            pokemon = pokemon,
                            pokemonApiService = pokemonApiService,
                            onFormClick = { _, formApiName -> onEvolutionPokemonClick(formApiName) },
                            cardColor = colorLight,
                            itemCardColor = colorSoft,
                            colorTexto = colorTexto,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }

                SectionPage.INFO -> {
                    InfoPokemon(
                        pokemon = pokemon,
                        species = pokemonSpecies,
                        colorFondo = colorLight,
                        colorTexto = colorTexto
                    )
                }

                SectionPage.ENCOUNTERS -> {
                    PokemonEncountersView(
                        encounters = encounters,
                        isLoading = isLoadingEncounters,
                        colorFondo = colorLight,
                        colorTexto = colorTexto,
                        colorDropdown = colorDropdown
                    )
                }
            }
        }

        // Menu a pantalla completa con pills distribuidas
        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val pillTextColor = if (isDark) Color.White else Color.Black
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { menuExpanded = false },
                contentAlignment = Alignment.Center
            ) {
                // Grid de pills distribuido equitativamente
                val columns = 3
                val rows = availableSections.chunked(columns)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    rows.forEach { rowSections ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowSections.forEach { section ->
                                val isSelected = section.name == selectedSection
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) colorLight
                                            else colorCard.copy(alpha = 0.9f)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedSection = section.name
                                            menuExpanded = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = section.iconRes),
                                        contentDescription = section.label,
                                        modifier = Modifier.size(26.dp),
                                        tint = pillTextColor
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = section.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = pillTextColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                            // Rellenar celdas vacías en la última fila
                            repeat(columns - rowSections.size) {
                                Spacer(Modifier.weight(1f).padding(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // FAB - mismo estilo que el de busqueda de las listas
        if (!menuExpanded) {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuExpanded = true
                },
                containerColor = color_boton_busqueda.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(65.dp)
                    .clip(RoundedCornerShape(50.dp))
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(
                        id = SectionPage.valueOf(selectedSection).iconRes
                    ),
                    contentDescription = "Menu secciones",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
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

private fun translateGameVersion(version: String): String = when (version.lowercase()) {
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
