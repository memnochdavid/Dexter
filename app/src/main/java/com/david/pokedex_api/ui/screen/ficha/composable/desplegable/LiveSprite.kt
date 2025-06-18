package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLOutput


const val BASE_GIF_URL = "https://projectpokemon.org/images/normal-sprite/"
const val SHINY_GIF_URL = "https://projectpokemon.org/images/shiny-sprite/"


//https://projectpokemon.org/home/docs/spriteindex_148/3d-models-generation-1-pok%C3%A9mon-r90/


suspend fun loadDrawableFromUrl(context: Context, url: String, imageLoader: ImageLoader): Drawable? {
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
    pokemonName: String,
    colorTexto : Color,
    modifier: Modifier = Modifier
){
    // Adaptar el nombre del Pokémon para las mega evoluciones X/Y
    val adaptedPokemonName = when {
        pokemonName.contains("-mega-x") -> pokemonName.replace("-mega-x", "-megax")
        pokemonName.contains("-mega-y") -> pokemonName.replace("-mega-y", "-megay")
        pokemonName.contains("-m") -> pokemonName.replace("-m", "_m")
        pokemonName.contains("-f") -> pokemonName.replace("-f", "_f")
        else -> pokemonName
    }
    println("LiveSprites: NOMBRE ORIGINAL: - $pokemonName, NOMBRE ADAPTADO: - $adaptedPokemonName") // Para depuración

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            modifier = modifier.weight(0.2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ){
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
        ){
            Row(
                modifier = modifier.weight(0.45f), // Considera si este peso sigue siendo adecuado
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column(
                    modifier = Modifier.weight(0.45f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    PokemonAnimatedSpriteWithAccompanist(
                        pokemonName = adaptedPokemonName, // Usar el nombre adaptado
                        esShiny = false,
                        modifier = Modifier.fillMaxSize(1f)
                    )
                }
                Column(
                    modifier = Modifier.weight(0.45f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    PokemonAnimatedSpriteWithAccompanist(
                        pokemonName = adaptedPokemonName, // Usar el nombre adaptado
                        esShiny = true,
                        modifier = Modifier.fillMaxSize(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PokemonAnimatedSpriteWithAccompanist(
    pokemonName: String,
    esShiny : Boolean,
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

    val formattedPokemonName = pokemonName.lowercase() // Puede que necesites un formateo más complejo
    // dependiendo de los nombres de archivo del servidor.
    val imageUrl = if(!esShiny) "$BASE_GIF_URL$formattedPokemonName.gif" else "$SHINY_GIF_URL$formattedPokemonName.gif"

    LaunchedEffect(imageUrl) { // Recargar si la URL cambia
        isLoading = true
        Log.d("PokemonGif", "Cargando Pokémon desde: $imageUrl")
        drawable = withContext(Dispatchers.IO) {
            loadDrawableFromUrl(context, imageUrl, imageLoader)
        }
        Log.d("PokemonGif", "Drawable para $pokemonName: ${drawable?.javaClass?.name}")
        if (drawable is Animatable) {
            Log.d("PokemonGif", "$pokemonName ES Animatable.")
        } else {
            Log.d("PokemonGif", "$pokemonName NO es Animatable. URL podría ser incorrecta o no es un GIF animado.")
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

