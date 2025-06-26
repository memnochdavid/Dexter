package com.david.pokedex_api.ui.screen.lista.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeGradientColors
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.adaptaNombre
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.transformPokemonNameToResourceName

@Composable
fun PokemonListItemCard(
    pokemonSummary: PokemonSummary,
    onItemClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Modifica la animación aquí
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 50), // Duración muy corta para un efecto más rápido
        label = "scaleAnimation"
    )

    // ... (el resto de tu lógica para backgroundBrush y cardActualContainerColor)
    val backgroundBrush: Brush
    val cardActualContainerColor: Color

    if (pokemonSummary.types.size == 2) {
        val color1 = getPokemonTypeColorClear(pokemonSummary.types[0])
        val color2 = getPokemonTypeColorClear(pokemonSummary.types[1])
        backgroundBrush = Brush.linearGradient(
            colors = listOf(color1, color2)
        )
        cardActualContainerColor = Color.Transparent
    } else if (pokemonSummary.types.isNotEmpty()) {
        val typeName = pokemonSummary.types[0]
        val (gradientStartColor, gradientEndColor) = getPokemonTypeGradientColors(typeName)
        backgroundBrush = Brush.linearGradient(
            colors = listOf(gradientStartColor, gradientEndColor)
        )
        cardActualContainerColor = Color.Transparent
    } else {
        val defaultColor = MaterialTheme.colorScheme.surface
        backgroundBrush = SolidColor(defaultColor)
        cardActualContainerColor = defaultColor
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleX = scaleFactor, scaleY = scaleFactor)
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onItemClick(pokemonSummary.name)
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = cardActualContainerColor
        ),
    ) {
        // ... (el resto de tu Composable)
        Box(
            modifier = Modifier
                .then(
                    if (pokemonSummary.types.isNotEmpty()) {
                        Modifier.background(brush = backgroundBrush)
                    } else {
                        Modifier
                    }
                )
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.5f), shape = DShape()
                        )
                        .clip(DShape())
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
//                        text = pokemonSummary.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        text = adaptaNombre(transformPokemonNameToResourceName(pokemonSummary.name)),
                        color = if (pokemonSummary.types.isNotEmpty() && esTipoColorOscuro(
                                pokemonSummary.types[0]
                            )
                        ) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${pokemonSummary.id.toString().padStart(3, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (pokemonSummary.types.isNotEmpty() && esTipoColorOscuro(
                                pokemonSummary.types[0]
                            )
                        ) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (pokemonSummary.types.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterHorizontally
                            ),
                        ) {
                            pokemonSummary.types.forEach { typeName ->
                                PokemonTypeChip(
                                    typeName = typeName,
                                    modifier = Modifier.weight(0.8f)
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

