package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
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
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorSurface
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.screen.ficha.composable.DetallesDesplegables
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.NombreNumAlturaPeso
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.ExoPlayerSimple
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.adaptaNombre
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.transformPokemonNameToResourceName
import com.david.pokedex_api.ui.theme.background_app
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
                        .background(background_app),
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = getPokemonTypeColorSurface(type1))
    ) {
        // Header fijo: imagen + nombre
        ComponenteImagen(pokemon = pokemon, isActivePage = isActivePage, pokemonViewModel = pokemonViewModel)

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

        // Contenido con FAB de secciones - sin separacion
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
            onAvailableSectionsChanged = { pokemonViewModel.availableDetailSections.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
fun ComponenteImagen(
    pokemon: PokemonDetailResponse,
    isActivePage: Boolean = true,
    pokemonViewModel: PokemonViewModel? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Shiny toggle
    var isShiny by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Fondo dual-color por tipo
        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            shape = RoundedCornerShape(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Banda superior — tipo 1
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(color1),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (pokemon.types.size == 2 && type1Name != null) {
                        val iconResId = getPokemonTypeToIcon(type1Name)
                        if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = type1Name,
                                modifier = Modifier.padding(8.dp).size(40.dp)
                            )
                        }
                    }
                }

                // Banda inferior — tipo 2 (o tipo 1 si mono-tipo)
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(color2),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
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

                        // Icono de tipo
                        val typeToShowOnBottom = if (pokemon.types.size == 1) type1Name else type2Name
                        if (typeToShowOnBottom != null) {
                            val iconResId = getPokemonTypeToIcon(typeToShowOnBottom)
                            if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = typeToShowOnBottom,
                                    modifier = Modifier.padding(start = 8.dp).size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Contenido central: official artwork con toggle shiny al pulsar
        val imageUrl = if (isShiny) {
            pokemon.sprites.other?.officialArtwork?.frontShiny
                ?: pokemon.sprites.frontShiny
                ?: pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
        } else {
            pokemon.sprites.other?.officialArtwork?.frontDefault
                ?: pokemon.sprites.frontDefault
        }
        if (imageUrl != null) {
            Box(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .graphicsLayer { scaleX = actualScale; scaleY = actualScale }
                    .align(Alignment.Center)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isShiny = !isShiny
                    },
                contentAlignment = Alignment.Center
            ) {
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
