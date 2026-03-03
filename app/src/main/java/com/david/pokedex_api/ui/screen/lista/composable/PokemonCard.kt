package com.david.pokedex_api.ui.screen.lista.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "scale"
    )

    val type1 = pokemonSummary.types.getOrNull(0)
    val type2 = pokemonSummary.types.getOrNull(1)
    
    val color1 = if (type1 != null) getPokemonTypeColorClear(type1) else Color.Gray
    val color2 = if (type2 != null) getPokemonTypeColorClear(type2) else color1
    
    val gradientPair = if (type1 != null) {
        getPokemonTypeGradientColors(type1)
    } else {
        Color.Gray to Color.DarkGray
    }

    val backgroundBrush = remember(pokemonSummary.types, color1, color2, gradientPair) {
        if (pokemonSummary.types.size >= 2) {
            Brush.linearGradient(listOf(color1, color2))
        } else {
            Brush.linearGradient(listOf(gradientPair.first, gradientPair.second))
        }
    }

    val isDark = remember(pokemonSummary.types) {
        pokemonSummary.types.isNotEmpty() && esTipoColorOscuro(pokemonSummary.types[0])
    }
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    val dShape = remember { DShape() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .pointerInput(pokemonSummary.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            awaitRelease()
                            onItemClick(pokemonSummary.name)
                        } finally {
                            isPressed = false
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.background(backgroundBrush).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(90.dp).background(Color.White.copy(0.3f), dShape).clip(dShape)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("https://resource.pokemon-home.com/battledata/img/pokei128/icon${pokemonSummary.id.toString().padStart(4, '0')}_f00_s0.png")
                            .crossfade(200)
                            .diskCacheKey("pkmn_${pokemonSummary.id}")
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = adaptaNombre(transformPokemonNameToResourceName(pokemonSummary.name)),
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "#${pokemonSummary.id.toString().padStart(3, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pokemonSummary.types.forEach { type ->
                            PokemonTypeChip(typeName = type, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

class DShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, size.height)
            lineTo(size.width * 0.75f, size.height)
            quadraticBezierTo(size.width, size.height * 0.75f, size.width, size.height * 0.5f)
            quadraticBezierTo(size.width, size.height * 0.25f, size.width * 0.75f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
