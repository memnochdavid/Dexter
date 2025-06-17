package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.ficha.composable.DetallesDesplegables
import com.david.pokedex_api.util.Lottie
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.NombreNumAlturaPeso
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.util.shimmerBrush
import kotlinx.coroutines.delay

// Nueva pantalla para los detalles del Pokémon, para manejar la carga y la UI de detalles.
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


    // Cargar los detalles del Pokémon cuando esta pantalla se compone o pokemonName cambia
    LaunchedEffect(pokemonName) {
        Log.d("PokemonDetailScreen", "Fetching details for $pokemonName")
        pokemonViewModel.fetchPokemonDetailsByName(pokemonName, "es")
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

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingDetails && pokemonDetail == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(background_app),
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
                    description = pokemonDescription, // Asumo que lo obtienes del ViewModel
                    evolutionChainDetailResponse = evolutionChain, // Asumo que lo obtienes del ViewModel
                    isLoadingEvolutionChain = isLoadingEvolutionChain, // Asumo que lo obtienes del ViewModel
                    onEvolutionPokemonClick = { pokemonNameClicked ->
                        pokemonViewModel.fetchPokemonDetailsByName(pokemonNameClicked, "es")
                    },
                    pokemonViewModel = pokemonViewModel // <--- Importante: Pasa el ViewModel completo
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


@SuppressLint("DefaultLocale")
@Composable
fun PokemonDetailsView(
    pokemon: PokemonDetailResponse,
    description: String?,
    evolutionChainDetailResponse: EvolutionChainDetailResponse?,
    isLoadingEvolutionChain: Boolean,
    onEvolutionPokemonClick: (pokemonName: String) -> Unit,
    pokemonViewModel: PokemonViewModel
) {

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
            ComponenteImagen(pokemon = pokemon)
        }
        //nombre, número, altura y peso
        Row(
            modifier = Modifier
                .fillMaxWidth(1f)
                .fillMaxHeight(0.11f)
                .constrainAs(nombre_num_altura_peso_tipos) {
                    top.linkTo(imagen.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(desplegables.top)
                },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.65f)
            ){
                NombreNumAlturaPeso(
                    colorFondo = getPokemonTypeColor(pokemon.types[0].type.name),
                    colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                        Color.White
                    } else {
                        CardBorder
                    },
                    nombre = pokemon.name,
                    numero = pokemon.id,
                    altura = pokemon.height.toDouble(),
                    peso = pokemon.weight.toDouble(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 3.dp)
                        .background(Color.Transparent)
                )
            }
            Column(
                modifier = Modifier.weight(0.25f)
            ){
                pokemon.types.forEach { typeInfo ->
                    PokemonTypeChip(
                        typeName = typeInfo.type.name,
                        modifier = Modifier.weight(1f).padding(end = 16.dp)
                    )
                }
            }
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
                pokemonApiService = pokemonViewModel.pokemonApiService
            )
        }
    }
}

@Composable
fun ComponenteImagen(
    pokemon: PokemonDetailResponse,
) {
    var playAppearAnimation by remember(pokemon) { mutableStateOf(true) }

    LaunchedEffect(key1 = pokemon) {
        // This ensures that when a new Pokemon is passed, we trigger the animation.
        playAppearAnimation = true
        // SonidoPokeball(context, 500) // Play sound if you have it

        // After the animation duration, we can set playAppearAnimation to false.
        // This signifies the initial "appear" animation is done.
        // The image will then remain at scale 1f.
        delay(550) // Slightly longer than the animation duration
        playAppearAnimation = false
    }

    // A more direct way to handle the scale animation for appearing:
    var internalScaleTarget by remember(pokemon) { mutableStateOf(0f) }

    LaunchedEffect(pokemon) {
        internalScaleTarget = 0f // Reset to 0 for a new Pokemon
        // SonidoPokeball(context, 500) // Play sound
        delay(50) // Brief delay to ensure reset takes effect before targeting 1f
        internalScaleTarget = 1f // Trigger animation to 1f
    }

    val actualScale by animateFloatAsState(
        targetValue = internalScaleTarget,
        animationSpec = keyframes {
            durationMillis = 1000 // Total duration
            0f at 0 // Start at 0
            1f at 500 // Reach 1 at 500ms (halfway)
        }, label = ""
    )


    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
            ?: pokemon.sprites.frontDefault

        if (imageUrl != null) {
            val color1 = getPokemonTypeColor(pokemon.types[0].type.name)
            val color2 = if (pokemon.types.size > 1) {
                getPokemonTypeColor(pokemon.types[1].type.name)
            } else {
                color1
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                shape = RoundedCornerShape(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color1),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val iconResId = getPokemonTypeToIcon(pokemon.types[0].type.name)
                    if (iconResId != 0) {
                        if (pokemon.types.size == 2) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = pokemon.types[0].type.name,
                                modifier = Modifier
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(color2),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.pokeball_icon),
                            contentDescription = "Pokeball icon",
                            modifier = Modifier.size(100.dp)
                        )
                        if (pokemon.types.size == 1) {
                            val iconResId = getPokemonTypeToIcon(pokemon.types[0].type.name)
                            if (iconResId != 0) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = pokemon.types[0].type.name,
                                    modifier = Modifier
                                )
                            }
                        } else if (pokemon.types.size > 1) {
                            val iconResId = getPokemonTypeToIcon(pokemon.types[1].type.name)
                            if (iconResId != 0) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = pokemon.types[1].type.name,
                                    modifier = Modifier
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(1f)
                    .padding(horizontal = 75.dp)
                    .scale(actualScale) // Use the corrected scale
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${pokemon.name} sprite",
                    modifier = Modifier.size(350.dp),
                    colorFilter = if (actualScale < 1f && actualScale > 0.01f) ColorFilter.tint(color = Color.White.copy(alpha = 0.7f), blendMode = BlendMode.SrcAtop) else null,
                    error = painterResource(id = R.drawable.pokeball_icon),
                    placeholder = painterResource(id = R.drawable.pokeball_icon)
                )
                if (actualScale < 1f && actualScale > 0.01f) {
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
