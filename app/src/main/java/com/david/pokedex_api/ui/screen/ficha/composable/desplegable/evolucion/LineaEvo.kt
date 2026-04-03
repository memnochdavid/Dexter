package com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.ChainLink
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.EvolutionDetail
import com.david.pokedex_api.api.viewModel.PokemonViewModel

private fun formatApiName(name: String): String =
    name.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

private fun getPokemonIdFromSpeciesUrl(url: String): Int? =
    url.trimEnd('/').split("/").lastOrNull()?.toIntOrNull()

private fun getPokemonSpriteUrl(pokemonId: Int): String =
    "https://resource.pokemon-home.com/battledata/img/pokei128/icon${pokemonId.toString().padStart(4, '0')}_f00_s0.png"

private fun getPokemonHomeSpriteUrl(speciesId: Int, formIndex: Int): String =
    "https://resource.pokemon-home.com/battledata/img/pokei128/icon${speciesId.toString().padStart(4, '0')}_f${formIndex.toString().padStart(2, '0')}_s0.png"

// ==================== ICONOS DE CONDICION ====================

private fun getItemSpriteUrl(itemName: String): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/$itemName.png"

// URL del sprite del item/objeto de la condicion (null si no hay item)
private fun getConditionItemSpriteUrl(detail: EvolutionDetail?): String? {
    if (detail == null) return null
    detail.item?.name?.let { return getItemSpriteUrl(it) }
    detail.heldItem?.name?.let { return getItemSpriteUrl(it) }
    return null
}

// Emoji para condiciones sin sprite de item
private fun getConditionIcon(detail: EvolutionDetail?): String {
    if (detail == null) return ""
    // Si tiene item o heldItem, el sprite del item se muestra en vez de emoji
    if (detail.item != null || detail.heldItem != null) return ""
    return when {
        detail.trigger.name == "trade" -> "\uD83D\uDD04" // 🔄
        detail.minHappiness != null -> "\u2764\uFE0F" // ❤️
        detail.minLevel != null -> "\u2B06\uFE0F" // ⬆️
        detail.timeOfDay.equals("day", true) -> "\u2600\uFE0F" // ☀️
        detail.timeOfDay.equals("night", true) -> "\uD83C\uDF19" // 🌙
        detail.knownMove != null -> "\u2694\uFE0F" // ⚔️
        detail.location != null -> "\uD83D\uDCCD" // 📍
        detail.gender == 1 -> "\u2640\uFE0F" // ♀️
        detail.gender == 2 -> "\u2642\uFE0F" // ♂️
        detail.needsOverworldRain -> "\uD83C\uDF27\uFE0F" // 🌧️
        detail.trigger.name == "shed" -> "\uD83D\uDC1B" // 🐛
        else -> "\u2728" // ✨
    }
}

// ==================== CONECTOR ANIMADO ====================

@Composable
fun EnergyFlowConnector(
    color: Color,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "evoFlow")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowPhase"
    )

    Canvas(modifier = modifier) {
        if (horizontal) {
            val centerY = size.height / 2f
            val startX = 0f
            val endX = size.width

            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.3f)
                    )
                ),
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            val path = Path().apply {
                moveTo(startX, centerY)
                lineTo(endX, centerY)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8.dp.toPx(), 12.dp.toPx()),
                        phase = -phase.dp.toPx()
                    ),
                    cap = StrokeCap.Round
                )
            )

            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(startX + 2.dp.toPx(), centerY))
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(endX - 2.dp.toPx(), centerY))
        } else {
            val centerX = size.width / 2f
            val startY = 0f
            val endY = size.height

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.8f),
                        color.copy(alpha = 0.3f)
                    )
                ),
                start = Offset(centerX, startY),
                end = Offset(centerX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            val path = Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX, endY)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8.dp.toPx(), 12.dp.toPx()),
                        phase = -phase.dp.toPx()
                    ),
                    cap = StrokeCap.Round
                )
            )

            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(centerX, startY + 2.dp.toPx()))
            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(centerX, endY - 2.dp.toPx()))
        }
    }
}

// ==================== CARD DE UN POKEMON EN LA CADENA ====================

@Composable
fun EvolutionStageView(
    chainLink: ChainLink,
    builtEvolutionCondition: String?,
    conditionIcon: String = "",
    itemSpriteUrl: String? = null,
    onClick: (String) -> Unit,
    viewModel: PokemonViewModel = viewModel(),
    color: Color = Color.LightGray,
    colorTexto: Color = Color.Black,
    staggerIndex: Int = 0,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pokemonId = getPokemonIdFromSpeciesUrl(chainLink.species.url)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Animacion staggered: cada card aparece con un pequeño retraso
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(staggerIndex * 120L)
        appeared = true
    }
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300),
        label = "staggerAlpha"
    )
    val appearSlide by animateFloatAsState(
        targetValue = if (appeared) 0f else 20f,
        animationSpec = tween(300),
        label = "staggerSlide"
    )

    var localizedPokemonName by remember(chainLink.species.name) {
        mutableStateOf(formatApiName(chainLink.species.name))
    }

    LaunchedEffect(chainLink.species.url, chainLink.species.name) {
        if (chainLink.species.url.isNotBlank()) {
            localizedPokemonName = viewModel.fetchLocalizedName(
                resourceUrl = chainLink.species.url,
                fallbackApiName = chainLink.species.name,
                resourceTypeHint = "pokemon-species"
            )
        }
    }

    ElevatedCard(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick(chainLink.species.name)
        },
        modifier = modifier.graphicsLayer {
            alpha = appearAlpha
            translationY = if (compact) 0f else appearSlide
            translationX = if (compact) appearSlide else 0f
        },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color)
    ) {
        if (compact) {
            // Layout compacto vertical (landscape) — se adapta al ancho disponible
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                if (pokemonId != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(getPokemonSpriteUrl(pokemonId))
                            .crossfade(true)
                            .build(),
                        contentDescription = localizedPokemonName,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(60.dp))
                }
                Text(
                    text = localizedPokemonName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colorTexto,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
                builtEvolutionCondition?.let { condition ->
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (itemSpriteUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(itemSpriteUrl)
                                    .crossfade(true)
                                    .size(64)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).padding(end = 2.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else if (conditionIcon.isNotEmpty()) {
                            Text(text = conditionIcon, fontSize = 11.sp, modifier = Modifier.padding(end = 2.dp))
                        }
                        Text(
                            text = condition,
                            fontSize = 10.sp,
                            color = colorTexto.copy(alpha = 0.7f),
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        } else {
            // Layout normal horizontal (portrait)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                if (pokemonId != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(getPokemonSpriteUrl(pokemonId))
                            .crossfade(true)
                            .build(),
                        contentDescription = localizedPokemonName,
                        modifier = Modifier.size(90.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(90.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedPokemonName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colorTexto
                    )
                    builtEvolutionCondition?.let { condition ->
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (itemSpriteUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(itemSpriteUrl)
                                        .crossfade(true)
                                        .size(64)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else if (conditionIcon.isNotEmpty()) {
                                Text(
                                    text = conditionIcon,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = condition,
                                fontSize = 12.sp,
                                color = colorTexto.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== VISTA PRINCIPAL ====================

@Composable
fun PokemonEvolutionChainView(
    evolutionChainResponse: EvolutionChainDetailResponse?,
    onPokemonClick: (pokemonName: String) -> Unit,
    viewModel: PokemonViewModel = viewModel(),
    color1: Color = MaterialTheme.colorScheme.surfaceVariant,
    color2: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var isExpanded by remember { mutableStateOf(true) }

    if (evolutionChainResponse == null) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Loading evolution chain...", color = colorTexto)
        }
        return
    }

    // Nombres de species para descubrir formas regionales
    val chainSpeciesNames = remember(evolutionChainResponse.chain) {
        val names = mutableListOf<String>()
        fun collectNames(link: ChainLink) {
            names.add(link.species.name)
            link.evolvesTo.forEach { collectNames(it) }
        }
        collectNames(evolutionChainResponse.chain)
        names
    }

    val branchedEvolutionData = remember(evolutionChainResponse.chain) {
        getEvolutionSteps(evolutionChainResponse.chain)
    }

    val isLinear = remember(branchedEvolutionData) {
        isChainPredominantlyLinear(branchedEvolutionData)
    }

    val linearEvolutionPath = remember(evolutionChainResponse.chain, isLinear) {
        if (isLinear) flattenEvolutionChainForLinearDisplay(evolutionChainResponse.chain)
        else emptyList()
    }

    if (branchedEvolutionData.isEmpty() && linearEvolutionPath.isEmpty() && evolutionChainResponse.chain.evolvesTo.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            val baseName by produceState(initialValue = formatApiName(evolutionChainResponse.chain.species.name)) {
                if (evolutionChainResponse.chain.species.url.isNotBlank()) {
                    value = viewModel.fetchLocalizedName(
                        resourceUrl = evolutionChainResponse.chain.species.url,
                        fallbackApiName = evolutionChainResponse.chain.species.name,
                        resourceTypeHint = "pokemon-species"
                    )
                }
            }
            Text("$baseName no tiene evoluciones.", color = colorTexto)
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = color1)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Linea Evolutiva",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorTexto
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLinear && linearEvolutionPath.isNotEmpty()) {
                        if (isLandscape) {
                            // LINEAL LANDSCAPE: Row horizontal, cards se reparten el ancho
                            val evoCount = linearEvolutionPath.size
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                linearEvolutionPath.forEachIndexed { index, (chainLink, evolutionDetail) ->
                                    val condition by produceState<String?>(initialValue = null, evolutionDetail) {
                                        value = evolutionDetail?.let { viewModel.buildEvolutionConditionString(it) }
                                    }
                                    EvolutionStageView(
                                        chainLink = chainLink,
                                        builtEvolutionCondition = condition,
                                        conditionIcon = getConditionIcon(evolutionDetail),
                                        itemSpriteUrl = getConditionItemSpriteUrl(evolutionDetail),
                                        onClick = onPokemonClick,
                                        viewModel = viewModel,
                                        color = color2,
                                        colorTexto = colorTexto,
                                        staggerIndex = index,
                                        compact = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (index < evoCount - 1) {
                                        EnergyFlowConnector(
                                            color = colorTexto.copy(alpha = 0.6f),
                                            horizontal = true,
                                            modifier = Modifier
                                                .width(32.dp)
                                                .height(24.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // LINEAL PORTRAIT: lista vertical con conectores animados entre cards
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                linearEvolutionPath.forEachIndexed { index, (chainLink, evolutionDetail) ->
                                    item(key = "evo_${chainLink.species.name}") {
                                        val condition by produceState<String?>(initialValue = null, evolutionDetail) {
                                            value = evolutionDetail?.let { viewModel.buildEvolutionConditionString(it) }
                                        }
                                        EvolutionStageView(
                                            chainLink = chainLink,
                                            builtEvolutionCondition = condition,
                                            conditionIcon = getConditionIcon(evolutionDetail),
                                            itemSpriteUrl = getConditionItemSpriteUrl(evolutionDetail),
                                            onClick = onPokemonClick,
                                            viewModel = viewModel,
                                            color = color2,
                                            colorTexto = colorTexto,
                                            staggerIndex = index,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (index < linearEvolutionPath.size - 1) {
                                        item(key = "connector_$index") {
                                            EnergyFlowConnector(
                                                color = colorTexto.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .width(24.dp)
                                                    .height(36.dp)
                                            )
                                        }
                                    }
                                }

                                // Ramas regionales al final de la cadena
                                item(key = "regional_branches") {
                                    RegionalEvolutionBranches(
                                        chainSpeciesNames = chainSpeciesNames,
                                        onPokemonClick = onPokemonClick,
                                        viewModel = viewModel,
                                        cardColor = color2,
                                        textColor = colorTexto,
                                        connectorColor = colorTexto.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        // RAMIFICADA: cada paso con base + evoluciones
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(branchedEvolutionData.size) { index ->
                                EvolutionStepDisplay(
                                    step = branchedEvolutionData[index],
                                    onPokemonClick = onPokemonClick,
                                    viewModel = viewModel,
                                    cardColor = color2,
                                    textColor = colorTexto,
                                    connectorColor = colorTexto.copy(alpha = 0.6f),
                                    baseStaggerIndex = index * 3
                                )
                            }

                            // Ramas regionales al final
                            item(key = "regional_branches") {
                                RegionalEvolutionBranches(
                                    chainSpeciesNames = chainSpeciesNames,
                                    onPokemonClick = onPokemonClick,
                                    viewModel = viewModel,
                                    cardColor = color2,
                                    textColor = colorTexto,
                                    connectorColor = colorTexto.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== PASO EVOLUTIVO (RAMIFICADO) ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvolutionStepDisplay(
    step: EvolutionStep,
    onPokemonClick: (String) -> Unit,
    viewModel: PokemonViewModel = viewModel(),
    cardColor: Color,
    textColor: Color,
    connectorColor: Color,
    baseStaggerIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        // Pokemon base de este paso
        val fromCondition by produceState<String?>(initialValue = null, step.fromPokemonEvolutionDetail) {
            value = step.fromPokemonEvolutionDetail?.let { viewModel.buildEvolutionConditionString(it) }
        }
        EvolutionStageView(
            chainLink = step.fromPokemon,
            builtEvolutionCondition = fromCondition,
            conditionIcon = getConditionIcon(step.fromPokemonEvolutionDetail),
            itemSpriteUrl = getConditionItemSpriteUrl(step.fromPokemonEvolutionDetail),
            onClick = onPokemonClick,
            viewModel = viewModel,
            color = cardColor,
            colorTexto = textColor,
            staggerIndex = baseStaggerIndex,
            modifier = Modifier.fillMaxWidth()
        )

        if (step.toEvolutions.isNotEmpty()) {
            EnergyFlowConnector(
                color = connectorColor,
                modifier = Modifier
                    .width(24.dp)
                    .height(36.dp)
            )

            // Evoluciones: 2 columnas si hay varias
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (step.toEvolutions.size > 2) 2 else step.toEvolutions.size
            ) {
                step.toEvolutions.forEachIndexed { i, (evolutionLink, evolutionDetail) ->
                    val condition by produceState<String?>(initialValue = null, evolutionDetail) {
                        value = evolutionDetail?.let { viewModel.buildEvolutionConditionString(it) }
                    }
                    EvolutionStageView(
                        chainLink = evolutionLink,
                        builtEvolutionCondition = condition,
                        conditionIcon = getConditionIcon(evolutionDetail),
                        itemSpriteUrl = getConditionItemSpriteUrl(evolutionDetail),
                        onClick = onPokemonClick,
                        viewModel = viewModel,
                        color = cardColor,
                        colorTexto = textColor,
                        staggerIndex = baseStaggerIndex + 1 + i,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== FORMAS REGIONALES EN LA CADENA ====================

data class RegionalBranch(
    val regionLabel: String, // ej: "Galar", "Alola"
    val forms: List<RegionalFormEntry> // formas en orden evolutivo
)

data class RegionalFormEntry(
    val pokemonName: String, // ej: "mr-mime-galar"
    val pokemonId: Int,
    val speciesId: Int, // ID de la species base (para URL HOME)
    val formIndex: Int, // indice de la variedad en la lista de varieties
    val displayName: String // se resuelve async
)

private val REGIONAL_SUFFIXES = mapOf(
    "-alola" to "Alola", "-galar" to "Galar", "-hisui" to "Hisui", "-paldea" to "Paldea"
)

/**
 * Composable que muestra ramas de evolucion regionales.
 * Descubre varieties regionales de las species de la cadena y las muestra como ramas adicionales.
 */
@Composable
fun RegionalEvolutionBranches(
    chainSpeciesNames: List<String>,
    onPokemonClick: (String) -> Unit,
    viewModel: PokemonViewModel = viewModel(),
    cardColor: Color,
    textColor: Color,
    connectorColor: Color
) {
    // Descubrir formas regionales de todas las species de la cadena
    val regionalBranches by produceState<List<RegionalBranch>>(initialValue = emptyList(), chainSpeciesNames) {
        if (chainSpeciesNames.isEmpty()) return@produceState

        val allRegionalForms = mutableMapOf<String, MutableList<RegionalFormEntry>>() // region -> forms

        chainSpeciesNames.forEach { speciesName ->
            try {
                val resp = viewModel.pokemonApiService.getPokemonSpeciesDetails(speciesName)
                if (resp.isSuccessful) {
                    val body = resp.body() ?: return@forEach
                    val speciesId = body.id
                    val varieties = body.varieties
                    varieties.forEachIndexed { index, variety ->
                        if (variety.isDefault) return@forEachIndexed
                        val name = variety.pokemon.name
                        val matchedRegion = REGIONAL_SUFFIXES.entries.firstOrNull { name.contains(it.key) }
                        if (matchedRegion != null) {
                            val formId = variety.pokemon.url.trimEnd('/').split("/").lastOrNull()?.toIntOrNull() ?: return@forEachIndexed
                            val region = matchedRegion.value
                            allRegionalForms.getOrPut(region) { mutableListOf() }.add(
                                RegionalFormEntry(name, formId, speciesId, index, name)
                            )
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        value = allRegionalForms.map { (region, forms) ->
            RegionalBranch(region, forms.sortedBy { it.pokemonId })
        }.sortedBy { it.regionLabel }
    }

    if (regionalBranches.isEmpty()) return

    var staggerOffset = 0
    regionalBranches.forEach { branch ->
        Spacer(Modifier.height(12.dp))

        // Header de la region
        Text(
            text = "Formas de ${branch.regionLabel}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        branch.forms.forEachIndexed { index, form ->
            if (index > 0) {
                EnergyFlowConnector(
                    color = connectorColor,
                    modifier = Modifier
                        .width(24.dp)
                        .height(28.dp)
                )
            }

            RegionalFormCard(
                formEntry = form,
                onClick = onPokemonClick,
                viewModel = viewModel,
                color = cardColor,
                colorTexto = textColor,
                staggerIndex = staggerOffset + index
            )
        }
        staggerOffset += branch.forms.size
    }
}

@Composable
private fun RegionalFormCard(
    formEntry: RegionalFormEntry,
    onClick: (String) -> Unit,
    viewModel: PokemonViewModel,
    color: Color,
    colorTexto: Color,
    staggerIndex: Int
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(staggerIndex * 120L)
        appeared = true
    }
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300), label = "regAlpha"
    )
    val appearSlide by animateFloatAsState(
        targetValue = if (appeared) 0f else 20f,
        animationSpec = tween(300), label = "regSlide"
    )

    var localizedName by remember(formEntry.pokemonName) {
        mutableStateOf(formatApiName(formEntry.pokemonName))
    }
    LaunchedEffect(formEntry.pokemonName) {
        try {
            localizedName = viewModel.fetchLocalizedName(
                resourceUrl = "https://pokeapi.co/api/v2/pokemon-species/${formEntry.pokemonName.substringBefore("-")}/",
                fallbackApiName = formEntry.pokemonName,
                resourceTypeHint = "pokemon-species"
            )
            // Append region suffix
            val regionSuffix = REGIONAL_SUFFIXES.entries.firstOrNull { formEntry.pokemonName.contains(it.key) }?.value
            if (regionSuffix != null && !localizedName.contains(regionSuffix, ignoreCase = true)) {
                localizedName = "$localizedName de $regionSuffix"
            }
        } catch (_: Exception) { }
    }

    ElevatedCard(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick(formEntry.pokemonId.toString())
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer { alpha = appearAlpha; translationY = appearSlide },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            val homeUrl = remember(formEntry) {
                getPokemonHomeSpriteUrl(formEntry.speciesId, formEntry.formIndex)
            }
            val fallbackUrl = remember(formEntry) {
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${formEntry.pokemonId}.png"
            }
            var spriteUrl by remember(formEntry.pokemonId) { mutableStateOf(homeUrl) }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(spriteUrl)
                    .crossfade(true)
                    .listener(onError = { _, _ ->
                        if (spriteUrl == homeUrl) spriteUrl = fallbackUrl
                    })
                    .build(),
                contentDescription = localizedName,
                modifier = Modifier.size(90.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = localizedName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colorTexto
            )
        }
    }
}

// ==================== DATOS Y ALGORITMOS ====================

data class EvolutionStep(
    val fromPokemon: ChainLink,
    val fromPokemonEvolutionDetail: EvolutionDetail?,
    val toEvolutions: List<Pair<ChainLink, EvolutionDetail?>>
)

fun getEvolutionSteps(baseChainLink: ChainLink): List<EvolutionStep> {
    val steps = mutableListOf<EvolutionStep>()
    val queue = ArrayDeque<Pair<ChainLink, EvolutionDetail?>>()
    val processedAsFromPokemon = mutableSetOf<String>()

    steps.add(
        EvolutionStep(
            fromPokemon = baseChainLink,
            fromPokemonEvolutionDetail = null,
            toEvolutions = baseChainLink.evolvesTo.map { Pair(it, it.evolutionDetails.firstOrNull()) }
        )
    )
    processedAsFromPokemon.add(baseChainLink.species.name)

    baseChainLink.evolvesTo.forEach {
        queue.add(Pair(it, it.evolutionDetails.firstOrNull()))
    }

    while (queue.isNotEmpty()) {
        val (currentLink, detailToReachCurrent) = queue.removeFirst()
        if (currentLink.evolvesTo.isNotEmpty() && !processedAsFromPokemon.contains(currentLink.species.name)) {
            steps.add(
                EvolutionStep(
                    fromPokemon = currentLink,
                    fromPokemonEvolutionDetail = detailToReachCurrent,
                    toEvolutions = currentLink.evolvesTo.map { Pair(it, it.evolutionDetails.firstOrNull()) }
                )
            )
            processedAsFromPokemon.add(currentLink.species.name)
            currentLink.evolvesTo.forEach { queue.add(Pair(it, it.evolutionDetails.firstOrNull())) }
        }
    }
    return steps
}

fun isChainPredominantlyLinear(evolutionSteps: List<EvolutionStep>): Boolean {
    return evolutionSteps.all { it.toEvolutions.size <= 1 }
}

fun flattenEvolutionChainForLinearDisplay(baseChainLink: ChainLink): List<Pair<ChainLink, EvolutionDetail?>> {
    val path = mutableListOf<Pair<ChainLink, EvolutionDetail?>>()
    fun traverse(link: ChainLink, detail: EvolutionDetail?) {
        path.add(Pair(link, detail))
        if (link.evolvesTo.isNotEmpty()) {
            val next = link.evolvesTo[0]
            traverse(next, next.evolutionDetails.firstOrNull())
        }
    }
    traverse(baseChainLink, null)
    return path
}
