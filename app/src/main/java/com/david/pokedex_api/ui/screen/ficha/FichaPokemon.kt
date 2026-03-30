package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
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
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorDark
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeGradientColors
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorSurface
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.screen.ficha.composable.DetallesDesplegables
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.NombreNumAlturaPeso
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.dinamaxLiveSprites
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.ExoPlayerSimple
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.adaptaNombre
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.transformPokemonNameToResourceName
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.background_app_gradient
import com.david.pokedex_api.util.Lottie
import com.david.pokedex_api.util.shimmerBrush
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

// Nueva pantalla para los detalles del Pokémon, para manejar la carga y la UI de detalles.
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    // Cuando llega la cadena evolutiva, pre-cargar TODOS los Pokemon de la cadena
    LaunchedEffect(evolutionChain) {
        evolutionChain?.chain?.let { chain ->
            val ids = mutableListOf<Int>()
            fun traverse(link: com.david.pokedex_api.api.model.ChainLink) {
                link.species.url.trimEnd('/').split("/").lastOrNull()?.toIntOrNull()?.let { ids.add(it) }
                link.evolvesTo.forEach { traverse(it) }
            }
            traverse(chain)
            ids.sort()
            pokemonViewModel.setNavigationList(ids)
            pokemonViewModel.preloadEvolutionChain(ids)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background_app_gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Lottie(
                        rawResId = R.raw.pokeball,
                        modifier = Modifier.size(200.dp),
                    )
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
                LaunchedEffect(allPreloaded) {
                    navList.forEach { id ->
                        evoMap[id]?.detail?.sprites?.other?.officialArtwork?.frontDefault?.let { url ->
                            val request = coil.request.ImageRequest.Builder(context)
                                .data(url)
                                .size(coil.size.Size.ORIGINAL)
                                .build()
                            coil.ImageLoader(context).enqueue(request)
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
                            selectedSection = pokemonViewModel.selectedDetailSection.collectAsState().value
                        )
                    }
                }
            } else {
                // Vista simple antes de que la cadena evolutiva este pre-cargada
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
                    selectedSection = pokemonViewModel.selectedDetailSection.collectAsState().value
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
    selectedSection: String = "DESC"
) {
    val spanishGenus = remember(pokemonSpecies) {
        pokemonSpecies?.genera?.find { it.language.name == "es" }?.genus
    }

    val spanishPokemonName = remember(pokemonSpecies, pokemon) {
        val localizedName = pokemonSpecies?.localizedNames?.find { it.language.name == "es" }?.name
        localizedName ?: pokemonSpecies?.name ?: pokemon.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
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
                    tipo = pokemon.types[0].type.name
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

@Composable
fun ComponenteImagen(
    pokemon: PokemonDetailResponse,
    isActivePage: Boolean = true,
    pokemonViewModel: PokemonViewModel? = null,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    nombreSpanish: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Shiny toggle
    var isShiny by remember { mutableStateOf(false) }

    // Animated WebP resources (normal + shiny)
    // Usamos pokemon.name (nombre API con forma, ej: "charizard-mega-x", "meowth-alola")
    // en vez de nombreSpanish (que solo tiene el nombre base sin forma)
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

    // Gigamax GIF fallback (no existen webm/webp para estas formas)
    val gigamaxSpriteUrl = remember(pokemon.id) {
        dinamaxLiveSprites.find { it.pokeId == pokemon.id }?.spriteUrl
    }

    // Animacion de escala: solo se dispara una vez por Pokemon
    val alreadyAnimated = pokemonViewModel?.animatedPokemonIds?.contains(pokemon.id) == true
    var internalScaleTarget by remember { mutableStateOf(if (alreadyAnimated) 1f else 0f) }
    LaunchedEffect(pokemon.id, isActivePage) {
        if (isActivePage && pokemonViewModel?.animatedPokemonIds?.contains(pokemon.id) != true) {
            internalScaleTarget = 0f
            delay(50)
            internalScaleTarget = 1f
            pokemonViewModel?.animatedPokemonIds?.add(pokemon.id)
        } else if (isActivePage) {
            internalScaleTarget = 1f
        }
    }
    val actualScale by animateFloatAsState(
        targetValue = internalScaleTarget,
        animationSpec = keyframes { durationMillis = 500; 0f at 0; 1f at 500 },
        label = "PokemonImageAppearScale"
    )
    val showShimmerEffect = actualScale < 0.95f && actualScale > 0.01f

    val type1Name = pokemon.types.getOrNull(0)?.type?.name
    val type2Name = pokemon.types.getOrNull(1)?.type?.name
    val color1 = type1Name?.let { getPokemonTypeColorDark(it) } ?: Color.Gray
    val color2 = type2Name?.let { getPokemonTypeColorDark(it) } ?: color1

    // Gradientes por tipo, oscurecidos para la ficha
    fun Color.darken(factor: Float = 0.75f): Color =
        Color(red * factor, green * factor, blue * factor, alpha)

    val gradient1 = type1Name?.let { getPokemonTypeGradientColors(it) }
        ?: (Color.Gray to Color.DarkGray)
    val gradient2 = type2Name?.let { getPokemonTypeGradientColors(it) }
    val isDualType = gradient2 != null

    val g1 = gradient1.first.darken() to gradient1.second.darken()
    val g2 = (gradient2?.first?.darken() ?: g1.first) to (gradient2?.second?.darken() ?: g1.second)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isDualType) {
            // Dos bandas, cada una con gradiente sutil interno
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
            // Una sola banda con gradiente sutil
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

        // Botones Pokeball + Shiny — apilados verticalmente, sobre la imagen (zIndex superior)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp)
                .zIndex(2f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Boton shiny — mismo estilo que LiveSprites
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
                        isShiny = !isShiny
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

            // Pokeball = reproduce cry
            if (pokemon.cries?.latest != null) {
                Image(
                    painter = painterResource(id = R.drawable.pokeball_icon),
                    contentDescription = "Escuchar cry",
                    modifier = Modifier
                        .size(45.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            playCry(context, pokemon.cries.latest)
                        }
                )
            }
        }

        // Contenido central: animated webp > gigamax gif > official artwork
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
        if (useAnimated || useGigamax || imageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = imagePadding)
                    .graphicsLayer { scaleX = actualScale; scaleY = actualScale }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (showShimmerEffect) Modifier.clip(CircleShape) else Modifier),
                        contentScale = ContentScale.Fit
                    )
                } else if (useGigamax) {
                    // Gigamax: GIF animado desde URL (ya tiene transparencia)
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(gigamaxSpriteUrl)
                            .crossfade(true)
                            .build(),
                        imageLoader = animatedImageLoader,
                        contentDescription = "${pokemon.name} gigamax sprite",
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (showShimmerEffect) Modifier.clip(CircleShape) else Modifier),
                        contentScale = ContentScale.Fit,
                        error = painterResource(id = R.drawable.pokeball_icon),
                        placeholder = painterResource(id = R.drawable.pokeball_icon)
                    )
                } else if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "${pokemon.name} sprite",
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (showShimmerEffect) Modifier.clip(CircleShape) else Modifier),
                        contentScale = ContentScale.Fit,
                        colorFilter = if (showShimmerEffect) ColorFilter.tint(
                            color = Color.White.copy(alpha = 0.7f),
                            blendMode = BlendMode.SrcAtop
                        ) else null,
                        error = painterResource(id = R.drawable.pokeball_icon),
                        placeholder = painterResource(id = R.drawable.pokeball_icon)
                    )
                }
                if (showShimmerEffect) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(shimmerBrush(showShimmer = true))
                    )
                }
            }
        }

    }
}

private fun playCry(context: Context, url: String) {
    val player = ExoPlayer.Builder(context).build()
    player.setMediaItem(MediaItem.fromUri(url))
    player.prepare()
    player.play()
    player.addListener(object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                player.release()
            }
        }
    })
}
