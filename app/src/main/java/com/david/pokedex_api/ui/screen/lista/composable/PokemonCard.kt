package com.david.pokedex_api.ui.screen.lista.composable

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.LocalAnimatedVisibilityScope
import com.david.pokedex_api.LocalSharedTransitionScope
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeGradientColors
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.adaptaNombre
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.transformPokemonNameToResourceName
import com.david.pokedex_api.ui.theme.rojo_pokeball
import kotlinx.coroutines.flow.first

private val CardDShape = DShape()

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PokemonListItemCard(
    pokemonSummary: PokemonSummary,
    isRecalled: Boolean = false,
    onRecallAndNavigate: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    var isPressed by remember { mutableStateOf(false) }
    var isRecalling by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "scale"
    )

    // Animaciones solo activas cuando el card esta en recall/return
    val isAnimating = isRecalling || isRecalled
    val contentScale by animateFloatAsState(
        targetValue = if (isAnimating) 0f else 1f,
        animationSpec = tween(if (isRecalling) 250 else 300),
        label = "contentScale"
    )
    val showShimmer = isAnimating && contentScale < 0.95f && contentScale > 0.05f
    val silhouetteFilter = if (showShimmer) {
        val color = lerp(rojo_pokeball, Color.White, contentScale)
        ColorFilter.tint(color, BlendMode.SrcAtop)
    } else null

    val pokeballAlpha by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(200),
        label = "pokeballAlpha"
    )

    // Recall completo → navegar (una sola vez)
    LaunchedEffect(isRecalling) {
        if (isRecalling) {
            snapshotFlow { contentScale }.first { it < 0.05f }
            onRecallAndNavigate()
        }
    }

    val type1 = pokemonSummary.types.getOrNull(0)
    val type2 = pokemonSummary.types.getOrNull(1)
    val color1 = if (type1 != null) getPokemonTypeColorClear(type1) else Color.Gray
    val color2 = if (type2 != null) getPokemonTypeColorClear(type2) else color1
    val gradientPair = if (type1 != null) getPokemonTypeGradientColors(type1) else Color.Gray to Color.DarkGray

    val backgroundBrush = remember(pokemonSummary.types, color1, color2, gradientPair) {
        if (pokemonSummary.types.size >= 2) Brush.linearGradient(listOf(color1, color2))
        else Brush.linearGradient(listOf(gradientPair.first, gradientPair.second))
    }

    val isDark = remember(pokemonSummary.types) {
        pokemonSummary.types.isNotEmpty() && esTipoColorOscuro(pokemonSummary.types[0])
    }
    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .pointerInput(pokemonSummary.id) {
                detectTapGestures(
                    onPress = {
                        if (!isRecalling && !isRecalled) {
                            isPressed = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            try {
                                awaitRelease()
                                isRecalling = true
                            } finally {
                                if (!isRecalling) isPressed = false
                            }
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Contenido del card
            // graphicsLayer solo si esta animando (evita overhead en 99% de cards)
            val contentModifier = if (isAnimating) {
                Modifier.graphicsLayer {
                    scaleX = contentScale; scaleY = contentScale
                    alpha = contentScale
                    transformOrigin = TransformOrigin(0.06f, 0.5f)
                }
            } else Modifier

            Box(
                modifier = Modifier
                    .background(backgroundBrush)
                    .fillMaxWidth()
                    .then(contentModifier)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(90.dp).background(Color.White.copy(0.3f), CardDShape).clip(CardDShape)) {
                        val homeUrl = remember(pokemonSummary.id) {
                            "https://resource.pokemon-home.com/battledata/img/pokei128/icon${pokemonSummary.id.toString().padStart(4, '0')}_f00_s0.png"
                        }
                        val primaryUrl = if (pokemonSummary.id < 10000) homeUrl else (pokemonSummary.spriteUrl ?: homeUrl)
                        val fallbackUrl = if (pokemonSummary.id < 10000) pokemonSummary.spriteUrl else pokemonSummary.fallbackSpriteUrl
                        var imageUrl by remember(pokemonSummary.id) { mutableStateOf(primaryUrl) }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .memoryCacheKey("pkmn_${pokemonSummary.id}")
                                .diskCacheKey("pkmn_${pokemonSummary.id}")
                                .size(128)
                                .listener(onError = { _, _ ->
                                    if (imageUrl == primaryUrl && fallbackUrl != null && fallbackUrl != primaryUrl) {
                                        imageUrl = fallbackUrl
                                    }
                                })
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            colorFilter = silhouetteFilter
                        )

                        // Shimmer solo durante animacion activa
                        if (showShimmer) {
                            val burstAlpha = (1f - contentScale).coerceIn(0f, 1f)
                            val ringCenter = (1f - contentScale) * 0.55f
                            val ringThickness = 0.25f
                            val innerEdge = (ringCenter - ringThickness / 2f).coerceAtLeast(0f)
                            val outerEdge = (ringCenter + ringThickness / 2f).coerceAtMost(0.95f)

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.radialGradient(
                                            colorStops = arrayOf(
                                                0.0f to Color.White.copy(alpha = 0.9f * burstAlpha),
                                                innerEdge.coerceAtLeast(0.01f) to rojo_pokeball.copy(alpha = 0.2f * burstAlpha),
                                                ringCenter.coerceAtLeast(0.03f) to rojo_pokeball.copy(alpha = 1f * burstAlpha),
                                                outerEdge to rojo_pokeball.copy(alpha = 0.3f * burstAlpha),
                                                1.0f to Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
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

            // Pokeball: solo compuesta cuando este card esta animando
            if (isAnimating && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Image(
                        painter = painterResource(id = R.drawable.pokeball_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = 27.dp)
                            .graphicsLayer { alpha = pokeballAlpha }
                            .sharedElement(
                                rememberSharedContentState(key = "pokeball-transit-${pokemonSummary.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
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
            quadraticTo(size.width, size.height * 0.75f, size.width, size.height * 0.5f)
            quadraticTo(size.width, size.height * 0.25f, size.width * 0.75f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
