package com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png"

// ==================== CARD DE UN POKEMON EN LA CADENA ====================

@Composable
fun EvolutionStageView(
    chainLink: ChainLink,
    builtEvolutionCondition: String?,
    onClick: (String) -> Unit,
    viewModel: PokemonViewModel = viewModel(),
    color: Color = Color.LightGray,
    colorTexto: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val pokemonId = getPokemonIdFromSpeciesUrl(chainLink.species.url)
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = color)
    ) {
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
                    Text(
                        text = condition,
                        fontSize = 12.sp,
                        color = colorTexto.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
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
    var isExpanded by remember { mutableStateOf(true) }

    if (evolutionChainResponse == null) {
        Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Loading evolution chain...", color = colorTexto)
        }
        return
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
        modifier = modifier.fillMaxSize(),
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
                    text = "Línea Evolutiva",
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
                if (isLinear && linearEvolutionPath.isNotEmpty()) {
                    // LINEAL: lista vertical scrollable con flechas entre cards
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    onClick = onPokemonClick,
                                    viewModel = viewModel,
                                    color = color2,
                                    colorTexto = colorTexto,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (index < linearEvolutionPath.size - 1) {
                                item(key = "arrow_$index") {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Evolves to",
                                        modifier = Modifier.size(28.dp),
                                        tint = colorTexto
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // RAMIFICADA: cada paso con base + evoluciones
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(branchedEvolutionData.size) { index ->
                            EvolutionStepDisplay(
                                step = branchedEvolutionData[index],
                                onPokemonClick = onPokemonClick,
                                viewModel = viewModel,
                                cardColor = color2,
                                textColor = colorTexto,
                                arrowColor = colorTexto
                            )
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
    arrowColor: Color,
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
            onClick = onPokemonClick,
            viewModel = viewModel,
            color = cardColor,
            colorTexto = textColor,
            modifier = Modifier.fillMaxWidth()
        )

        if (step.toEvolutions.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Evolves to",
                modifier = Modifier.size(28.dp),
                tint = arrowColor
            )

            // Evoluciones: 2 columnas si hay varias
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (step.toEvolutions.size > 2) 2 else step.toEvolutions.size
            ) {
                step.toEvolutions.forEach { (evolutionLink, evolutionDetail) ->
                    val condition by produceState<String?>(initialValue = null, evolutionDetail) {
                        value = evolutionDetail?.let { viewModel.buildEvolutionConditionString(it) }
                    }
                    EvolutionStageView(
                        chainLink = evolutionLink,
                        builtEvolutionCondition = condition,
                        onClick = onPokemonClick,
                        viewModel = viewModel,
                        color = cardColor,
                        colorTexto = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
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
