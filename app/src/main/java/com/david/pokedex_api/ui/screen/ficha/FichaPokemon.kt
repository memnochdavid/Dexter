package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorDark
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeGradientColors
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.screen.ficha.composable.DetallesDesplegables
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.NombreNumAlturaPeso
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.dinamaxLiveSprites
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.adaptaNombre
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.transformPokemonNameToResourceName
import com.david.pokedex_api.LocalAnimatedVisibilityScope
import com.david.pokedex_api.LocalSharedTransitionScope
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.background_app_gradient
import com.david.pokedex_api.ui.theme.rojo_pokeball
import com.david.pokedex_api.ui.screen.ficha.composable.background.TypeAnimatedOverlay
import com.david.pokedex_api.util.AnimatedPokeball
import com.david.pokedex_api.util.ShinySparkleEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Filtra la cadena expandida para mostrar solo la línea relevante al pokémon actual.
 * - Si el pokémon tiene sufijo regional → extrae la rama regional de la raíz.
 * - Si no → intenta la cadena base; si el pokémon no está en ella (exclusivo de
 *   región, ej. Obstagoon), busca en qué rama regional se encuentra.
 */
private fun romanToInt(roman: String): Int? {
    val values = mapOf('i' to 1, 'v' to 5, 'x' to 10)
    val s = roman.lowercase()
    var result = 0
    for (i in s.indices) {
        val curr = values[s[i]] ?: return null
        val next = if (i + 1 < s.length) values[s[i + 1]] ?: 0 else 0
        result += if (curr < next) -curr else curr
    }
    return if (result > 0) result else null
}

private fun filterChainForPokemon(
    expanded: com.david.pokedex_api.api.model.ChainLink,
    currentPokemonName: String
): com.david.pokedex_api.api.model.ChainLink {
    val regionSuffixes = listOf("-alola", "-galar", "-hisui", "-paldea")
    val currentRegion = regionSuffixes.firstOrNull { currentPokemonName.contains(it) }
    val rootSpeciesName = expanded.species.name
    val regionalRootNames = regionSuffixes.map { rootSpeciesName + it }.toSet()

    // Busca recursivamente si un nombre de especie está en el subárbol
    fun chainContains(link: com.david.pokedex_api.api.model.ChainLink, name: String): Boolean {
        if (link.species.name == name) return true
        return link.evolvesTo.any { chainContains(it, name) }
    }

    if (currentRegion != null) {
        // Forma regional explícita → extraer rama de la raíz regional
        return expanded.evolvesTo.firstOrNull { child ->
            child.species.name == rootSpeciesName + currentRegion
        } ?: expanded
    }

    // Forma sin sufijo → probar cadena base primero
    val baseChain = expanded.copy(
        evolvesTo = expanded.evolvesTo.filter { it.species.name !in regionalRootNames }
    )
    if (chainContains(baseChain, currentPokemonName)) {
        return baseChain
    }

    // No está en la cadena base → buscar en ramas regionales (ej. Obstagoon en Galar)
    for (child in expanded.evolvesTo) {
        if (child.species.name in regionalRootNames && chainContains(child, currentPokemonName)) {
            return child
        }
    }

    // Fallback: cadena base
    return baseChain
}

// Nueva pantalla para los detalles del Pokémon, para manejar la carga y la UI de detalles.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PokemonDetailScreen(
    pokemonViewModel: PokemonViewModel,
    pokemonName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val pokemonDetail by pokemonViewModel.pokemonDetails.observeAsState()
    val pokemonDescription by pokemonViewModel.pokemonDescription.observeAsState()
    val isLoadingDetails by pokemonViewModel.isLoadingDetails.observeAsState(false)
    val error by pokemonViewModel.error.observeAsState()
    val evolutionChain by pokemonViewModel.evolutionChainDetails.observeAsState()
    val isLoadingEvolutionChain by pokemonViewModel.isLoadingEvolutionChain.observeAsState(false)
    val pokemonSpecies by pokemonViewModel.pokemonSpeciesDetails.observeAsState()
    val moveDetailsMap by pokemonViewModel.moveDetailsMap.collectAsState()
    val encounters by pokemonViewModel.pokemonEncounters.collectAsState()
    val isLoadingEncounters by pokemonViewModel.isLoadingEncounters.collectAsState()
    val wikiDexFlavorTexts by pokemonViewModel.wikiDexFlavorTexts.observeAsState(emptyMap())
    val wikiDexLocations by pokemonViewModel.wikiDexLocations.observeAsState(emptyMap())

    // Cargar detalles del Pokemon
    LaunchedEffect(pokemonName) {
        pokemonViewModel.fetchPokemonDetailsByName(pokemonName, "es")
    }

    // Cargar encuentros cuando tengamos el ID del pokemon
    LaunchedEffect(pokemonDetail?.id) {
        pokemonDetail?.id?.let { pokemonViewModel.fetchPokemonEncounters(it) }
    }

    // Cuando llega la cadena evolutiva, expandir con formas regionales y pre-cargar.
    // Se cachea la expansión para no repetir llamadas de red al cambiar pokemonDetail.
    var rawExpandedChain by remember { mutableStateOf<com.david.pokedex_api.api.model.ChainLink?>(null) }
    var expandedAllIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    // Referencia al chain que ya fue expandido, para detectar si cambió
    var lastExpandedEvolutionChain by remember { mutableStateOf<com.david.pokedex_api.api.model.EvolutionChainDetailResponse?>(null) }

    LaunchedEffect(evolutionChain, pokemonDetail?.id) {
        evolutionChain?.chain?.let { chain ->
            // Expandir solo si la cadena cambió (evitar re-llamadas de red costosas)
            if (evolutionChain != lastExpandedEvolutionChain) {
                val (expanded, allIds) = pokemonViewModel.expandChainWithRegionals(chain)
                rawExpandedChain = expanded
                expandedAllIds = allIds
                lastExpandedEvolutionChain = evolutionChain
            }

            val expanded = rawExpandedChain ?: return@let
            val regionSuffixes = listOf("-alola", "-galar", "-hisui", "-paldea")
            val sInfoMap = pokemonViewModel.speciesInfoMap.value

            // Extraer líneas evolutivas completas del expanded chain
            // grid[línea][posición] — cada línea es una cadena evolutiva horizontal
            fun extractLineIds(link: com.david.pokedex_api.api.model.ChainLink): List<Int> {
                val id = link.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull() ?: return emptyList()
                val rest = if (link.evolvesTo.size == 1) extractLineIds(link.evolvesTo[0])
                    else link.evolvesTo.flatMap { extractLineIds(it) }
                return listOf(id) + rest
            }

            val grid = mutableListOf<List<Int>>()
            val baseLineIndex: Int // índice de la línea base

            // Separar hijos del root: base vs regionales
            val baseChildren = expanded.evolvesTo.filter { child ->
                !regionSuffixes.any { s -> child.species.name.contains(s) }
            }
            val regionalRoots = expanded.evolvesTo.filter { child ->
                regionSuffixes.any { s -> child.species.name.contains(s) }
            }

            // Línea base
            val baseChain = expanded.copy(evolvesTo = baseChildren)
            val baseLineIds = extractLineIds(baseChain)

            // Líneas mega/gmax: una por cada mega/gmax de CUALQUIER especie en la cadena
            val megaGmaxLines = baseLineIds.flatMap { speciesId ->
                val info = sInfoMap[speciesId]
                info?.varieties
                    ?.filter { !it.isDefault && (it.pokemon.name.contains("-mega") || it.pokemon.name.contains("-gmax")) }
                    ?.mapNotNull { v -> v.pokemon.url.trimEnd('/').substringAfterLast('/').toIntOrNull()?.let { listOf(it) } }
                    ?: emptyList()
            }

            // Agregar: megas arriba, línea base, líneas regionales abajo
            grid.addAll(megaGmaxLines)
            baseLineIndex = grid.size
            grid.add(baseLineIds)
            regionalRoots.forEach { grid.add(extractLineIds(it)) }

            // Asegurar que el pokémon actual esté en el grid
            val currentId = pokemonDetail?.id
            if (currentId != null && grid.none { it.contains(currentId) }) {
                grid.add(listOf(currentId))
            }

            // Precargar todos los IDs del grid + IDs extra para display de evolución
            val allGridIds = grid.flatten()
            val finalIds = allGridIds.toMutableList()
            expandedAllIds.forEach { id -> if (!finalIds.contains(id)) finalIds.add(id) }

            pokemonViewModel.setNavigationGrid(grid, listOf(baseLineIndex))
            pokemonViewModel.preloadEvolutionChain(finalIds)
        }
    }

    // Chain filtrado para mostrar: derivado del raw + pokémon actual (sin llamadas de red)
    val expandedChain = remember(rawExpandedChain, pokemonDetail?.name) {
        val expanded = rawExpandedChain ?: return@remember null
        filterChainForPokemon(expanded, pokemonDetail?.name ?: "")
    }

    // Al salir de la ficha, volver al estado inicial limpio
    DisposableEffect(Unit) {
        onDispose {
            pokemonViewModel.resetDetailState()
        }
    }

    // Mostrar Toast de error si ocurre y es relevante para la pantalla de detalles
    LaunchedEffect(error) {
        error?.let {
            // Solo muestra el Toast si el error NO es claramente de la lista
            // y si estamos en la pantalla de detalles (implícito por estar aquí)
            if (!it.contains("list", ignoreCase = true)) {
                Log.e("PokemonDetailScreen", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                pokemonViewModel.clearError() // Limpia el error después de mostrarlo
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val navGrid by pokemonViewModel.navigationGrid.observeAsState(emptyList())
            val baseFormIndices by pokemonViewModel.baseFormIndices.observeAsState(emptyList())
            val evoMap by pokemonViewModel.evoChainPokemonMap.collectAsState()

            val allGridIds = remember(navGrid) { navGrid.flatten() }
            val allPreloaded = navGrid.isNotEmpty() && allGridIds.all { evoMap.containsKey(it) }

            // Chain expandido con ramas regionales (para la visualización de línea evolutiva)
            val displayChain = remember(expandedChain, evolutionChain) {
                if (expandedChain != null) {
                    com.david.pokedex_api.api.model.EvolutionChainDetailResponse(
                        id = evolutionChain?.id ?: 0,
                        babyTriggerItem = evolutionChain?.babyTriggerItem,
                        chain = expandedChain!!
                    )
                } else evolutionChain
            }

            if (pokemonDetail == null) {
                val sharedTransitionScope = LocalSharedTransitionScope.current
                val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background_app_gradient)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                    ) {
                        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                            with(sharedTransitionScope) {
                                AnimatedPokeball(
                                    isOpen = false,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 8.dp, bottom = 8.dp)
                                        .size(45.dp)
                                        .sharedElement(
                                            rememberSharedContentState(key = "pokeball-transit-$pokemonName"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxWidth().weight(0.65f))
                }
            } else if (allPreloaded && allGridIds.contains(pokemonDetail!!.id)) {
                // Pager 2D: vertical = línea evolutiva (megas↑ base regionales↓), horizontal = evolución dentro de la línea
                // grid[línea][posición]: cada línea es una cadena evolutiva completa
                val currentId = pokemonDetail!!.id
                val initialVLine = navGrid.indexOfFirst { line -> line.contains(currentId) }.coerceAtLeast(0)
                val initialHPos = navGrid.getOrNull(initialVLine)?.indexOf(currentId)?.coerceAtLeast(0) ?: 0
                val baseLineIdx = baseFormIndices.firstOrNull() ?: 0

                val vPagerState = rememberPagerState(
                    initialPage = initialVLine,
                    pageCount = { navGrid.size }
                )
                val pagerCoroutineScope = rememberCoroutineScope()

                // Posición horizontal activa (se sincroniza entre líneas al hacer swipe vertical)
                var activeHorizontalPos by remember { mutableIntStateOf(initialHPos) }
                // Navegación pendiente (para clicks que cruzan línea+posición)
                var pendingTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (línea, posición)

                // Pre-cachear imágenes para swipe fluido
                val imageLoader = remember { coil.ImageLoader(context) }
                LaunchedEffect(allPreloaded) {
                    allGridIds.forEach { id ->
                        evoMap[id]?.detail?.sprites?.other?.officialArtwork?.frontDefault?.let { url ->
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .size(coil.size.Size.ORIGINAL)
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }

                // Callback compartido para clicks en evolución/formas
                val onEvoPokemonClick: (String) -> Unit = { clickedName ->
                    val targetId = clickedName.toIntOrNull()
                        ?: evoMap.entries.firstOrNull { it.value.detail.name == clickedName }?.key
                        ?: evoMap.entries.firstOrNull { it.value.detail.species.name == clickedName }?.key

                    if (targetId != null) {
                        val targetLine = navGrid.indexOfFirst { it.contains(targetId) }
                        if (targetLine >= 0) {
                            val targetPos = navGrid[targetLine].indexOf(targetId)
                            pendingTarget = Pair(targetLine, targetPos)
                            pagerCoroutineScope.launch {
                                vPagerState.animateScrollToPage(targetLine)
                            }
                        } else {
                            pokemonViewModel.fetchPokemonDetailsByName(clickedName, "es")
                        }
                    } else {
                        pokemonViewModel.fetchPokemonDetailsByName(clickedName, "es")
                    }
                }

                VerticalPager(
                    state = vPagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { vLine ->
                    val line = navGrid.getOrNull(vLine) ?: return@VerticalPager
                    val initialHForLine = if (vLine == initialVLine) initialHPos
                        else activeHorizontalPos.coerceIn(0, (line.size - 1).coerceAtLeast(0))

                    val hPagerState = rememberPagerState(
                        initialPage = initialHForLine,
                        pageCount = { line.size }
                    )

                    // Sincronizar ViewModel y posición horizontal activa
                    LaunchedEffect(hPagerState) {
                        snapshotFlow { hPagerState.settledPage }
                            .distinctUntilChanged()
                            .collect { hPos ->
                                if (vLine == vPagerState.settledPage) {
                                    activeHorizontalPos = hPos
                                    val id = line.getOrNull(hPos) ?: return@collect
                                    pokemonViewModel.switchToPreloadedPokemon(id)
                                }
                            }
                    }

                    // Sincronizar posición horizontal al llegar a esta línea por swipe vertical
                    LaunchedEffect(vPagerState.settledPage) {
                        if (vPagerState.settledPage == vLine) {
                            val target = pendingTarget
                            if (target != null && target.first == vLine) {
                                hPagerState.animateScrollToPage(target.second)
                                pendingTarget = null
                            } else {
                                val targetPos = activeHorizontalPos.coerceIn(0, (line.size - 1).coerceAtLeast(0))
                                if (hPagerState.currentPage != targetPos) {
                                    hPagerState.scrollToPage(targetPos)
                                }
                            }
                        }
                    }

                    // Atender target pendiente
                    LaunchedEffect(pendingTarget) {
                        val target = pendingTarget ?: return@LaunchedEffect
                        if (target.first == vLine && vPagerState.settledPage == vLine) {
                            hPagerState.animateScrollToPage(target.second)
                            pendingTarget = null
                        }
                    }

                    HorizontalPager(
                        state = hPagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { hPos ->
                        val pokemonId = line.getOrNull(hPos) ?: return@HorizontalPager
                        val preloaded = evoMap[pokemonId]
                        if (preloaded != null) {
                            PokemonDetailsView(
                                pokemon = preloaded.detail,
                                pokemonSpecies = preloaded.species,
                                description = null,
                                evolutionChainDetailResponse = displayChain,
                                isLoadingEvolutionChain = isLoadingEvolutionChain,
                                onEvolutionPokemonClick = onEvoPokemonClick,
                                pokemonViewModel = pokemonViewModel,
                                moveDetailsMap = moveDetailsMap,
                                wikiDexFlavorTexts = wikiDexFlavorTexts,
                                wikiDexLocations = wikiDexLocations,
                                encounters = encounters,
                                isLoadingEncounters = isLoadingEncounters,
                                isActivePage = vLine == vPagerState.settledPage && hPos == hPagerState.settledPage,
                                selectedSection = pokemonViewModel.selectedDetailSection.collectAsState().value,
                                onNavigateBack = onNavigateBack,
                                shouldAnimate = true
                            )
                        }
                    }
                }
            } else {
                // Vista simple: transitoria (cadena aun cargando) o definitiva (pokemon sin pager)
                val chainLoaded = evolutionChain != null && !isLoadingEvolutionChain
                val pagerWillTakeOver = !chainLoaded || (navGrid.isNotEmpty() && allGridIds.contains(pokemonDetail!!.id))
                PokemonDetailsView(
                    pokemon = pokemonDetail!!,
                    pokemonSpecies = pokemonSpecies,
                    description = pokemonDescription,
                    evolutionChainDetailResponse = displayChain,
                    isLoadingEvolutionChain = isLoadingEvolutionChain,
                    onEvolutionPokemonClick = { pokemonNameClicked ->
                        pokemonViewModel.fetchPokemonDetailsByName(pokemonNameClicked, "es")
                    },
                    pokemonViewModel = pokemonViewModel,
                    moveDetailsMap = moveDetailsMap,
                    wikiDexFlavorTexts = wikiDexFlavorTexts,
                    wikiDexLocations = wikiDexLocations,
                    encounters = encounters,
                    isLoadingEncounters = isLoadingEncounters,
                    selectedSection = pokemonViewModel.selectedDetailSection.collectAsState().value,
                    onNavigateBack = onNavigateBack,
                    shouldAnimate = !pagerWillTakeOver
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("DefaultLocale")
@Composable
fun PokemonDetailsView(
    pokemon: PokemonDetailResponse,
    pokemonSpecies: PokemonSpeciesResponse?,
    description: String?,
    evolutionChainDetailResponse: EvolutionChainDetailResponse?,
    isLoadingEvolutionChain: Boolean,
    onEvolutionPokemonClick: (pokemonName: String) -> Unit,
    pokemonViewModel: PokemonViewModel,
    moveDetailsMap: Map<String, MoveDetailResponse> = emptyMap(),
    wikiDexFlavorTexts: Map<String, String> = emptyMap(),
    wikiDexLocations: Map<String, String> = emptyMap(),
    encounters: List<com.david.pokedex_api.api.model.GameEncounterGroup> = emptyList(),
    isLoadingEncounters: Boolean = false,
    isActivePage: Boolean = true,
    selectedSection: String = "DESC",
    onNavigateBack: () -> Unit = {},
    shouldAnimate: Boolean = true
) {
    val spanishGenus = remember(pokemonSpecies) {
        pokemonSpecies?.genera?.find { it.language.name == "es" }?.genus
    }

    val spanishPokemonName = remember(pokemonSpecies, pokemon) {
        pokemonSpecies?.localizedNames?.find { it.language.name == "es" }?.name
            ?: pokemonSpecies?.name
            ?: pokemon.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }

    val regionTag = remember(pokemon.name) {
        mapOf(
            "-alola" to "Alola", "-galar" to "Galar",
            "-hisui" to "Hisui", "-paldea" to "Paldea"
        ).entries.firstOrNull { pokemon.name.contains(it.key) }?.value
    }

    // Número de especie (para megas, gigamax y formas especiales)
    val speciesId = remember(pokemon.species.url) {
        pokemon.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
    }
    val isRegional = regionTag != null
    val displayNumber = remember(pokemon.id, speciesId, isRegional) {
        if (!isRegional && speciesId != null && speciesId != pokemon.id) speciesId
        else pokemon.id
    }

    // Nombre de la forma (si tiene)
    val formDisplayName = remember(pokemon.name, spanishPokemonName) {
        val apiName = pokemon.name.lowercase()
        // Extraer el sufijo de forma del nombre API
        val regionalSuffixes = listOf("-alola", "-galar", "-hisui", "-paldea")
        val isRegionalForm = regionalSuffixes.any { apiName.contains(it) }
        if (isRegionalForm) {
            null // Las regionales ya tienen su regionTag
        } else if (apiName.contains("-")) {
            // Convertir nombre recurso → nombre display de la forma
            val resourceName = transformPokemonNameToResourceName(apiName)
            val baseName = transformPokemonNameToResourceName(
                apiName.substringBefore("-")
            )
            if (resourceName == baseName) {
                null // Forma por defecto (confined, normal, etc.)
            } else {
                // Extraer la parte de forma del nombre recurso
                val formPart = resourceName.removePrefix("${baseName}_")
                    .also { if (it == resourceName) return@remember null } // no se pudo extraer
                // Mega tiene formato especial: mega_X → Mega X
                if (resourceName.startsWith("mega_")) {
                    val megaSuffix = resourceName.removePrefix("mega_${baseName.removePrefix("mega_")}")
                        .removePrefix("_").uppercase().takeIf { it.isNotEmpty() }
                    if (megaSuffix != null) "Mega $megaSuffix" else "Mega"
                } else if (resourceName.startsWith("ultra_")) {
                    "Ultra"
                } else {
                    // Capitalizar cada parte: "alas_del_alba" → "Alas Del Alba"
                    adaptaNombre(formPart)
                }
            }
        } else {
            null
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Estado de swap de forma (para formas locales sin navegación)
    var overrideResourceName by remember { mutableStateOf<String?>(null) }
    var overrideFormDisplayName by remember { mutableStateOf<String?>(null) }
    var overrideTypes by remember { mutableStateOf<List<String>?>(null) }

    val type1 = overrideTypes?.firstOrNull() ?: pokemon.types[0].type.name

    // Estado de género (dimorfismo sexual)
    var isFemale by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val webpResourceName = remember(pokemon.name) {
        transformPokemonNameToResourceName(pokemon.name.lowercase())
    }
    val femaleResourceId = remember(webpResourceName) {
        context.resources.getIdentifier("${webpResourceName}_hembra", "raw", context.packageName)
    }
    val hasFemaleSprite = remember(femaleResourceId, pokemon.sprites.frontFemale) {
        femaleResourceId != 0 || pokemon.sprites.frontFemale != null
    }

    // Mapa pre-cargado de la cadena evolutiva (tipos, sprites, etc.)
    val evoChainMap by pokemonViewModel.evoChainPokemonMap.collectAsState()

    // Estado de imagen expandida
    var isImageExpanded by remember { mutableStateOf(false) }

    // Peso animado: imagen ocupa 0.35f normal, 1f expandida
    val imageWeight by animateFloatAsState(
        targetValue = if (isImageExpanded) 1f else 0f,
        animationSpec = tween(450),
        label = "imageWeight"
    )
    // Opacidad de las secciones inferiores
    val bottomAlpha by animateFloatAsState(
        targetValue = if (isImageExpanded) 0f else 1f,
        animationSpec = tween(350),
        label = "bottomAlpha"
    )

    // Contenido reutilizable: nombre + desplegables
    @Composable
    fun BottomContent(modifier: Modifier) {
        Column(modifier = modifier) {
            NombreNumAlturaPeso(
                colorFondo = getPokemonTypeColorDark(type1),
                colorTexto = Color.White,
                nombre = spanishPokemonName,
                numero = displayNumber,
                genus = spanishGenus,
                formName = overrideFormDisplayName ?: formDisplayName,
                altura = pokemon.height.toDouble(),
                peso = pokemon.weight.toDouble(),
                modifier = Modifier.fillMaxWidth(),
                tipo = pokemon.types[0].type.name,
                cryUrl = pokemon.cries?.latest,
                generationNum = mapOf("-alola" to 7, "-galar" to 8, "-hisui" to 8, "-paldea" to 9)
                    .entries.firstOrNull { pokemon.name.contains(it.key) }?.value
                    ?: pokemonSpecies?.generation?.name
                        ?.removePrefix("generation-")
                        ?.let { romanToInt(it) },
                regionTag = regionTag,
                isLegendary = pokemonSpecies?.isLegendary == true,
                isMythical = pokemonSpecies?.isMythical == true,
                isMega = pokemon.name.contains("-mega"),
                isGigamax = pokemon.name.contains("-gmax"),
                hasFemale = hasFemaleSprite,
                isFemale = isFemale,
                onToggleGender = { female -> isFemale = female }
            )

            DetallesDesplegables(
                pokemon = pokemon,
                evolutionChainDetailResponse = evolutionChainDetailResponse,
                isLoadingEvolutionChain = isLoadingEvolutionChain,
                onEvolutionPokemonClick = onEvolutionPokemonClick,
                description = description,
                pokemonApiService = pokemonViewModel.pokemonApiService,
                moveDetailsMap = moveDetailsMap,
                pokemonSpecies = pokemonSpecies,
                wikiDexFlavorTexts = wikiDexFlavorTexts,
                wikiDexLocations = wikiDexLocations,
                encounters = encounters,
                isLoadingEncounters = isLoadingEncounters,
                selectedSection = selectedSection,
                onSectionSelected = { pokemonViewModel.selectedDetailSection.value = it },
                onAvailableSectionsChanged = { pokemonViewModel.availableDetailSections.value = it },
                evoChainMap = evoChainMap,
                onFormSwap = { resName, displayName, types ->
                    overrideResourceName = resName
                    overrideFormDisplayName = displayName
                    overrideTypes = types
                },
                spanishPokemonName = spanishPokemonName,
                fetchFormSprites = { name -> pokemonViewModel.getWikiDexFormSprites(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (isLandscape) {
        // Landscape: imagen a la izquierda, contenido a la derecha
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app_gradient)
        ) {
            ComponenteImagen(
                pokemon = pokemon,
                isActivePage = isActivePage,
                pokemonViewModel = pokemonViewModel,
                isExpanded = isImageExpanded,
                onToggleExpand = { isImageExpanded = !isImageExpanded },
                onNavigateBack = onNavigateBack,
                shouldAnimate = shouldAnimate,
                nombreSpanish = spanishPokemonName,
                isFemale = isFemale,
                overrideResourceName = overrideResourceName,
                overrideTypes = overrideTypes,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f + imageWeight * 0.65f)
            )

            if (imageWeight < 0.99f) {
                BottomContent(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.65f - imageWeight * 0.65f)
                        .graphicsLayer { alpha = bottomAlpha }
                )
            }
        }
    } else {
        // Portrait: imagen arriba, contenido abajo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app_gradient)
        ) {
            ComponenteImagen(
                pokemon = pokemon,
                isActivePage = isActivePage,
                pokemonViewModel = pokemonViewModel,
                isExpanded = isImageExpanded,
                onToggleExpand = { isImageExpanded = !isImageExpanded },
                onNavigateBack = onNavigateBack,
                shouldAnimate = shouldAnimate,
                nombreSpanish = spanishPokemonName,
                isFemale = isFemale,
                overrideResourceName = overrideResourceName,
                overrideTypes = overrideTypes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f + imageWeight * 0.65f)
            )

            if (imageWeight < 0.99f) {
                BottomContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.65f - imageWeight * 0.65f)
                        .graphicsLayer { alpha = bottomAlpha }
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ComponenteImagen(
    pokemon: PokemonDetailResponse,
    isActivePage: Boolean = true,
    pokemonViewModel: PokemonViewModel? = null,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    shouldAnimate: Boolean = true,
    nombreSpanish: String = "",
    isFemale: Boolean = false,
    overrideResourceName: String? = null,
    overrideTypes: List<String>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Shiny toggle + sparkle trigger
    var isShiny by remember { mutableStateOf(false) }
    var shinySparkleKey by remember { mutableStateOf(0) } // incrementa para re-disparar

    // Animated WebP resources
    val webpResourceName = remember(pokemon.name, overrideResourceName) {
        overrideResourceName ?: transformPokemonNameToResourceName(pokemon.name.lowercase())
    }
    val femaleResourceName = remember(webpResourceName) { "${webpResourceName}_hembra" }
    val femaleResourceId = remember(femaleResourceName) {
        context.resources.getIdentifier(femaleResourceName, "raw", context.packageName)
    }
    val femaleShinyResourceName = remember(webpResourceName) { "${webpResourceName}_hembra_shiny" }
    val femaleShinyResourceId = remember(femaleShinyResourceName) {
        context.resources.getIdentifier(femaleShinyResourceName, "raw", context.packageName)
    }

    val baseWebpResourceId = remember(webpResourceName) {
        context.resources.getIdentifier(webpResourceName, "raw", context.packageName)
    }
    // Seleccionar recurso webp según género
    val webpResourceId = remember(baseWebpResourceId, femaleResourceId, isFemale) {
        if (isFemale && femaleResourceId != 0) femaleResourceId else baseWebpResourceId
    }
    val shinyResourceName = remember(webpResourceName, isFemale) {
        if (isFemale && femaleShinyResourceId != 0) "${webpResourceName}_hembra_shiny"
        else "${webpResourceName}_shiny"
    }
    val shinyResourceId = remember(shinyResourceName) {
        context.resources.getIdentifier(shinyResourceName, "raw", context.packageName)
    }
    val animatedImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
    }

    // Gigamax GIF fallback
    val gigamaxSpriteUrl = remember(pokemon.id) {
        dinamaxLiveSprites.find { it.pokeId == pokemon.id }?.spriteUrl
    }

    // --- Pokeball state ---
    var isRecalling by remember { mutableStateOf(false) }

    // --- Animacion de escala del Pokemon ---
    val canAnimate = shouldAnimate && isActivePage
    val alreadyAnimated = pokemonViewModel?.animatedPokemonIds?.contains(pokemon.id) == true
    // Solo mostrar inmediatamente si ya se animo antes (swipe en pager).
    // Si no se animo y no toca animar (vista simple pre-pager), queda oculto (0f).
    var internalScaleTarget by remember {
        mutableStateOf(if (alreadyAnimated) 1f else 0f)
    }
    LaunchedEffect(pokemon.id, isActivePage, shouldAnimate) {
        if (canAnimate && pokemonViewModel?.animatedPokemonIds?.contains(pokemon.id) != true) {
            internalScaleTarget = 0f
            delay(50)
            internalScaleTarget = 1f
            pokemonViewModel?.animatedPokemonIds?.add(pokemon.id)
        } else if (alreadyAnimated) {
            internalScaleTarget = 1f
        }
    }

    val actualScale by animateFloatAsState(
        targetValue = internalScaleTarget,
        animationSpec = if (isRecalling) tween(300) else keyframes { durationMillis = 500; 0f at 0; 1f at 500 },
        label = "PokemonImageAppearScale"
    )
    val showShimmerEffect = actualScale < 0.95f && actualScale > 0.01f

    // Recall: cuando el pokemon se encoge, navegar atras (una sola vez)
    LaunchedEffect(isRecalling) {
        if (isRecalling) {
            snapshotFlow { actualScale }.first { it < 0.05f }
            delay(80)
            onNavigateBack()
        }
    }

    val type1Name = overrideTypes?.firstOrNull() ?: pokemon.types.getOrNull(0)?.type?.name
    val type2Name = if (overrideTypes != null) overrideTypes.getOrNull(1) else pokemon.types.getOrNull(1)?.type?.name

    // Gradientes por tipo, oscurecidos para la ficha
    fun Color.darken(factor: Float = 0.75f): Color =
        Color(red * factor, green * factor, blue * factor, alpha)

    val gradient1 = type1Name?.let { getPokemonTypeGradientColors(it) }
        ?: (Color.Gray to Color.DarkGray)
    val gradient2 = type2Name?.let { getPokemonTypeGradientColors(it) }
    val isDualType = gradient2 != null

    val g1 = gradient1.first.darken() to gradient1.second.darken()
    val g2 = (gradient2?.first?.darken() ?: g1.first) to (gradient2?.second?.darken() ?: g1.second)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()

        // --- Fondo de tipo ---
        if (isDualType) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(g1.first, g1.second))),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        TypeAnimatedOverlay(typeName = type1Name ?: "normal", modifier = Modifier.matchParentSize())
                        val iconResId = type1Name?.let { getPokemonTypeToIcon(it) } ?: 0
                        if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = type1Name,
                                modifier = Modifier.padding(8.dp).size(40.dp),
                                alpha = 0.4f
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(g2.first, g2.second))),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        TypeAnimatedOverlay(typeName = type2Name ?: "normal", modifier = Modifier.matchParentSize())
                        val iconResId = type2Name?.let { getPokemonTypeToIcon(it) } ?: 0
                        if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = type2Name,
                                modifier = Modifier.padding(8.dp).size(40.dp),
                                alpha = 0.4f
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(g1.first, g1.second)))
            ) {
                TypeAnimatedOverlay(typeName = type1Name ?: "normal", modifier = Modifier.matchParentSize())
                val iconResId = type1Name?.let { getPokemonTypeToIcon(it) } ?: 0
                if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = type1Name,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(40.dp),
                        alpha = 0.4f
                    )
                }
            }
        }

        // --- Botones: Shiny + Pokeball en Column vertical, bottom-start ---
        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

        // Pulse sutil de la pokeball — solo en pagina activa para no gastar CPU
        val pulseScale = if (isActivePage) {
            val infiniteTransition = rememberInfiniteTransition(label = "pokeballPulse")
            infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "pulse"
            ).value
        } else 1f

        val sharedElementModifier = if (isActivePage && sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    rememberSharedContentState(key = "pokeball-transit-${pokemon.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        } else Modifier

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
                .zIndex(10f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Boton shiny
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isShiny) Color(0xFFFFD700).copy(alpha = 0.4f)
                        else Color.White.copy(alpha = 0.15f)
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val wasShiny = isShiny
                        isShiny = !isShiny
                        if (!wasShiny) shinySparkleKey++ // sparkles solo al activar shiny
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2726",
                    fontSize = 18.sp,
                    color = if (isShiny) Color(0xFFB8860B) else Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Pokeball animada: se abre al soltar/capturar el pokemon
            AnimatedPokeball(
                isOpen = showShimmerEffect, // abierta mientras el pokemon emerge/se recoge
                modifier = sharedElementModifier
                    .then(Modifier
                        .size(45.dp)
                        .graphicsLayer {
                            scaleX = if (!isRecalling) pulseScale else 1f
                            scaleY = if (!isRecalling) pulseScale else 1f
                        }
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRecalling = true
                            internalScaleTarget = 0f
                        }
                    )
            )
        }

        // --- Sprite del Pokemon: crece + se mueve desde pokeball al centro ---
        val activeResourceId = if (isShiny && shinyResourceId != 0) shinyResourceId
            else if (isShiny) 0
            else webpResourceId
        val useAnimated = activeResourceId != 0
        val useGigamax = !useAnimated && !isShiny && gigamaxSpriteUrl != null

        val imageUrl = when {
            isFemale && isShiny -> pokemon.sprites.frontShinyFemale
                ?: pokemon.sprites.other?.home?.frontShinyFemale
                ?: pokemon.sprites.other?.officialArtwork?.frontShiny
                ?: pokemon.sprites.frontShiny
            isFemale -> pokemon.sprites.frontFemale
                ?: pokemon.sprites.other?.home?.frontFemale
                ?: pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
            isShiny -> pokemon.sprites.other?.officialArtwork?.frontShiny
                ?: pokemon.sprites.frontShiny
                ?: pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
            else -> pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
        }
        val imagePadding by animateDpAsState(
            targetValue = if (isExpanded) 4.dp else 30.dp,
            animationSpec = tween(450),
            label = "imagePadding"
        )

        // Offset desde pokeball (bottom-start) hacia centro: se reduce con actualScale
        val density = LocalDensity.current
        val startOffsetXPx = remember(containerWidthPx) {
            with(density) { (-(maxWidth / 2) + 30.dp).toPx() }
        }
        val startOffsetYPx = remember(containerHeightPx) {
            with(density) { ((maxHeight / 2) - 30.dp).toPx() }
        }
        val moveProgress = actualScale // 0=en pokeball, 1=en centro

        // Silueta rojo-blanco durante el shimmer: interpola de rojo a blanco con la escala
        val silhouetteColor = androidx.compose.ui.graphics.lerp(
            rojo_pokeball, Color.White, actualScale.coerceIn(0f, 1f)
        )
        val silhouetteFilter = if (showShimmerEffect) {
            ColorFilter.tint(color = silhouetteColor, blendMode = BlendMode.SrcAtop)
        } else null

        val burstAlpha = (1f - actualScale).coerceIn(0f, 1f)

        if (useAnimated || useGigamax || imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = imagePadding)
                    .graphicsLayer {
                        scaleX = actualScale
                        scaleY = actualScale
                        translationX = startOffsetXPx * (1f - moveProgress)
                        translationY = startOffsetYPx * (1f - moveProgress)
                    }
                    .zIndex(1f)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (useAnimated) {
                    val animatedUri = "android.resource://${context.packageName}/$activeResourceId"
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(animatedUri)
                            .crossfade(true)
                            .build(),
                        imageLoader = animatedImageLoader,
                        contentDescription = "${pokemon.name} sprite",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = silhouetteFilter
                    )
                } else if (useGigamax) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(gigamaxSpriteUrl)
                            .crossfade(true)
                            .build(),
                        imageLoader = animatedImageLoader,
                        contentDescription = "${pokemon.name} gigamax sprite",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = silhouetteFilter,
                        error = painterResource(id = R.drawable.pokeball_icon),
                        placeholder = painterResource(id = R.drawable.pokeball_icon)
                    )
                } else if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "${pokemon.name} sprite",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = silhouetteFilter,
                        error = painterResource(id = R.drawable.pokeball_icon),
                        placeholder = painterResource(id = R.drawable.pokeball_icon)
                    )
                }

                // Sparkles al activar shiny
                androidx.compose.runtime.key(shinySparkleKey) {
                    if (shinySparkleKey > 0) {
                        ShinySparkleEffect(
                            trigger = true,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                // Shimmer rojo: anillo que sigue al pokemon (dentro del Box trasladado+escalado)
                if (showShimmerEffect) {
                    val ringCenter = actualScale * 0.65f
                    val ringThickness = 0.22f
                    val innerEdge = (ringCenter - ringThickness / 2f).coerceAtLeast(0f)
                    val outerEdge = (ringCenter + ringThickness / 2f).coerceAtMost(0.95f)
                    val glowEdge = (outerEdge + 0.10f).coerceAtMost(1f)

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.White.copy(alpha = 0.95f * burstAlpha),
                                        (innerEdge * 0.4f).coerceAtLeast(0.01f) to Color.White.copy(alpha = 0.4f * burstAlpha),
                                        innerEdge.coerceAtLeast(0.02f) to rojo_pokeball.copy(alpha = 0.15f * burstAlpha),
                                        ((innerEdge + ringCenter) / 2f).coerceAtLeast(0.03f) to rojo_pokeball.copy(alpha = 1f * burstAlpha),
                                        ringCenter.coerceAtLeast(0.04f) to rojo_pokeball.copy(alpha = 1f * burstAlpha),
                                        ((ringCenter + outerEdge) / 2f).coerceAtLeast(0.05f) to rojo_pokeball.copy(alpha = 0.85f * burstAlpha),
                                        outerEdge to rojo_pokeball.copy(alpha = 0.35f * burstAlpha),
                                        glowEdge to rojo_pokeball.copy(alpha = 0.06f * burstAlpha),
                                        1.0f to Color.Transparent
                                    ),
                                    radius = containerHeightPx * 0.7f
                                )
                            )
                    )
                }
            }
        }
    }
}
