package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
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
import com.david.pokedex_api.util.AnimatedPokeball
import com.david.pokedex_api.util.Lottie
import com.david.pokedex_api.util.ShinySparkleEffect
import kotlinx.coroutines.delay

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

    // Cuando llega la cadena evolutiva, expandir con formas regionales y pre-cargar
    LaunchedEffect(evolutionChain, pokemonDetail?.id) {
        evolutionChain?.chain?.let { chain ->
            // Fase 1: recorrer la cadena en ORDEN EVOLUTIVO (no numerico)
            val chainOrderIds = mutableListOf<Int>()
            fun traverse(link: com.david.pokedex_api.api.model.ChainLink) {
                link.species.url.trimEnd('/').split("/").lastOrNull()?.toIntOrNull()?.let { chainOrderIds.add(it) }
                link.evolvesTo.forEach { traverse(it) }
            }
            traverse(chain)

            // Fase 2: expandir con formas regionales (Alola, Galar, Hisui, Paldea)
            // Cada regional se inserta justo despues de su species base
            val expandedIds = pokemonViewModel.expandChainWithRegionalForms(chainOrderIds)

            // Fase 3: incluir el pokemon actual si no esta (megas, gigas, etc.)
            val finalIds = expandedIds.toMutableList()
            val currentId = pokemonDetail?.id
            if (currentId != null && !finalIds.contains(currentId)) {
                // Insertar cerca de su species base si es posible
                val speciesUrl = pokemonDetail?.species?.url
                val baseSpeciesId = speciesUrl?.trimEnd('/')?.split("/")?.lastOrNull()?.toIntOrNull()
                val insertIdx = if (baseSpeciesId != null) {
                    val baseIdx = finalIds.indexOf(baseSpeciesId)
                    if (baseIdx >= 0) baseIdx + 1 else finalIds.size
                } else finalIds.size
                finalIds.add(insertIdx, currentId)
            }

            // NO sort — el orden de la cadena es el correcto
            pokemonViewModel.setNavigationList(finalIds)
            pokemonViewModel.preloadEvolutionChain(finalIds)
        }
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
            val navList by pokemonViewModel.navigationList.observeAsState(emptyList())
            val evoMap by pokemonViewModel.evoChainPokemonMap.collectAsState()
            val allPreloaded = navList.size > 1 && evoMap.size >= navList.size

            if (pokemonDetail == null) {
                val sharedTransitionScope = LocalSharedTransitionScope.current
                val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

                // Replica la estructura del layout final: 35% imagen + 65% contenido
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background_app_gradient)
                ) {
                    // Zona superior (35%): donde ira ComponenteImagen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                    ) {
                        // Lottie centrado en la zona de imagen
                        Lottie(
                            rawResId = R.raw.pokeball,
                            modifier = Modifier.size(120.dp).align(Alignment.Center),
                        )
                        // Pokeball en la misma posicion que tendra en ComponenteImagen
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
                    // Zona inferior (65%): placeholder vacio
                    Spacer(modifier = Modifier.fillMaxWidth().weight(0.65f))
                }
            } else if (allPreloaded && navList.contains(pokemonDetail!!.id)) {
                // Pager por linea evolutiva: cada pagina con sus propios datos
                val initialPage = navList.indexOf(pokemonDetail!!.id).coerceAtLeast(0)
                val pagerState = rememberPagerState(
                    initialPage = initialPage,
                    pageCount = { navList.size }
                )

                // Cuando cambia la pagina, cargar extras (encuentros, wikidex, movimientos)
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .distinctUntilChanged()
                        .collect { page ->
                            pokemonViewModel.switchToPreloadedPokemon(navList[page])
                        }
                }

                // Pre-cachear imagenes de artwork para swipe fluido
                val imageLoader = remember { coil.ImageLoader(context) }
                LaunchedEffect(allPreloaded) {
                    navList.forEach { id ->
                        evoMap[id]?.detail?.sprites?.other?.officialArtwork?.frontDefault?.let { url ->
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .size(coil.size.Size.ORIGINAL)
                                .build()
                            imageLoader.enqueue(request)
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val preloaded = evoMap[navList[page]]
                    if (preloaded != null) {
                        PokemonDetailsView(
                            pokemon = preloaded.detail,
                            pokemonSpecies = preloaded.species,
                            description = null,
                            evolutionChainDetailResponse = evolutionChain,
                            isLoadingEvolutionChain = isLoadingEvolutionChain,
                            onEvolutionPokemonClick = { clickedName ->
                                pokemonViewModel.fetchPokemonDetailsByName(clickedName, "es")
                            },
                            pokemonViewModel = pokemonViewModel,
                            moveDetailsMap = moveDetailsMap,
                            wikiDexFlavorTexts = wikiDexFlavorTexts,
                            wikiDexLocations = wikiDexLocations,
                            encounters = encounters,
                            isLoadingEncounters = isLoadingEncounters,
                            isActivePage = page == pagerState.settledPage,
                            selectedSection = pokemonViewModel.selectedDetailSection.collectAsState().value,
                            onNavigateBack = onNavigateBack,
                            shouldAnimate = true
                        )
                    }
                }
            } else {
                // Vista simple: transitoria (cadena aun cargando) o definitiva (pokemon sin pager)
                // Es definitiva si la cadena ya cargo pero este pokemon no entrara en el pager
                val chainLoaded = evolutionChain != null && !isLoadingEvolutionChain
                val pagerWillTakeOver = !chainLoaded || (navList.size > 1 && navList.contains(pokemonDetail!!.id))
                PokemonDetailsView(
                    pokemon = pokemonDetail!!,
                    pokemonSpecies = pokemonSpecies,
                    description = pokemonDescription,
                    evolutionChainDetailResponse = evolutionChain,
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

    val type1 = pokemon.types[0].type.name

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background_app_gradient)
    ) {
        // Header fijo: imagen + nombre
        ComponenteImagen(
            pokemon = pokemon,
            isActivePage = isActivePage,
            pokemonViewModel = pokemonViewModel,
            isExpanded = isImageExpanded,
            onToggleExpand = { isImageExpanded = !isImageExpanded },
            onNavigateBack = onNavigateBack,
            shouldAnimate = shouldAnimate,
            nombreSpanish = spanishPokemonName,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f + imageWeight * 0.65f)
        )

        // Secciones inferiores: nombre + desplegables — se encogen con peso animado
        if (imageWeight < 0.99f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f - imageWeight * 0.65f)
                    .graphicsLayer { alpha = bottomAlpha }
            ) {
                NombreNumAlturaPeso(
                    colorFondo = getPokemonTypeColorDark(type1),
                    colorTexto = Color.White,
                    nombre = spanishPokemonName,
                    numero = pokemon.id,
                    genus = spanishGenus,
                    altura = pokemon.height.toDouble(),
                    peso = pokemon.weight.toDouble(),
                    modifier = Modifier.fillMaxWidth(),
                    tipo = pokemon.types[0].type.name,
                    cryUrl = pokemon.cries?.latest,
                    regionTag = regionTag
                )

                // Contenido con barra de secciones integrada
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Shiny toggle + sparkle trigger
    var isShiny by remember { mutableStateOf(false) }
    var shinySparkleKey by remember { mutableStateOf(0) } // incrementa para re-disparar

    // Animated WebP resources
    val webpResourceName = remember(pokemon.name) {
        transformPokemonNameToResourceName(pokemon.name.lowercase())
    }
    val webpResourceId = remember(webpResourceName) {
        context.resources.getIdentifier(webpResourceName, "raw", context.packageName)
    }
    val shinyResourceName = remember(webpResourceName) { "${webpResourceName}_shiny" }
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

    val type1Name = pokemon.types.getOrNull(0)?.type?.name
    val type2Name = pokemon.types.getOrNull(1)?.type?.name

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

        val imageUrl = if (isShiny) {
            pokemon.sprites.other?.officialArtwork?.frontShiny
                ?: pokemon.sprites.frontShiny
                ?: pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
        } else {
            pokemon.sprites.other?.officialArtwork?.frontDefault
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
