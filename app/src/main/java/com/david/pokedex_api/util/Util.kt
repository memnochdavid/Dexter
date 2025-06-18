package com.david.pokedex_api.util


import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat.getDrawable
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.david.pokedex_api.api.model.TypeResponseSlot
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.david.pokedex_api.R
import com.david.pokedex_api.ui.theme.*
import com.google.accompanist.drawablepainter.rememberDrawablePainter


//globales
var muestraDesc by mutableStateOf(false)
var muestraStats by mutableStateOf(false)
var muestraEvos by mutableStateOf(false)
var vistaDatos by mutableStateOf("")




//utilidades
fun extractDominantColor(bitmap: Bitmap?, defaultColor: Color): Color {
    if (bitmap == null) return defaultColor
    // 1. Crea un Palette.Builder a partir del Bitmap
    val builder = Palette.from(bitmap)

    // OPCIONAL: Ajustar el número máximo de colores (clusters)
    // Un número más bajo podría agrupar colores más dispares, un número más alto
    // podría separar colores muy similares en diferentes clusters.
    // El valor por defecto es 16.
    // builder.maximumColorCount(16) // Puedes experimentar aquí

    // 2. Genera la paleta. Esto hace el trabajo pesado de analizar los píxeles.
    val palette = builder.generate()

    // 3. Obtén el dominantSwatch. Este es el swatch con la mayor población de píxeles.
    val dominantSwatch = palette.dominantSwatch

    // 4. Si se encontró un dominantSwatch, usa su color (rgb).
    //    Si no (muy raro para una imagen válida), usa el defaultColor.
    return dominantSwatch?.rgb?.let { Color(it) } ?: defaultColor
}

suspend fun String?.toBitmap(context: Context, imageLoader: ImageLoader): Bitmap? {
    if (this == null) return null
    return withContext(Dispatchers.IO) { // Ejecutar en un hilo de fondo
        try {
            val request = ImageRequest.Builder(context)
                .data(this@toBitmap)
                .allowHardware(false) // Necesario para que Palette funcione correctamente
                .build()
            val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
            (result as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            // Manejar excepciones de carga o conversión
            null
        }
    }
}
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f,
    durationMillis: Int = 450
): Brush {
    val alpha by animateFloatAsState(
        targetValue = if (showShimmer) 1f else 0f,
        animationSpec = tween(durationMillis), label = ""
    )

    return if (showShimmer) {
        val shimmerColors = listOf(
            rojo_pokeball.copy(alpha = 0.6f * alpha),
            colorResource(id = R.color.white).copy(alpha = 0.2f * alpha),
            rojo_pokeball.copy(alpha = 0.6f * alpha),
        )

        val transition = rememberInfiniteTransition(label = "")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis), repeatMode = RepeatMode.Reverse
            ), label = ""
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}





// Data class para representar una interacción de un tipo atacante
data class TypeInteraction(
    val attackingType: String, // El tipo que está atacando
    val multiplier: Double // 0.0, 0.25, 0.5, 1.0, 2.0, 4.0
)

suspend fun getCombinedDefensiveInteractions(
    pokemonTypes: List<TypeResponseSlot>, // Los tipos del Pokémon actual
    apiService: PokeApiService // Asegúrate que es tu interfaz de servicio actualizada
): List<TypeInteraction> {
    if (pokemonTypes.isEmpty()) return emptyList()

    val typeDetailsResponses = pokemonTypes.mapNotNull { typeSlot ->
        try {
            // CAMBIO AQUÍ: Usa el nuevo nombre de la función del servicio
            val response = apiService.getTypeDetailsByName(typeSlot.type.name)
            //                            ^^^^^^^^^^^^^^^^^^^
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            println("Error fetching type details for ${typeSlot.type.name}: ${e.message}")
            null
        }
    }

    if (typeDetailsResponses.isEmpty()) return emptyList()

    val combinedInteractions = mutableMapOf<String, Double>()

    ALL_POKEMON_TYPES.forEach { attackingType ->
        combinedInteractions[attackingType] = 1.0
    }

    typeDetailsResponses.forEach { typeDetail ->
        // Asumiendo que typeDetail es de tipo TypeDetailResponse y tiene 'damageRelations'
        // y que ALL_POKEMON_TYPES es tu lista de constantes de todos los tipos
        typeDetail.damageRelations.noDamageFrom.forEach { attackingTypeInfo ->
            combinedInteractions[attackingTypeInfo.name] = (combinedInteractions[attackingTypeInfo.name] ?: 1.0) * 0.0
        }
        typeDetail.damageRelations.halfDamageFrom.forEach { attackingTypeInfo ->
            combinedInteractions[attackingTypeInfo.name] = (combinedInteractions[attackingTypeInfo.name] ?: 1.0) * 0.5
        }
        typeDetail.damageRelations.doubleDamageFrom.forEach { attackingTypeInfo ->
            combinedInteractions[attackingTypeInfo.name] = (combinedInteractions[attackingTypeInfo.name] ?: 1.0) * 2.0
        }
    }

    // La normalización/doble comprobación que tenías antes para el 0.0 ya está implícita
    // en la multiplicación directa. Si alguna vez se multiplica por 0.0, el resultado será 0.0.

    return combinedInteractions.map { TypeInteraction(it.key, it.value) }
        .sortedBy { ALL_POKEMON_TYPES.indexOf(it.attackingType) }
}

fun formatPokemonName(apiName: String): String {
    if (apiName.isBlank()) return "Desconocido"

    val parts = apiName.lowercase().split('-')
    if (parts.isEmpty()) return apiName.replaceFirstChar { it.titlecase() }

    val regionalFormsPrefixMap = mapOf(
        "alola" to "de Alola",
        "galar" to "de Galar",
        "hisui" to "de Hisui",
        "paldea" to "de Paldea"
    )

    var baseNameString: String
    var varietySuffixParts = mutableListOf<String>()
    var regionalSuffix = ""

    if (parts.size > 1 && regionalFormsPrefixMap.containsKey(parts.first())) {
        regionalSuffix = regionalFormsPrefixMap[parts.first()] ?: ""
        baseNameString = parts.drop(1).joinToString("-") { it.replaceFirstChar { char -> char.titlecase() } }
        // Para casos como "paldea-tauros-aqua-breed", varietySuffixParts se llenaría después si es necesario
        // Esta simplificación actual asume que si hay un prefijo regional, no hay más sufijos complejos procesados aquí.
    } else {
        baseNameString = parts.first().replaceFirstChar { it.titlecase() }
        val rawVarietyParts = parts.drop(1)

        var i = 0
        while (i < rawVarietyParts.size) {
            val part = rawVarietyParts[i]
            when (part) {
                "mega" -> varietySuffixParts.add("Mega")
                "gmax", "gigantamax" -> varietySuffixParts.add("G-Max")
                "x" -> varietySuffixParts.add("X")
                "y" -> varietySuffixParts.add("Y")
                // Formas regionales como sufijo (ej. raticate-alola)
                "alola" -> if (regionalSuffix.isEmpty()) regionalSuffix = "Alola" else varietySuffixParts.add("Alola")
                "galar" -> if (regionalSuffix.isEmpty()) regionalSuffix = "Galar" else varietySuffixParts.add("Galar")
                "hisui" -> if (regionalSuffix.isEmpty()) regionalSuffix = "Hisui" else varietySuffixParts.add("Hisui")
                "paldea" -> if (regionalSuffix.isEmpty()) regionalSuffix = "Paldea" else varietySuffixParts.add("Paldea")

                "totem" -> varietySuffixParts.add("Totem")
                "own-tempo" -> varietySuffixParts.add("Own Tempo")
                "starter" -> varietySuffixParts.add("Starter")
                // Casos específicos de Pikachu
                "world-cap" -> varietySuffixParts.add("World Cap")
                "partner-cap" -> varietySuffixParts.add("Partner Cap")
                "original-cap", "hoenn-cap", "sinnoh-cap", "unova-cap", "kalos-cap", "alola-cap" ->
                    varietySuffixParts.add(part.split('-').joinToString(" ") { it.replaceFirstChar { char -> char.titlecase()} })

                "average", "small", "large", "super" ->
                    if (baseNameString.equals("Pumpkaboo", true) || baseNameString.equals("Gourgeist", true))
                        varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })
                    else if (baseNameString.equals("Indeedee", true) && (part == "male" || part == "female")) {
                        // No hacer nada, el nombre base es suficiente o se maneja por género
                    }
                    else varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })


                "attack", "defense", "speed" ->
                    if (baseNameString.equals("Deoxys", true))
                        varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })
                    else varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })

                // Formas que son solo el nombre capitalizado
                "standard", "altered", "land", "sky", "origin", "normal", "zen", "incarnate", "therian",
                "aria", "pirouette", "shield", "blade", "sunny", "rainy", "snowy",
                "attack", "defense", "speed", "female", "male", // 'male' y 'female' pueden ser redundantes si el nombre base ya los implica
                "crowned", "hero", "dada", "eternal", "armored", "red-striped", "blue-striped", "white-striped",
                "hangry", "amped", "low-key", "ice-rider", "shadow-rider", "rapid-strike", "single-strike",
                "busted", "disguised", "complete", "10", "50", // Para Zygarde, etc.
                "one", "two", "three", // Para Maushold
                "combat-breed", "blaze-breed", "aqua-breed", // Para Tauros Paldea
                "active", "school", "solo", "core", "meteor" // Minior
                    -> {
                    // Evitar duplicar el nombre base si está en la variedad, ej. "Pikachu-Pikachu-Starter"
                    if (!part.equals(baseNameString, ignoreCase = true)) {
                        varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })
                    }
                }
                // Palabras comunes que no queremos añadir si son la única parte de la variedad
                // o si parecen ser parte del nombre base ya procesado
                "form", "mode", "style", "size", "breed" -> { /* Ignorar o manejar específicamente si es necesario */ }

                else -> {
                    // Si no es un caso especial y no es el nombre base, añadirlo.
                    if (!part.equals(baseNameString, ignoreCase = true)) {
                        varietySuffixParts.add(part.replaceFirstChar { it.titlecase() })
                    }
                }
            }
            i++
        }
    }

    val finalVarietyString = varietySuffixParts.joinToString(" ").trim()

    return buildString {
        if (finalVarietyString.startsWith("Mega") || finalVarietyString.startsWith("G-Max")) {
            append(finalVarietyString)
            append(" ")
            append(baseNameString)
        } else {
            append(baseNameString)
            if (finalVarietyString.isNotEmpty()) {
                append(" ")
                append(finalVarietyString)
            }
        }
        if (regionalSuffix.isNotEmpty()) {
            if (regionalFormsPrefixMap.containsValue(regionalSuffix)) { // "de Alola", "de Galar", etc.
                append(" ")
                append(regionalSuffix)
            } else { // "Alola", "Galar" como sufijo directo
                append(" ")
                append(regionalSuffix)
            }
        }
    }.trim()
}

fun getPokemonSpeciesColor(colorName: String?): Color {
    return when (colorName?.lowercase()) {
        "black" -> black_api
        "blue" -> blue_api
        "brown" -> brown_api
        "gray" -> gray_api
        "green" -> green_api
        "pink" -> pink_api
        "purple" -> purple_api
        "red" -> red_api
        "white" -> white_api
        "yellow" -> yellow_api


        // Añade más colores según los que devuelve la API y tus preferencias
        else -> Color.LightGray // Un color por defecto si el nombre no se reconoce o es null
    }
}

@Composable
fun GifAnimado(drawableId: Int, modifier: Modifier = Modifier) {
    Image(
        painter = rememberDrawablePainter(
            drawable = getDrawable(
                LocalContext.current,
                drawableId
            )
        ),
        contentDescription = "Loading animation",
        modifier = modifier
    )
}


fun formatApiName(apiName: String?): String {
    return apiName?.replace("-", " ")?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    } ?: "Desconocido" // O devuelve una cadena vacía o maneja el nulo como prefieras
}

// También necesitarás formatPokemonName, que es similar pero puede tener ligeras diferencias
// si quieres una capitalización específica para nombres de Pokémon.
//fun formatPokemonName(apiName: String?): String {
//    return apiName?.replace("-", " ")?.replaceFirstChar {
//        if (it.isLowerCase()) it.titlecase() else it.toString()
//    } ?: "Pokémon Desconocido"
//}

fun translateEvolutionTrigger(triggerApiName: String?): String {
    if (triggerApiName.isNullOrBlank()) {
        return "Desconocido"
    }

    // Usa Locale.getDefault() de java.util.Locale
    return when (triggerApiName.lowercase()) {
        "level-up" -> "Nivel"
        "trade" -> "Intercambio"
        "use-item" -> "Usar objeto"
        "shed" -> "Mudar piel"
        "spin" -> "Girar"
        "tower-of-darkness" -> "Torre de la Oscuridad"
        "tower-of-waters" -> "Torre de las Aguas"
        "three-critical-hits" -> "3 Golpes Críticos"
        "take-damage" -> "Recibir daño"
        "other" -> "Otro método"
        "agile-style-move" -> "Mov. Estilo Ágil"
        "strong-style-move" -> "Mov. Estilo Fuerte"
        "recoil-damage" -> "Daño de retroceso"
        else -> {
            triggerApiName.split('-').joinToString(" ") { part ->
                part.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
        }
    }
}
