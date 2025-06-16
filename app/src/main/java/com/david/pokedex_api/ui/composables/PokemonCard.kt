package com.david.pokedex_api.ui.composables

import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.util.getPokemonSpeciesColor

@Composable
fun PokemonListItemCard(
    pokemonSummary: PokemonSummary,
    onItemClick: (String) -> Unit,
) {
    val context = LocalContext.current

    // Determinar el pincel de fondo (Brush) o color sólido
    val backgroundBrush: Brush
    val cardActualContainerColor: Color // Color para la Card si no es gradiente
    val colorPokemon = getPokemonSpeciesColor(pokemonSummary.colorName)

    if (pokemonSummary.types.size == 2) {
        val color1 = getPokemonTypeColorClear(pokemonSummary.types[0])
        val color2 = getPokemonTypeColorClear(pokemonSummary.types[1])
        backgroundBrush = Brush.linearGradient(
            colors = listOf(color1, color2)
            // Puedes ajustar start y end para la dirección del gradiente
            // start = Offset.Zero, end = Offset.Infinite
        )
        cardActualContainerColor =
            Color.Transparent // La Card será transparente para mostrar el Box con gradiente
    } else if (pokemonSummary.types.isNotEmpty()) {
        val solidColor = getPokemonTypeColorClear(pokemonSummary.types[0])
        backgroundBrush = SolidColor(solidColor) // O simplemente no usar un Box con brush
        cardActualContainerColor = solidColor
    } else {
        val defaultColor = MaterialTheme.colorScheme.surface
        backgroundBrush = SolidColor(defaultColor)
        cardActualContainerColor = defaultColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { onItemClick(pokemonSummary.name) },
        colors = CardDefaults.cardColors(
            containerColor = cardActualContainerColor // Si no es gradiente, este color se usa.
            // Si es gradiente, este es transparente.
        ),
        // elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // border = BorderStroke(...) // Si necesitas borde
    ) {
        // Usamos un Box para aplicar el gradiente si es necesario.
        // Si no es gradiente, cardActualContainerColor ya pintó la Card.
        // Si es gradiente, este Box pinta el fondo.
        Box(
            modifier = Modifier
                .then(
                    if (pokemonSummary.types.size == 2) { // Aplicar gradiente solo si hay dos tipos
                        Modifier.background(brush = backgroundBrush)
                    } else {
                        Modifier // No se necesita fondo adicional si es color sólido
                    }
                )
                .fillMaxSize() // Asegura que el Box llene la Card
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 12.dp) // Añadido padding vertical
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp) // Tamaño del contenedor de la imagen
                        .background(
                            Color.White.copy(alpha = 0.5f), shape = DShape()
                        ) // Color de fondo de la D y la forma
                        .clip(DShape()) // Asegúrate de recortar el contenido a la forma
                ) {

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(pokemonSummary.spriteUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${pokemonSummary.name} sprite",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally // Centrar contenido de la columna
                ) {
                    Text(
                        text = pokemonSummary.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        // Asegúrate de que CardBorder esté definido o usa un color de MaterialTheme
                        text = "#${pokemonSummary.id.toString().padStart(3, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Ejemplo, usa tu color CardBorder
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (pokemonSummary.types.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp,Alignment.CenterHorizontally), // Centrar los chips si el espacio lo permite
                        ) {
                            pokemonSummary.types.forEach { typeName ->
                                PokemonTypeChip(
                                    typeName = typeName,
                                    // El weight en los chips puede ser complicado si quieres que se centren
                                    // y no siempre llenen todo el espacio.
                                    // Considera no usar weight o ajustar su lógica.
                                    modifier = Modifier.weight(0.8f) // Cada chip toma igual espacio disponible
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class DShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            // Empieza en la esquina superior izquierda
            moveTo(0f, 0f)
            // Línea hacia la esquina inferior izquierda
            lineTo(0f, size.height)
            // Línea hacia la esquina inferior derecha (más ancha)
            lineTo(size.width * 0.8f, size.height) // <--- AJUSTADO para mayor anchura en la base
            // Curva hacia la parte superior derecha
            quadraticBezierTo(
                size.width, size.height * 0.75f, // Punto de control 1
                size.width, size.height * 0.5f   // Punto final del primer segmento de la curva
            )
            quadraticBezierTo(
                size.width,
                size.height * 0.25f, // Punto de control 2
                size.width * 0.8f,
                0f            // <--- AJUSTADO Punto final del segundo segmento (esquina superior de la curva)
            )
            // Línea de vuelta a la esquina superior izquierda (para cerrar la parte recta de la D)
            // Esta línea ahora es más corta porque la curva empieza más a la derecha
            lineTo(0f, 0f)
            close() // Cierra el camino
        }
        return Outline.Generic(path)
    }
}