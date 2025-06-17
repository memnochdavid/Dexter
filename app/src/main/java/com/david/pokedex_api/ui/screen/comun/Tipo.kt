package com.david.pokedex_api.ui.screen.comun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.R
import com.david.pokedex_api.ui.theme.*

// Un Composable simple para mostrar un "chip" de tipo de Pokémon
// Puedes personalizar esto mucho más con colores específicos por tipo, etc.
@Composable
fun PokemonTypeChip(typeName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface( // Usamos Surface para poder darle forma y color de fondo
            shape = RoundedCornerShape(6.dp),
            color = getPokemonTypeColorTypeChip(typeName),//.copy(alpha = 0.2f), // Color de fondo semi-transparente
            contentColor = blanco5, // Color del texto
//            border = BorderStroke( // <-- AÑADIR BORDE AQUÍ
//                width = 1.dp, // Grosor del borde
//                color = getPokemonTypeColor(typeName) // Color del borde (puedes usar el mismo color base o uno diferente)
//            )
        ) {
            val iconResId = getPokemonTypeToIcon(typeName)
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = typeName, // Buena práctica para accesibilidad
                modifier = Modifier // Aquí van los modificadores que tenías en tu código original
                    // Por ejemplo:
                    .size(20.dp)
                // .align(Alignment.CenterStart) // o lo que necesites
            )
            Spacer(modifier = Modifier.width(4.dp)) // Espacio entre el icono y el texto
            Text(
                text = pokemonTypeNameTranslator(typeName.replaceFirstChar { it.titlecase() }),
                fontSize = 10.sp,
                color = blanco80,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .fillMaxWidth(1f),
                fontWeight = FontWeight.Bold
            )

        }
    }
}

@Composable
fun PokemonTypeChipSmall( // Versión más pequeña de tu PokemonTypeChip
    typeName: String,
    modifier: Modifier = Modifier,
    colorClaro : Boolean
) {
    val typeColor = if(colorClaro) getPokemonTypeColorTypeChip(typeName = typeName) else getPokemonTypeColorTypeChip(typeName = typeName)
    // Determinar el color del texto para contraste (simple ejemplo)
    val contentColor = if (isDarkColor(typeColor)) Color.White else Color.Black

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24)) // Forma de píldora o círculo
            .background(typeColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = pokemonTypeNameTranslator(typeName).replaceFirstChar { it.titlecase() },
            color = contentColor,
            style = MaterialTheme.typography.labelSmall, // Texto más pequeño
            fontWeight = FontWeight.Bold
        )
    }
}

// Función auxiliar para determinar si un color es oscuro (para contraste de texto)
// Esta es una implementación muy básica. Hay algoritmos más precisos.
fun isDarkColor(color: Color): Boolean {
    val darkness = 1 - (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return darkness >= 0.5
}

fun pokemonTypeNameTranslator(typeName: String): String {
    return when (typeName.lowercase()) {
        "grass" -> "Planta"
        "fire" -> "Fuego"
        "water" -> "Agua"
        "bug" -> "Bicho"
        "normal" -> "Normal"
        "poison" -> "Veneno"
        "electric" -> "Eléctrico"
        "ground" -> "Tierra"
        "fairy" -> "Hada"
        "fighting" -> "Lucha"
        "psychic" -> "Psíquico"
        "rock" -> "Roca"
        "ghost" -> "Fantasma"
        "ice" -> "Hielo"
        "dragon" -> "Dragón"
        "dark" -> "Siniestro"
        "steel" -> "Acero"
        "flying" -> "Volador"

        else -> {"Sin tipo"}
    }
}


// Función de utilidad para obtener un color basado en el tipo de Pokémon
// ¡Esta es una implementación básica! Puedes expandirla mucho.
fun getPokemonTypeColor(typeName: String): Color {
    return when (typeName.lowercase()) {
        "grass" -> color_planta_card
        "fire" -> color_fuego_card
        "water" -> color_agua_card
        "bug" -> color_bicho_card
        "normal" -> color_normal_card
        "poison" -> color_veneno_card
        "electric" -> color_electrico_card
        "ground" -> color_tierra_card
        "fairy" -> color_hada_card
        "fighting" -> color_lucha_card
        "psychic" -> color_psiquico_card
        "rock" -> color_roca_card
        "ghost" -> color_fantasma_card
        "ice" -> color_hielo_card
        "dragon" -> color_dragon_card
        "dark" -> color_siniestro_card
        "steel" -> color_acero_card
        "flying" -> color_volador_card
        else -> Color.LightGray // Color por defecto
    }
}
fun getPokemonTypeColorClear(typeName: String): Color {
    return when (typeName.lowercase()) {
        "grass" -> color_planta_card_foto
        "fire" -> color_fuego_card_foto
        "water" -> color_agua_card_foto
        "bug" -> color_bicho_card_foto
        "normal" -> color_normal_card_foto
        "poison" -> color_veneno_card_foto
        "electric" -> color_electrico_card_foto
        "ground" -> color_tierra_card_foto
        "fairy" -> color_hada_card_foto
        "fighting" -> color_lucha_card_foto
        "psychic" -> color_psiquico_card_foto
        "rock" -> color_roca_card_foto
        "ghost" -> color_fantasma_card_foto
        "ice" -> color_hielo_card_foto
        "dragon" -> color_dragon_card_foto
        "dark" -> color_siniestro_card_foto
        "steel" -> color_acero_card_foto
        "flying" -> color_volador_card_foto
        else -> Color.LightGray // Color por defecto
    }
}
fun getPokemonTypeToIcon(typeName: String): Int {
    return when (typeName.lowercase()) {
        "grass" -> R.drawable.planta2
        "fire" -> R.drawable.fuego2
        "water" -> R.drawable.agua2
        "bug" -> R.drawable.bicho2
        "normal" -> R.drawable.normal2
        "poison" -> R.drawable.veneno2
        "electric" -> R.drawable.electrico2
        "ground" -> R.drawable.tierra2
        "fairy" -> R.drawable.hada2
        "fighting" -> R.drawable.lucha2
        "psychic" -> R.drawable.psiquico2
        "rock" -> R.drawable.roca2
        "ghost" -> R.drawable.fantasma2
        "ice" -> R.drawable.hielo2
        "dragon" -> R.drawable.dragon2
        "dark" -> R.drawable.siniestro2
        "steel" -> R.drawable.acero2
        "flying" -> R.drawable.volador2
        else -> {0}
    }
}
fun getTextColorForTypeBackground(backgroundColor: Color): Color {
    // Fórmula de luminancia simple para determinar si el fondo es claro u oscuro
    val red = backgroundColor.red * 255
    val green = backgroundColor.green * 255
    val blue = backgroundColor.blue * 255
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
    return if (luminance > 0.5) Color.Black else Color.White // Texto negro sobre fondos claros, blanco sobre oscuros
}
@Composable
fun getPokemonTypeGradientColors(typeName: String): Pair<Color, Color> {
    return when (typeName.lowercase()) { // Convertimos a minúsculas para asegurar la coincidencia
        "grass" -> color_planta_card_foto to color_planta_card
        "water" -> color_agua_card_foto to color_agua_card
        "fire" -> color_fuego_card_foto to color_fuego_card
        "fighting" -> color_lucha_card_foto to color_lucha_card
        "poison" -> color_veneno_card_foto to color_veneno_card
        "steel" -> color_acero_card_foto to color_acero_card
        "bug" -> color_bicho_card_foto to color_bicho_card
        "dragon" -> color_dragon_card_foto to color_dragon_card
        "electric" -> color_electrico_card_foto to color_electrico_card
        "fairy" -> color_hada_card_foto to color_hada_card
        "ice" -> color_hielo_card_foto to color_hielo_card
        "psychic" -> color_psiquico_card_foto to color_psiquico_card
        "rock" -> color_roca_card_foto to color_roca_card
        "ground" -> color_tierra_card_foto to color_tierra_card
        "dark" -> color_siniestro_card_foto to color_siniestro_card
        "normal" -> color_normal_card_foto to color_normal_card
        "flying" -> color_volador_card_foto to color_volador_card
        "ghost" -> color_fantasma_card_foto to color_fantasma_card
        else -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.surface // Color por defecto
    }
}
fun getPokemonTypeColorTypeChip(typeName: String): Color {
    return when (typeName.lowercase()) {
        "grass" -> color_planta_light
        "fire" -> color_fuego_light
        "water" -> color_agua_light
        "bug" -> color_bicho_light
        "normal" -> color_normal_light
        "poison" -> color_veneno_light
        "electric" -> color_electrico_light
        "ground" -> color_tierra_light
        "fairy" -> color_hada_light
        "fighting" -> color_lucha_light
        "psychic" -> color_psiquico_light
        "rock" -> color_roca_light
        "ghost" -> color_fantasma_light
        "ice" -> color_hielo_light
        "dragon" -> color_dragon_light
        "dark" -> color_siniestro_light
        "steel" -> color_acero_light
        "flying" -> color_volador_light
        else -> Color.LightGray // Color por defecto
    }
}


fun esTipoColorOscuro(nombreTipo:String): Boolean {
    return when (nombreTipo.lowercase()) {
        "dark", "fire", "ghost", "poison", "dragon", "ground" -> true
        else -> false
    }
}

val ALL_POKEMON_TYPES = listOf(
    "normal", "fire", "water", "electric", "grass", "ice",
    "fighting", "poison", "ground", "flying", "psychic", "bug",
    "rock", "ghost", "dragon", "dark", "steel", "fairy"
)

const val NO_TYPE_SELECTED = "Sin tipo" // Constante para representar "ningún tipo"