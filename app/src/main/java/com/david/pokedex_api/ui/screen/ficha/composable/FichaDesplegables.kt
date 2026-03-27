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
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorDark
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorSurface
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorTypeChip
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonEncountersView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.descripcionesMegasGigas

enum class SectionPage(val label: String, val iconRes: Int) {
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
    wikiDexFlavorTexts: Map<String, String> = emptyMap(),
    wikiDexLocations: Map<String, String> = emptyMap(),
    encounters: List<com.david.pokedex_api.api.model.GameEncounterGroup> = emptyList(),
    isLoadingEncounters: Boolean = false,
    selectedSection: String = SectionPage.DESC.name,
    modifier: Modifier = Modifier
) {
    val typeName = pokemon.types[0].type.name
    val colorTexto = CardBorder

    // 5 variantes del color del tipo
    val colorDark = getPokemonTypeColorDark(typeName)          // profundo - cabeceras, hero
    val colorBase = getPokemonTypeColor(typeName)              // medio - secciones principales
    val colorSoft = getPokemonTypeColorClear(typeName)         // pastel - fondos de secciones
    val colorAccent = getPokemonTypeColorTypeChip(typeName)    // intenso - chips, badges, acentos
    val colorSurface = getPokemonTypeColorSurface(typeName)    // tinte sutil - cards internas, listas

    // Color para dropdowns: segundo tipo surface, o colorBase si mono-tipo
    val type2Name = pokemon.types.getOrNull(1)?.type?.name
    val colorDropdown = if (type2Name != null) getPokemonTypeColorSurface(type2Name) else colorSurface

    // Fondo del contenedor general de secciones
    val colorComponentBg = colorSurface

    val currentSection = try { SectionPage.valueOf(selectedSection) } catch (_: Exception) { SectionPage.DESC }

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
                    val flavorEntries = remember(pokemonSpecies, wikiDexFlavorTexts) {
                        val entries = pokemonSpecies?.flavorTextEntries ?: emptyList()
                        val spanishByVersion = entries
                            .filter { it.language.name == "es" && !it.flavorText.isNullOrBlank() }
                            .associateBy { it.version.name }
                        val englishByVersion = entries
                            .filter { it.language.name == "en" && !it.flavorText.isNullOrBlank() }
                            .associateBy { it.version.name }
                        // Merge 3 fuentes: PokeAPI ES + WikiDex ES + PokeAPI EN
                        val allVersions = (spanishByVersion.keys + wikiDexFlavorTexts.keys + englishByVersion.keys).distinct()
                        allVersions.mapNotNull { version ->
                            val text = spanishByVersion[version]?.flavorText
                                ?: wikiDexFlavorTexts[version]
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
                            modifier = Modifier.fillMaxSize().background(colorSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = fallback, style = MaterialTheme.typography.bodyLarge, color = colorTexto, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colorSoft)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorTexto,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Todas las descripciones con scroll vertical
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                flavorEntries.forEach { (version, text) ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                colorDropdown.copy(alpha = 0.5f),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = translateGameVersion(version),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorTexto.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = text,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            color = colorTexto
                                        )
                                    }
                                }
                                Spacer(Modifier.height(60.dp))
                            }
                        }
                    }
                }

                SectionPage.SPRITES -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorSoft)
                    ) {
                        LiveSprites(
                            pokemon = pokemon,
                            colorTexto = colorTexto,
                            nombreSpanish = pokemon.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                SectionPage.STATS -> {
                    MuestraStatsBase(
                        stats = pokemon.stats,
                        colorFondo = colorSoft,
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
                            color1 = colorSoft,
                            color2 = colorSurface,
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
                        cardColor = colorSoft,
                        itemCardColor = colorSurface,
                        colorTexto = colorTexto,
                        modifier = Modifier.fillMaxSize()
                    )
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
                        modifier = Modifier.fillMaxSize().padding(8.dp),
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
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                        PokemonFormsView(
                            pokemon = pokemon,
                            pokemonApiService = pokemonApiService,
                            onFormClick = { _, formApiName -> onEvolutionPokemonClick(formApiName) },
                            cardColor = colorSoft,
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
                        colorDropdown = colorDropdown
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
