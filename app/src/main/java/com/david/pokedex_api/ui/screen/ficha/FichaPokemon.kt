package com.david.pokedex_api.ui.screen.ficha

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImage
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
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
    val pokemonSpecies by pokemonViewModel.pokemonSpeciesDetails.observeAsState() // <--- OBSERVAR ESTE



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
    pokemonSpecies: PokemonSpeciesResponse?,
    description: String?,
    evolutionChainDetailResponse: EvolutionChainDetailResponse?,
    isLoadingEvolutionChain: Boolean,
    onEvolutionPokemonClick: (pokemonName: String) -> Unit,
    pokemonViewModel: PokemonViewModel
) {
    val spanishGenus = remember(pokemonSpecies) { // Recalcular solo si pokemonSpecies cambia
        pokemonSpecies?.genera?.find { it.language.name == "es" }?.genus
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
            ComponenteImagen(pokemon = pokemon)
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
                    nombre = pokemon.name,
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

    // Estado para controlar la visibilidad de la imagen expandida
    var showExpandedImage by remember { mutableStateOf(false) }
    val imageUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
        ?: pokemon.sprites.frontDefault


    LaunchedEffect(key1 = pokemon) {
        playAppearAnimation = true
        delay(550)
        playAppearAnimation = false
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
            durationMillis = 1000
            0f at 0
            1f at 500
        }, label = "PokemonAppearScale"
    )


    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            val color1 = getPokemonTypeColor(pokemon.types[0].type.name)
            val color2 = if (pokemon.types.size > 1) {
                getPokemonTypeColor(pokemon.types[1].type.name)
            } else {
                color1
            }

            // Card de fondo con los colores del tipo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Ajusta la altura según necesites para el fondo
                    .background(Color.Transparent),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) { // Usar Column para apilar los Box de colores
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
                                    modifier = Modifier.padding(8.dp) // Añade padding si es necesario
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(color2),
                        contentAlignment = Alignment.BottomStart // Alinea al inicio para el ícono de pokeball
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp), // Padding para los íconos
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.pokeball_icon),
                                contentDescription = "Pokeball icon",
                                modifier = Modifier.size(80.dp) // Tamaño ajustado del ícono
                            )
                            if (pokemon.types.size == 1) {
                                val iconResId = getPokemonTypeToIcon(pokemon.types[0].type.name)
                                if (iconResId != 0) {
                                    Image(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = pokemon.types[0].type.name,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else if (pokemon.types.size > 1) {
                                val iconResId = getPokemonTypeToIcon(pokemon.types[1].type.name)
                                if (iconResId != 0) {
                                    Image(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = pokemon.types[1].type.name,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Box para la imagen principal del Pokémon (la que se hace clic)
            Box(
                modifier = Modifier
                    .fillMaxSize() // Asegúrate de que este Box tenga un tamaño definido para que Alignment.Center funcione
                    .padding(horizontal = 75.dp)
                    .scale(actualScale)
                    .align(Alignment.Center)
                    .clickable { showExpandedImage = true }, // <-- IMPORTANTE: Hacer clic aquí
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${pokemon.name} sprite",
                    modifier = Modifier.size(350.dp), // Tamaño de la imagen principal
                    colorFilter = if (actualScale < 1f && actualScale > 0.01f) ColorFilter.tint(color = Color.White.copy(alpha = 0.7f), blendMode = BlendMode.SrcAtop) else null,
                    error = painterResource(id = R.drawable.pokeball_icon),
                    placeholder = painterResource(id = R.drawable.pokeball_icon)
                )
                if (actualScale < 1f && actualScale > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                         .background(shimmerBrush(showShimmer = true)) // Asegúrate que shimmerBrush esté definido
                    )
                }
            }
        }
    }

    // Mostrar el Composable de imagen expandida si showExpandedImage es true
    if (showExpandedImage && imageUrl != null) {
        ExpandedImageView(
            pokemon = pokemon, // <-- Pasa el objeto Pokemon completo
            onDismiss = { showExpandedImage = false }
        )
    }
}

@Composable
fun ExpandedImageView(
    pokemon: PokemonDetailResponse, // Acepta el objeto Pokemon completo
    onDismiss: () -> Unit
) {
    val imageUrl = pokemon.sprites.other?.officialArtwork?.frontDefault
        ?: pokemon.sprites.frontDefault

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)  // 90% del ancho de la pantalla, deja margen
                .wrapContentHeight()   // <-- CAMBIO CLAVE: La altura se ajusta al contenido
                .padding(vertical = 32.dp), // Opcional: añade padding vertical si quieres espacio arriba/abajo del contenido
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    // No necesitas fillMaxSize aquí si la Card se ajusta al contenido.
                    // Pero sí necesitas definir cómo se estructura el contenido interno.
                    .padding(0.dp), // Opcional: padding interno para la Card del Dialog
                contentAlignment = Alignment.Center
            ) {
                // Contenido interno (fondos e imagen)
                Column { // Usar Column para apilar fondos e imagen si es necesario
                    // Lógica de fondos de tipo (igual que antes)
                    if (pokemon.types.isNotEmpty()) {
                        val color1 = getPokemonTypeColor(pokemon.types[0].type.name)
                        val color2 = if (pokemon.types.size > 1) {
                            getPokemonTypeColor(pokemon.types[1].type.name)
                        } else {
                            color1
                        }

                        // Contenedor para los fondos, con una altura específica o basada en la imagen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Podrías darle una altura fija o relativa a la imagen esperada
                                // Por ejemplo, si la imagen es cuadrada y ocupa X dp, los fondos podrían ser X dp de alto
                                .height(300.dp) // Ejemplo: Altura fija para la sección de fondos
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(color1),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    if (pokemon.types.size == 2) {
                                        val iconResId = getPokemonTypeToIcon(pokemon.types[0].type.name)
                                        if (iconResId != 0) {
                                            Image(
                                                painter = painterResource(id = iconResId),
                                                contentDescription = pokemon.types[0].type.name,
                                                modifier = Modifier.padding(12.dp).size(50.dp)
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
                                            .fillMaxHeight()
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.pokeball_icon),
                                            contentDescription = "Pokeball icon",
                                            modifier = Modifier.size(90.dp)
                                        )
                                        val typeToShowIndex = if (pokemon.types.size == 1) 0 else 1
                                        if (pokemon.types.isNotEmpty()) {
                                            val iconResId = getPokemonTypeToIcon(pokemon.types[typeToShowIndex].type.name)
                                            if (iconResId != 0) {
                                                Image(
                                                    painter = painterResource(id = iconResId),
                                                    contentDescription = pokemon.types[typeToShowIndex].type.name,
                                                    modifier = Modifier.padding(start = 8.dp).size(50.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Imagen del Pokémon (superpuesta a los fondos)
                            if (imageUrl != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize() // Se ajusta al tamaño del Box de fondos
                                        .padding(16.dp), // Padding para que la imagen no toque los bordes del fondo
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "${pokemon.name} sprite (expanded)",
                                        modifier = Modifier.fillMaxSize(), // La imagen llena este Box interno
                                        contentScale = ContentScale.Fit,
                                        error = painterResource(id = R.drawable.pokeball_icon),
                                        placeholder = painterResource(id = R.drawable.pokeball_icon)
                                    )
                                }
                            }
                        } // Fin del Box de fondos e imagen
                    } else if (imageUrl != null) {
                        // Fallback si no hay tipos, solo mostrar la imagen
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "${pokemon.name} sprite (expanded)",
                            modifier = Modifier
                                .fillMaxWidth() // Ocupa el ancho disponible
                                .height(300.dp) // Dale una altura explícita
                                .padding(16.dp),
                            contentScale = ContentScale.Fit,
                            error = painterResource(id = R.drawable.pokeball_icon),
                            placeholder = painterResource(id = R.drawable.pokeball_icon)
                        )
                    }
                } // Fin de la Column principal dentro de la Card del Dialog

                // Botón para cerrar (se mantiene igual, se alinea al TopEnd de la Card del Dialog)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Se alinea respecto al Box que contiene la Column
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close expanded view",
                        tint = Color.Black
                    )
                }
            } // Fin del Box principal de la Card del Dialog
        } // Fin de la Card del Dialog
    } // Fin del Dialog
}
