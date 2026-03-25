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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.david.pokedex_api.util.formatApiName

fun getPokemonIdFromSpeciesUrl(url: String): Int? {
    // URL es como "https://pokeapi.co/api/v2/pokemon-species/1/"
    return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
}

// Función auxiliar para obtener la URL del sprite usando el ID
fun getPokemonSpriteUrl(pokemonId: Int): String {
    return "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$pokemonId.png"
}

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionStageView(
    chainLink: ChainLink,
    builtEvolutionCondition: String?, // <-- NUEVO: Condición ya construida por el padre
    onClick: (String) -> Unit, // onClick seguirá usando el nombre API o el ID
    viewModel: PokemonViewModel = viewModel(), // <-- Obtiene la instancia del ViewModel
    color: Color = Color.LightGray, // Usar colores más neutrales por defecto o de Theme
    colorTexto: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val pokemonId = getPokemonIdFromSpeciesUrl(chainLink.species.url)
    val context = LocalContext.current

    // Estado para el nombre localizado del Pokémon
    var localizedPokemonName by remember(chainLink.species.name) {
        mutableStateOf(formatApiName(chainLink.species.name)) // Nombre formateado inicial
    }

    // Carga el nombre localizado del Pokémon
    LaunchedEffect(chainLink.species.url, chainLink.species.name) {
        if (chainLink.species.url.isNotBlank()) {
            localizedPokemonName = viewModel.fetchLocalizedName(
                resourceUrl = chainLink.species.url,
                fallbackApiName = chainLink.species.name,
                resourceTypeHint = "pokemon-species" // o "pokemon" según tu API
            )
        } else {
            localizedPokemonName = formatApiName(chainLink.species.name)
        }
    }
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        ElevatedCard(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick(chainLink.species.name)
                      }, // Se sigue usando el nombre API para la navegación/ID
            modifier = Modifier
                .wrapContentSize(),
            //.background(Color.Transparent), // El fondo del Card es manejado por sus colors
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = color) // Fondo de la tarjeta
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    //.background(color) // El color de fondo ya está en ElevatedCard
                    .padding(8.dp)
            ) {
                if (pokemonId != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(getPokemonSpriteUrl(pokemonId))
                            .crossfade(true)
                            .build(),
                        contentDescription = localizedPokemonName, // Usar nombre localizado
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(72.dp)) // Placeholder
                }
                Text(
                    text = localizedPokemonName, // Usar nombre localizado
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colorTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))

        // Mostrar detalles de la evolución (condición ya construida)
        builtEvolutionCondition?.let { condition ->
            Text(
                text = condition, // Mostrar la condición pre-construida
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp, top = 2.dp),
                color = colorTexto
            )
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonEvolutionChainView(
    evolutionChainResponse: EvolutionChainDetailResponse?,
    onPokemonClick: (pokemonName: String) -> Unit,
    viewModel: PokemonViewModel = viewModel(), // Obtiene la instancia del ViewModel
    color1: Color = MaterialTheme.colorScheme.surfaceVariant, // Color de fondo de la Card principal
    color2: Color = MaterialTheme.colorScheme.surface,      // Color de fondo para las tarjetas internas (stages)
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) } // Estado para controlar si está expandido o plegado

    if (evolutionChainResponse == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
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
        if (isLinear) {
            flattenEvolutionChainForLinearDisplay(evolutionChainResponse.chain)
        } else {
            emptyList()
        }
    }

    if (branchedEvolutionData.isEmpty() && linearEvolutionPath.isEmpty() && evolutionChainResponse.chain.evolvesTo.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            val basePokemonNameState by produceState(initialValue = formatApiName(evolutionChainResponse.chain.species.name)) {
                if (evolutionChainResponse.chain.species.url.isNotBlank()) {
                    value = viewModel.fetchLocalizedName(
                        resourceUrl = evolutionChainResponse.chain.species.url,
                        fallbackApiName = evolutionChainResponse.chain.species.name,
                        resourceTypeHint = "pokemon-species"
                    )
                }
            }
            Text("$basePokemonNameState no tiene evoluciones.", color = colorTexto)
        }
        return
    } else if (branchedEvolutionData.isEmpty() && linearEvolutionPath.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text("No evolution data available for this Pokémon.", color = colorTexto)
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
            // No es necesario .animateContentSize() aquí si AnimatedVisibility maneja el contenido
        ) {
            Row( // Encabezado "Línea Evolutiva"
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded } // Hace el título clickeable
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

            // Contenido plegable animado
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) { // Columna interna para el contenido
                    if (isLinear) {
                        if (linearEvolutionPath.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
//                                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
//                                horizontalArrangement = Arrangement.SpaceEvenly, //.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                itemsIndexed(linearEvolutionPath) { index, pair ->
                                    val chainLink = pair.first
                                    val evolutionDetail = pair.second // Puede ser null

                                    val evolutionCondition by produceState<String?>(initialValue = null, evolutionDetail) {
                                        value = evolutionDetail?.let { detail ->
                                            viewModel.buildEvolutionConditionString(detail)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        EvolutionStageView(
                                            chainLink = chainLink,
                                            builtEvolutionCondition = evolutionCondition, // Pasas la condición ya construida
                                            onClick = onPokemonClick,
                                            viewModel = viewModel, // Pasas el viewModel
                                            color = color2,
                                            colorTexto = colorTexto
                                            // El modifier de EvolutionStageView se puede añadir aquí si es necesario
                                            // o dentro de su propia definición, como lo tenías.
                                        )
                                        if (index < linearEvolutionPath.size - 1) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Evolves to",
                                                modifier = Modifier
//                                                    .padding(horizontal = 4.dp) // Espacio alrededor de la flecha
                                                    .size(20.dp) // Tamaño de la flecha
                                                    .align(Alignment.CenterVertically), // Asegura que esté centrado con EvolutionStageView
                                                tint = colorTexto // Usa el color de texto general para la flecha
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (branchedEvolutionData.firstOrNull() != null && branchedEvolutionData.first().toEvolutions.isEmpty()) {
                            // Si es lineal pero el path está vacío (solo un Pokémon base), muestra ese Pokémon.
                            // Este caso podría ya estar cubierto por la lógica inicial de "no tiene evoluciones",
                            // pero lo mantenemos por si acaso hay una lógica específica aquí.
                            val baseStep = branchedEvolutionData.first()
                            val evolutionCondition by produceState<String?>(initialValue = null, baseStep.fromPokemonEvolutionDetail) {
                                value = baseStep.fromPokemonEvolutionDetail?.let { detail ->
                                    viewModel.buildEvolutionConditionString(detail)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EvolutionStageView(
                                    chainLink = baseStep.fromPokemon,
                                    builtEvolutionCondition = evolutionCondition,
                                    onClick = onPokemonClick,
                                    viewModel = viewModel,
                                    color = color2,
                                    colorTexto = colorTexto
                                )
                            }
                        }
                    } else { // Ramificada
                        // Usar LazyColumn si la lista de branchedEvolutionData puede ser larga
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight() // Permite que la altura se ajuste al contenido
                                .heightIn(max = 300.dp) // Limita la altura máxima para evitar que ocupe demasiado espacio
                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre cada EvolutionStepDisplay
                            horizontalAlignment = Alignment.CenterHorizontally // Centra los EvolutionStepDisplay
                        ) {
                            items(branchedEvolutionData.size) { index -> // Cambiado a items con solo el tamaño
                                val step = branchedEvolutionData[index]
                                EvolutionStepDisplay(
                                    step = step,
                                    onPokemonClick = onPokemonClick,
                                    viewModel = viewModel, // Pasa el viewModel
                                    cardColor = color2,    // Color para las tarjetas internas de EvolutionStepDisplay
                                    textColor = colorTexto,  // Color del texto general
                                    arrowColor = colorTexto  // Color para la flecha dentro de EvolutionStepDisplay
                                    // El modifier de EvolutionStepDisplay se puede añadir aquí si es necesario
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // Necesario para FlowRow
@Composable
fun EvolutionStepDisplay(
    step: EvolutionStep,
    onPokemonClick: (String) -> Unit,
    viewModel: PokemonViewModel = viewModel(), // <-- Obtiene instancia del ViewModel
    cardColor: Color,
    textColor: Color,
    arrowColor: Color, // Para el icono de flecha
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth() // Usa el modifier pasado
    ) {
        // Pokémon "desde" el que se evoluciona
        val fromPokemonEvolutionCondition by produceState<String?>(
            initialValue = null,
            step.fromPokemonEvolutionDetail // Key para recomponer si cambia
        ) {
            value = step.fromPokemonEvolutionDetail?.let { detail ->
                viewModel.buildEvolutionConditionString(detail)
            }
        }
        EvolutionStageView(
            chainLink = step.fromPokemon,
            builtEvolutionCondition = fromPokemonEvolutionCondition,
            onClick = onPokemonClick,
            viewModel = viewModel,
            color = cardColor,
            colorTexto = textColor,
            modifier = Modifier.padding(bottom = if (step.toEvolutions.isNotEmpty()) 8.dp else 0.dp)
        )

        // Si hay evoluciones "hacia", mostrarlas
        if (step.toEvolutions.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Evolves to",
                modifier = Modifier
//                    .padding(vertical = 8.dp)
                    .size(24.dp),
                tint = arrowColor // Usar el color para la flecha
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (step.toEvolutions.size >= 3) 3 else step.toEvolutions.size.coerceAtLeast(1)
            ) {
                step.toEvolutions.forEach { (evolutionLink, evolutionDetailToReachIt) ->
                    val toEvolutionCondition by produceState<String?>(
                        initialValue = null,
                        evolutionDetailToReachIt // Key para recomponer
                    ) {
                        value = evolutionDetailToReachIt?.let { detail ->
                            viewModel.buildEvolutionConditionString(detail)
                        }
                    }
                    EvolutionStageView(
                        chainLink = evolutionLink,
                        builtEvolutionCondition = toEvolutionCondition,
                        onClick = onPokemonClick,
                        viewModel = viewModel,
                        color = cardColor,
                        colorTexto = textColor,
                        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}


data class EvolutionStep(
    val fromPokemon: ChainLink,
    val fromPokemonEvolutionDetail: EvolutionDetail?, // Cómo se llegó a fromPokemon (null si es el inicio)
    val toEvolutions: List<Pair<ChainLink, EvolutionDetail?>> // Todas las evoluciones directas desde fromPokemon
)
fun getEvolutionSteps(baseChainLink: ChainLink): List<EvolutionStep> {
    val steps = mutableListOf<EvolutionStep>()
    val queue = ArrayDeque<Pair<ChainLink, EvolutionDetail?>>()
    val processedAsFromPokemon = mutableSetOf<String>()

    val initialToEvolutions = baseChainLink.evolvesTo.map {
        Pair(it, it.evolutionDetails.firstOrNull())
    }

    steps.add(
        EvolutionStep(
            fromPokemon = baseChainLink,
            fromPokemonEvolutionDetail = null,
            toEvolutions = initialToEvolutions
        )
    )
    processedAsFromPokemon.add(baseChainLink.species.name)

    baseChainLink.evolvesTo.forEach { firstLevelEvolution ->
        queue.add(Pair(firstLevelEvolution, firstLevelEvolution.evolutionDetails.firstOrNull()))
    }

    while (queue.isNotEmpty()) {
        val (currentLink, detailToReachCurrent) = queue.removeFirst()

        if (currentLink.evolvesTo.isNotEmpty() && !processedAsFromPokemon.contains(currentLink.species.name)) {
            steps.add(
                EvolutionStep(
                    fromPokemon = currentLink,
                    fromPokemonEvolutionDetail = detailToReachCurrent,
                    toEvolutions = currentLink.evolvesTo.map {
                        Pair(it, it.evolutionDetails.firstOrNull())
                    }
                )
            )
            processedAsFromPokemon.add(currentLink.species.name)

            currentLink.evolvesTo.forEach { nextLevelEvolution ->
                queue.add(Pair(nextLevelEvolution, nextLevelEvolution.evolutionDetails.firstOrNull()))
            }
        }
    }
    return steps
}
fun isChainPredominantlyLinear(evolutionSteps: List<EvolutionStep>): Boolean {
    if (evolutionSteps.isEmpty()) {
        return true // Una cadena vacía es lineal por defecto
    }
    // Si algún paso tiene más de una evolución directa, no es lineal.
    for (step in evolutionSteps) {
        if (step.toEvolutions.size > 1) {
            return false
        }
    }
    return true
}

// Para la visualización lineal con LazyRow, también necesitamos la lista "plana" original.
// Reutilizamos tu función flattenEvolutionChain original aquí.
fun flattenEvolutionChainForLinearDisplay(baseChainLink: ChainLink): List<Pair<ChainLink, EvolutionDetail?>> {
    val evolutionPath = mutableListOf<Pair<ChainLink, EvolutionDetail?>>()
    fun traverse(currentLink: ChainLink, evolutionDetailToReachThis: EvolutionDetail?) {
        evolutionPath.add(Pair(currentLink, evolutionDetailToReachThis))
        if (currentLink.evolvesTo.isNotEmpty()) {
            // Para una visualización lineal, solo tomamos la primera.
            val nextStageInMainBranch = currentLink.evolvesTo[0]
            val detailForNext = nextStageInMainBranch.evolutionDetails.firstOrNull()
            traverse(nextStageInMainBranch, detailForNext)
        }
    }
    traverse(baseChainLink, null)
    return evolutionPath
}


