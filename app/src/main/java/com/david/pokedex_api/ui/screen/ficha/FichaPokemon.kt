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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.screen.ficha.composable.DetallesDesplegables
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.NombreNumAlturaPeso
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.WebmImageDialog
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
@OptIn(ExperimentalMaterial3Api::class)
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

    // Cargar los detalles del Pokémon cuando esta pantalla se compone o pokemonName cambia
    LaunchedEffect(pokemonName) {
        Log.d("PokemonDetailScreen", "Fetching details for $pokemonName")
        pokemonViewModel.fetchPokemonDetailsByName(pokemonName, "es")
    }

    // Cargar encuentros cuando tengamos el ID del pokemon
    LaunchedEffect(pokemonDetail?.id) {
        pokemonDetail?.id?.let { pokemonViewModel.fetchPokemonEncounters(it) }
    }

    // Limpiar detalles cuando la pantalla se va
    DisposableEffect(Unit) {
        onDispose {
            // Descomenta si quieres limpiar los detalles al salir de esta pantalla
            // pokemonViewModel.clearPokemonDetails()
            // pokemonViewModel.clearError() // También podrías limpiar errores específicos de detalles
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
            if (isLoadingDetails && pokemonDetail == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background_app),
                    contentAlignment = Alignment.Center // El Box centrará su contenido
                ) {
                    Column( // Usamos una Column para apilar el Lottie y el Texto verticalmente
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Lottie(
                            rawResId = R.raw.pokeball, // <--- CAMBIA ESTO AL ID DE TU ARCHIVO LOTTIE
                            modifier = Modifier.size(200.dp),
                        )
                    }
                }
            } else if (pokemonDetail != null) {
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
                    encounters = encounters,
                    isLoadingEncounters = isLoadingEncounters
                )
            }
            else if (error != null && !isLoadingDetails) {
                // Si hubo un error y no está cargando, muestra el mensaje de error.
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(
//                        text = "Could not load Pokémon details.",
//                        style = MaterialTheme.typography.bodyLarge,
//                        textAlign = TextAlign.Center
//                    )
////                    Spacer(modifier = Modifier.height(8.dp))
//                    Button(onClick = {
//                        // Reintentar cargar
//                        pokemonViewModel.fetchPokemonDetailsByName(pokemonName, "es")
//                    }) {
//                        Text("Retry")
//                    }
//                }
            } else {
                // Estado inicial o si algo más sale mal
//                Text("Select a Pokémon from the list.", textAlign = TextAlign.Center)
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
    encounters: List<com.david.pokedex_api.api.model.DisplayableEncounter> = emptyList(),
    isLoadingEncounters: Boolean = false
) {
    val spanishGenus = remember(pokemonSpecies) { // Recalcular solo si pokemonSpecies cambia
        pokemonSpecies?.genera?.find { it.language.name == "es" }?.genus
    }

    val spanishPokemonName = remember(pokemonSpecies, pokemon) {
        // Primero intenta obtener el nombre en español de pokemonSpecies
        val localizedName = pokemonSpecies?.localizedNames?.find { it.language.name == "es" }?.name
        // Si no se encuentra, usa el nombre de pokemon.name (que suele ser el nombre científico/inglés)
        // o el nombre de la especie si el localizado no está pero la especie sí.
        localizedName ?: pokemonSpecies?.name ?: pokemon.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(color = getPokemonTypeColorClear(pokemon.types[0].type.name).copy(alpha = 0.5f))
    ) {
        val (imagen, nombre_num_altura_peso_tipos, desplegables) = createRefs()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .constrainAs(imagen) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }

        ) {
            ComponenteImagen(pokemon = pokemon, nombreSpanish = spanishPokemonName)
//            ComponenteImagenHomeSprite(pokemon = pokemon)

        }
        //nombre, número, altura y peso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.15f)
                .constrainAs(nombre_num_altura_peso_tipos) {
                    top.linkTo(imagen.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(desplegables.top)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Spacer(modifier = Modifier.weight(0.05f))
            Column(
                modifier = Modifier.weight(0.65f)
            ){
                NombreNumAlturaPeso(
                    colorFondo = getPokemonTypeColor(pokemon.types[0].type.name),
                    colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                        Color.White
                    } else {
                        Color.Black // Reemplaza CardBorder si no está definido globalmente
                        // o define CardBorder en tu tema.
                    },
                    nombre = spanishPokemonName,
                    numero = pokemon.id,
                    genus = spanishGenus, // <--- PASAR EL GENUS EXTRAÍDO
                    altura = pokemon.height.toDouble(),
                    peso = pokemon.weight.toDouble(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .background(Color.Transparent),
                    tipo = pokemon.types[0].type.name
                )
            }
            /*
            Column(
                modifier = Modifier.weight(0.25f)
            ){
                pokemon.types.forEach { typeInfo ->
                    PokemonTypeChip(
                        typeName = typeInfo.type.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    )
                }
            }
            */
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f)
                .constrainAs(desplegables) {
                    top.linkTo(nombre_num_altura_peso_tipos.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(horizontal = 16.dp), // Padding horizontal para el contenido de la LazyColumn
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            DetallesDesplegables(
                pokemon,
                evolutionChainDetailResponse,
                isLoadingEvolutionChain,
                onEvolutionPokemonClick,
                description,
                pokemonApiService = pokemonViewModel.pokemonApiService,
                moveDetailsMap = moveDetailsMap,
                pokemonSpecies = pokemonSpecies,
                encounters = encounters,
                isLoadingEncounters = isLoadingEncounters
            )
        }
    }
}

private const val TAG_COMP_IMG = "ComponenteImagen"
@Composable
fun ComponenteImagen(
    pokemon: PokemonDetailResponse, // Asumimos que PokemonDetailResponse tiene al menos 'name'
    nombreSpanish: String = "", // Asumimos que PokemonDetailResponse tiene al menos 'name'
) {
    val context = LocalContext.current
    var showWebmDialog by remember { mutableStateOf(false) }

    // Deriva el nombre del recurso directamente del nombre del Pokémon.
    // Asume que los archivos en res/raw son, por ejemplo, "pikachu.webm" para "Pikachu".
    val pokemonResourceNameForDialog = remember(nombreSpanish) {
        nombreSpanish.lowercase() // Convierte "Pikachu" a "pikachu"
    }

    // Shiny toggle
    var isShiny by remember { mutableStateOf(false) }
    val imageUrl = if (isShiny) {
        pokemon.sprites.other?.officialArtwork?.frontShiny
            ?: pokemon.sprites.frontShiny
            ?: pokemon.sprites.other?.officialArtwork?.frontDefault
            ?: pokemon.sprites.frontDefault
    } else {
        pokemon.sprites.other?.officialArtwork?.frontDefault
            ?: pokemon.sprites.frontDefault
    }

    var internalScaleTarget by remember(pokemon) { mutableStateOf(0f) }
    LaunchedEffect(pokemon) {
        internalScaleTarget = 0f
        delay(50)
        internalScaleTarget = 1f
    }
    val actualScale by animateFloatAsState(
        targetValue = internalScaleTarget,
        animationSpec = keyframes {
            durationMillis = 500
            0f at 0
            1f at 500
        },
        label = "PokemonImageAppearScale"
    )

    val showShimmerEffect = actualScale < 0.95f && actualScale > 0.01f

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(0.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val type1Name = pokemon.types.getOrNull(0)?.type?.name
                val type2Name = pokemon.types.getOrNull(1)?.type?.name

                val color1 = type1Name?.let { getPokemonTypeColor(it) } ?: Color.Gray
                val color2 = type2Name?.let { getPokemonTypeColor(it) } ?: color1

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color1),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (pokemon.types.size == 2 && type1Name != null) {
                        val iconResId = getPokemonTypeToIcon(type1Name)
                        if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) { // Asume R.drawable.ic_type_normal existe o ajusta la lógica
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = type1Name,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(40.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color2),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Botones Shiny + Cry
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Shiny toggle
                            Image(
                                painter = painterResource(id = R.drawable.pokeball_icon),
                                contentDescription = "Toggle shiny",
                                modifier = Modifier
                                    .size(if (isShiny) 55.dp else 45.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isShiny = !isShiny
                                    },
                                colorFilter = if (isShiny) ColorFilter.tint(
                                    Color(0xFFFFD700),
                                    BlendMode.SrcAtop
                                ) else null
                            )
                            // Botón cry
                            if (pokemon.cries?.latest != null) {
                                Image(
                                    painter = painterResource(id = R.drawable.cry_logo),
                                    contentDescription = "Escuchar cry",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            playCry(context, pokemon.cries.latest)
                                        }
                                )
                            }
                        }

                        val typeToShowOnBottom = if (pokemon.types.size == 1) type1Name else type2Name
                        if (typeToShowOnBottom != null) {
                            val iconResId = getPokemonTypeToIcon(typeToShowOnBottom)
                            if (iconResId != 0 && iconResId != R.drawable.pokeball_icon) { // Asume R.drawable.ic_type_normal existe o ajusta la lógica
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = typeToShowOnBottom,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (imageUrl != null) {
            Box(
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)//, vertical = 15.dp)
                    .graphicsLayer { scaleX = actualScale; scaleY = actualScale }
                    .align(Alignment.Center)
                    .clickable {
                        // pokemonResourceNameForDialog ahora es simplemente el nombre en minúsculas.
                        // No necesitamos verificar si es nulo si siempre se deriva de pokemon.name,
                        // a menos que pokemon.name pueda ser nulo/vacío.
                        if (pokemonResourceNameForDialog.isNotBlank()) {
                            // Antes de mostrar el diálogo, podrías verificar si el recurso realmente existe
                            // para evitar errores en ExoPlayer si el archivo no está.
                            val resourceId = context.resources.getIdentifier(
                                transformPokemonNameToResourceName(pokemonResourceNameForDialog),
                                "raw",
                                context.packageName
                            )
                            if (resourceId != 0) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showWebmDialog = true
                            } else {
                                Log.w(TAG_COMP_IMG, "WebM resource '$pokemonResourceNameForDialog' not found in res/raw.")
                                Toast.makeText(context, "Sprite no disponible para ${nombreSpanish}", Toast.LENGTH_SHORT).show()
                                Toast.makeText(context, "Nombre convertido: ${transformPokemonNameToResourceName(pokemonResourceNameForDialog)}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.w(TAG_COMP_IMG, "Nombre del Pokémon vacío, no se puede mostrar el diálogo 3D.")
                            Toast.makeText(context, "Nombre de Pokémon no válido", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${pokemon.name} sprite",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape), // Opcional
                    contentScale = ContentScale.Fit,
                    colorFilter = if (showShimmerEffect) ColorFilter.tint(
                        color = Color.White.copy(alpha = 0.7f),
                        blendMode = BlendMode.SrcAtop
                    ) else null,
                    error = painterResource(id = R.drawable.pokeball_icon), // Asume R.drawable.pokeball_icon existe
                    placeholder = painterResource(id = R.drawable.pokeball_icon) // Asume R.drawable.pokeball_icon existe
                )

                if (showShimmerEffect) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape) // Opcional
                            .background(shimmerBrush(showShimmer = true)) // Asume shimmerBrush existe
                    )
                }
            }
        }
    }

    // El diálogo WebM se muestra si showWebmDialog es true.
    // pokemonResourceNameForDialog aquí será el nombre en minúsculas, ej: "pikachu"
    if (showWebmDialog) {
        WebmImageDialog(
//            pokemonResourceName = pokemonResourceNameForDialog, // ej: "pikachu"
            pokemonResourceName = transformPokemonNameToResourceName(nombreSpanish), // ej: "pikachu"
            pokemonDisplayName = adaptaNombre(transformPokemonNameToResourceName(nombreSpanish)), // ej: "Pikachu"
            onDismiss = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showWebmDialog = false
            }
        )
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
