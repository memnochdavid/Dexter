package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.david.pokedex_api.R
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeToIcon
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLOutput


const val BASE_GIF_URL = "https://projectpokemon.org/images/normal-sprite/"
const val SHINY_GIF_URL = "https://projectpokemon.org/images/shiny-sprite/"


//https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-1-pok%C3%A9mon-r90/


suspend fun loadDrawableFromUrl(
    context: Context,
    url: String,
    imageLoader: ImageLoader
): Drawable? {
    return try {
        val request = ImageRequest.Builder(context)
            .data(url)
            // Para GIFs animados con Coil, el GifDecoder se encargará.
            // Si el resultado es un Drawable animado (como AnimatedImageDrawable),
            // accompanist-drawablepainter debería poder manejarlo.
            .build()
        imageLoader.execute(request).drawable
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


@Composable
fun LiveSprites(
    pokemon: PokemonDetailResponse,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    // Adaptar el nombre del Pokémon para las mega evoluciones X/Y
    val adaptedPokemonName = when {
        pokemon.name.contains("-mega-x") -> pokemon.name.replace("-mega-x", "-megax")
        pokemon.name.contains("-mega-y") -> pokemon.name.replace("-mega-y", "-megay")
        pokemon.name.contains("-m") -> pokemon.name.replace("-m", "_m")
        pokemon.name.contains("-f") -> pokemon.name.replace("-f", "_f")
        else -> pokemon.name
    }
//    println("LiveSprites: NOMBRE ORIGINAL: - $pokemonName, NOMBRE ADAPTADO: - $adaptedPokemonName") // Para depuración

    var showAnimatedSpriteDialog by remember { mutableStateOf(false) }
    var isShinySpriteForDialog by remember { mutableStateOf(false) } // Para saber si es el shiny el que se clickeó


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = modifier.weight(0.2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Sprites animados",
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = colorTexto,
                modifier = Modifier
                    .padding(vertical = 10.dp, horizontal = 10.dp)
            )
        }
        Row(
            modifier = modifier.weight(0.6f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = modifier.weight(0.45f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .clickable { // <-- Añadir clickable aquí
                            isShinySpriteForDialog = false
                            showAnimatedSpriteDialog = true
                        },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PokemonAnimatedSpriteWithAccompanist(
                        pokemonName = adaptedPokemonName,
                        pokemonId = pokemon.id,
                        esShiny = false,
                        modifier = Modifier.fillMaxSize(1f)
                    )
                }
                if (!pokemon.name.lowercase().contains("-gmax")) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .clickable { // <-- Añadir clickable aquí
                                isShinySpriteForDialog = true
                                showAnimatedSpriteDialog = true
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PokemonAnimatedSpriteWithAccompanist(
                            pokemonName = adaptedPokemonName,
                            pokemonId = pokemon.id,
                            esShiny = true,
                            modifier = Modifier.fillMaxSize(1f)
                        )
                    }
                }
            }
        }
    }

    // Mostrar el Composable de diálogo si showAnimatedSpriteDialog es true
    if (showAnimatedSpriteDialog) {
        AnimatedSpriteDialogView(
            pokemon = pokemon, // Pasamos el objeto Pokemon
            pokemonName = adaptedPokemonName, // El nombre adaptado para la URL del GIF
            pokemonId = pokemon.id,
            isShiny = isShinySpriteForDialog,
            onDismiss = { showAnimatedSpriteDialog = false }
        )
    }
}

@Composable
fun PokemonAnimatedSpriteWithAccompanist(
    pokemonName: String,
    pokemonId: Int,
    esShiny: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .logger(DebugLogger()) // Mantenlo para depurar al principio
            .build()
    }
    var drawable by remember { mutableStateOf<Drawable?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val formattedPokemonName =
        pokemonName.lowercase() // Puede que necesites un formateo más complejo
    // dependiendo de los nombres de archivo del servidor.
//    val imageUrl = if(!esShiny) "$BASE_GIF_URL$formattedPokemonName.gif" else "$SHINY_GIF_URL$formattedPokemonName.gif"

    val imageUrl = if (pokemonName.lowercase().contains("-gmax")) {
        dinamaxLiveSprites.find { it.pokeId == pokemonId }!!.spriteUrl
    } else if (pokemonName.lowercase().contains("mega") && !pokemonName.lowercase()
            .contains("yanmega") && !pokemonName.lowercase().contains("meganium")
    ) {
        val megaName = pokemonName.replace("_", "-")
        if (!esShiny) "$BASE_GIF_URL$megaName.gif" else "$SHINY_GIF_URL$megaName.gif"
    } else {
        if (!esShiny) "$BASE_GIF_URL$formattedPokemonName.gif" else "$SHINY_GIF_URL$formattedPokemonName.gif"
    }

    LaunchedEffect(imageUrl) { // Recargar si la URL cambia
        isLoading = true
        Log.d("PokemonGif", "Cargando Pokémon desde: $imageUrl")
        drawable = withContext(Dispatchers.IO) {
            loadDrawableFromUrl(context, imageUrl.toString(), imageLoader)
        }
        Log.d("PokemonGif", "Drawable para $pokemonName: ${drawable?.javaClass?.name}")
        if (drawable is Animatable) {
            Log.d("PokemonGif", "$pokemonName ES Animatable.")
        } else {
            Log.d(
                "PokemonGif",
                "$pokemonName NO es Animatable. URL podría ser incorrecta o no es un GIF animado."
            )
        }
        isLoading = false
    }

    if (isLoading) {
        Text("Cargando sprite de $pokemonName...")
    } else if (drawable != null) {
        Image(
            painter = rememberDrawablePainter(drawable = drawable),
            contentDescription = "Animated sprite for $pokemonName",
            modifier = modifier
                .size(200.dp) // Ajusta según necesites
//                .border(1.dp, Color.Green) // Para ver el borde
        )
    } else {
        Text("Error cargando sprite para $pokemonName desde $imageUrl")
    }
}

@Composable
fun AnimatedSpriteDialogView(
    pokemon: PokemonDetailResponse, // Para los colores de fondo y potencialmente otra información
    pokemonName: String, // Nombre adaptado para la URL del GIF
    pokemonId: Int,      // ID para la URL del GIF
    isShiny: Boolean,    // Para saber si mostrar el GIF shiny o normal
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Para controlar el ancho manualmente
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // Ocupa el 95% del ancho de la pantalla
                .wrapContentHeight()   // La altura se ajusta al contenido
                .padding(vertical = 16.dp), // Padding vertical para la Card
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box( // Box principal para poder alinear el botón de cierre
                modifier = Modifier
                    // No es necesario fillMaxSize aquí si la Card se ajusta al contenido.
                    .padding(0.dp) // Sin padding adicional aquí, el contenido lo maneja
            ) {
                Column { // Columna para apilar los fondos y el GIF
                    // Lógica de fondos de tipo (similar a ExpandedImageView)
                    if (pokemon.types.isNotEmpty()) {
                        val color1 = getPokemonTypeColor(pokemon.types[0].type.name)
                        val color2 = if (pokemon.types.size > 1) {
                            getPokemonTypeColor(pokemon.types[1].type.name)
                        } else {
                            color1 // Si solo hay un tipo, ambos fondos son del mismo color
                        }

                        // Contenedor para los fondos y el GIF
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Altura para la sección de fondos y GIF.
                                // Podrías hacerla dinámica o fijarla como en ExpandedImageView.
                                .height(350.dp) // Ajusta esta altura según necesites
                        ) {
                            // Fondos de tipo
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(color1),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    // Icono del primer tipo (si hay dos tipos)
                                    if (pokemon.types.size == 2) {
                                        val iconResId =
                                            getPokemonTypeToIcon(pokemon.types[0].type.name)
                                        if (iconResId != 0) { // Asumiendo que 0 es un ID inválido
                                            Image(
                                                painter = painterResource(id = iconResId),
                                                contentDescription = pokemon.types[0].type.name,
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .size(50.dp)
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
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ), // Padding para los íconos
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.pokeball_icon), // Reemplaza con tu drawable
                                            contentDescription = "Pokeball icon",
                                            modifier = Modifier.size(90.dp) // Tamaño ajustado del ícono
                                        )
                                        // Icono del segundo tipo (o del primero si solo hay uno)
                                        val typeToShowIndex = if (pokemon.types.size == 1) 0 else 1
                                        if (pokemon.types.isNotEmpty()) { // Siempre debería haber al menos un tipo
                                            val iconResId =
                                                getPokemonTypeToIcon(pokemon.types[typeToShowIndex].type.name)
                                            if (iconResId != 0) {
                                                Image(
                                                    painter = painterResource(id = iconResId),
                                                    contentDescription = pokemon.types[typeToShowIndex].type.name,
                                                    modifier = Modifier
                                                        .padding(start = 8.dp)
                                                        .size(50.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // GIF Animado del Pokémon (superpuesto a los fondos)
                            Box(
                                modifier = Modifier
                                    .matchParentSize() // Se ajusta al tamaño del Box de fondos
                                    .padding(16.dp),   // Padding para que el GIF no toque los bordes del fondo
                                contentAlignment = Alignment.Center
                            ) {
                                PokemonAnimatedSpriteWithAccompanist( // Reutilizamos el Composable que ya tienes
                                    pokemonName = pokemonName,       // Nombre adaptado
                                    pokemonId = pokemonId,           // ID del Pokémon
                                    esShiny = isShiny,               // Si es shiny o no
                                    modifier = Modifier.fillMaxSize()// El GIF llena este Box interno
                                )
                            }
                        } // Fin del Box de fondos y GIF

                    } else {
                        // Fallback si el Pokémon no tiene tipos (raro, pero para ser exhaustivos)
                        // Solo mostrar el GIF centrado
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(350.dp) // Altura consistente
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PokemonAnimatedSpriteWithAccompanist(
                                pokemonName = pokemonName,
                                pokemonId = pokemonId,
                                esShiny = isShiny,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Se alinea respecto al Box que contiene la Column
                        .padding(8.dp) // Un poco de padding para que no esté pegado al borde
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar vista expandida",
                        tint = Color.Black // O el color que prefieras para el icono
                    )
                }
            } // Fin del Box principal de la Card del Dialog
        } // Fin de la Card del Dialog
    } // Fin del Dialog
}


data class DinamaxLiveSprite(
    val pokeId: Int,
    val spriteUrl: String,
)

val dinamaxLiveSprites = listOf<DinamaxLiveSprite>(
    DinamaxLiveSprite(
        10196,
        "https://images.wikidexcdn.net/mwuploads/wikidex/9/98/latest/20191206054551/Charizard_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10198,
        "https://images.wikidexcdn.net/mwuploads/wikidex/f/f2/latest/20191208035316/Butterfree_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10199,
        "https://images.wikidexcdn.net/mwuploads/wikidex/7/74/latest/20191207032343/Pikachu_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10200,
        "https://images.wikidexcdn.net/mwuploads/wikidex/5/5c/latest/20191204065122/Meowth_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10201,
        "https://images.wikidexcdn.net/mwuploads/wikidex/9/9e/latest/20191206055329/Machamp_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10202,
        "https://images.wikidexcdn.net/mwuploads/wikidex/b/ba/latest/20191205025526/Gengar_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10203,
        "https://images.wikidexcdn.net/mwuploads/wikidex/6/6b/latest/20191204064551/Kingler_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10204,
        "https://images.wikidexcdn.net/mwuploads/wikidex/3/30/latest/20191206055216/Lapras_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10205,
        "https://images.wikidexcdn.net/mwuploads/wikidex/8/81/latest/20191208180045/Eevee_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10206,
        "https://images.wikidexcdn.net/mwuploads/wikidex/7/7d/latest/20191205030221/Snorlax_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10207,
        "https://images.wikidexcdn.net/mwuploads/wikidex/3/3a/latest/20191206055024/Garbodor_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10208,
        "https://images.wikidexcdn.net/mwuploads/wikidex/5/50/latest/20191208030656/Melmetal_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10212,
        "https://images.wikidexcdn.net/mwuploads/wikidex/8/86/latest/20191207034018/Corviknight_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10213,
        "https://images.wikidexcdn.net/mwuploads/wikidex/4/4a/latest/20191208043316/Orbeetle_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10214,
        "https://images.wikidexcdn.net/mwuploads/wikidex/a/a7/latest/20191202213011/Drednaw_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10215,
        "https://images.wikidexcdn.net/mwuploads/wikidex/c/c9/latest/20191205024332/Coalossal_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10216,
        "https://images.wikidexcdn.net/mwuploads/wikidex/b/b0/latest/20191205025109/Flapple_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10218,
        "https://images.wikidexcdn.net/mwuploads/wikidex/5/5f/latest/20191208043317/Sandaconda_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10219,
        "https://images.wikidexcdn.net/mwuploads/wikidex/1/14/latest/20191206055500/Toxtricity_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10228,
        "https://images.wikidexcdn.net/mwuploads/wikidex/1/14/latest/20191206055500/Toxtricity_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10220,
        "https://images.wikidexcdn.net/mwuploads/wikidex/b/be/latest/20191205024034/Centiskorch_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10221,
        "https://images.wikidexcdn.net/mwuploads/wikidex/5/51/latest/20191205030031/Hatterene_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10222,
        "https://images.wikidexcdn.net/mwuploads/wikidex/b/bf/latest/20191205025806/Grimmsnarl_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10223,
        "https://images.wikidexcdn.net/mwuploads/wikidex/0/09/latest/20191205023619/Alcremie_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10224,
        "https://images.wikidexcdn.net/mwuploads/wikidex/e/ea/latest/20191205024732/Copperajah_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10225,
        "https://images.wikidexcdn.net/mwuploads/wikidex/2/28/latest/20191206054822/Duraludon_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10195,
        "https://images.wikidexcdn.net/mwuploads/wikidex/5/56/latest/20200621041242/Venusaur_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10197,
        "https://images.wikidexcdn.net/mwuploads/wikidex/1/10/latest/20200621041607/Blastoise_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10209,
        "https://images.wikidexcdn.net/mwuploads/wikidex/4/46/latest/20200621042120/Rillaboom_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10210,
        "https://images.wikidexcdn.net/mwuploads/wikidex/c/c2/latest/20200621042347/Cinderace_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10211,
        "https://images.wikidexcdn.net/mwuploads/wikidex/8/89/latest/20200928210111/Inteleon_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10226,
        "https://images.wikidexcdn.net/mwuploads/wikidex/3/31/latest/20200715053735/Urshifu_brusco_Gigamax_EpEc.gif"
    ),
    DinamaxLiveSprite(
        10227,
        "https://images.wikidexcdn.net/mwuploads/wikidex/8/85/latest/20200715054208/Urshifu_fluido_Gigamax_EpEc.gif"
    ),


    )
