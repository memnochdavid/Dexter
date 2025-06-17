package com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        ElevatedCard(
            onClick = { onClick(chainLink.species.name) }, // Se sigue usando el nombre API para la navegación/ID
            modifier = Modifier
                .wrapContentSize(),
            //.background(Color.Transparent), // El fondo del Card es manejado por sus colors
            shape = RoundedCornerShape(12.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonEvolutionChainView(
    evolutionChainResponse: EvolutionChainDetailResponse?,
    onPokemonClick: (pokemonName: String) -> Unit,
    viewModel: PokemonViewModel = viewModel(), // <-- Obtiene la instancia del ViewModel
    color1: Color = MaterialTheme.colorScheme.surfaceVariant,
    color2: Color = MaterialTheme.colorScheme.surface,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    if (evolutionChainResponse == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text("Loading evolution chain...", color = colorTexto) // Usa colorTexto
        }
        return
    }

    // Estas funciones de ayuda (getEvolutionSteps, etc.) deberían idealmente
    // estar fuera del Composable o ser parte del ViewModel para evitar recalcular
    // innecesariamente si no dependen de `remember`. Aquí las dejamos como están
    // según tu código original, pero usando `remember` para eficiencia.
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
        // Modificado para también considerar el caso de un solo Pokémon sin evoluciones directas
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            // Mostrar el Pokémon base si no hay evoluciones
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
        // Caso general de no datos (aunque el anterior debería cubrir al Pokémon base)
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
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        //.background(Color.Transparent) // El fondo se define en colors
        colors = CardDefaults.cardColors(containerColor = color1) // Fondo de la tarjeta principal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
            //.background(color1) // El fondo ya está en Card
        ) {
            Row( // Encabezado "Línea Evolutiva"
                modifier = Modifier
                    .fillMaxWidth()
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

            if (isLinear) {
                // ... (código para la vista lineal como lo tenías) ...
                if (linearEvolutionPath.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top
                    ) {
                        itemsIndexed(linearEvolutionPath) { index, pair ->
                            val chainLink = pair.first
                            val evolutionDetail = pair.second // Puede ser null

                            // Produce el estado de la condición de evolución
                            val evolutionCondition by produceState<String?>(initialValue = null, evolutionDetail) {
                                value = evolutionDetail?.let { detail ->
                                    viewModel.buildEvolutionConditionString(detail)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EvolutionStageView(
                                    chainLink = chainLink,
                                    builtEvolutionCondition = evolutionCondition,
                                    onClick = onPokemonClick,
                                    viewModel = viewModel,
                                    color = color2,
                                    colorTexto = colorTexto
                                )
                                if (index < linearEvolutionPath.size - 1) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Evolves to",
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(20.dp)
                                            .align(Alignment.CenterVertically),
                                        tint = colorTexto
                                    )
                                }
                            }
                        }
                    }
                } else if (branchedEvolutionData.firstOrNull() != null && branchedEvolutionData.first().toEvolutions.isEmpty()) {
                    // Si es lineal pero el path está vacío (solo un Pokémon base), muestra ese Pokémon.
                    val baseStep = branchedEvolutionData.first()
                    val evolutionCondition by produceState<String?>(initialValue = null, baseStep.fromPokemonEvolutionDetail) {
                        value = baseStep.fromPokemonEvolutionDetail?.let { detail ->
                            viewModel.buildEvolutionConditionString(detail)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
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
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp), // Ajuste de padding
                    verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre cada EvolutionStepDisplay
                    horizontalAlignment = Alignment.CenterHorizontally // Centra los EvolutionStepDisplay
                ) {
                    items(branchedEvolutionData.size) { index ->
                        val step = branchedEvolutionData[index]
                        EvolutionStepDisplay( // Pasa el viewModel también a EvolutionStepDisplay
                            step = step,
                            onPokemonClick = onPokemonClick,
                            viewModel = viewModel,
                            cardColor = color2, // Este es el color para las tarjetas internas
                            textColor = colorTexto, // Color del texto general
                            arrowColor = colorTexto // Color para la flecha hacia abajo
                        )
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
                    .padding(vertical = 8.dp)
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





/*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonEvolutionChainView(
    evolutionChainResponse: EvolutionChainDetailResponse?,
    onPokemonClick: (pokemonName: String) -> Unit,
    color1: Color = Color.Black,
    color2: Color = Color.Black,
    colorTexto: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    if (evolutionChainResponse == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text("Loading evolution chain...")
        }
        return
    }

    // Obtenemos los datos para la visualización ramificada
    val branchedEvolutionData = remember(evolutionChainResponse.chain) {
        getEvolutionSteps(evolutionChainResponse.chain)
    }

    // Determinamos si la cadena es predominantemente lineal
    val isLinear = remember(branchedEvolutionData) {
        isChainPredominantlyLinear(branchedEvolutionData)
    }

    // Obtenemos los datos para la visualización lineal si es necesario
    val linearEvolutionPath = remember(evolutionChainResponse.chain, isLinear) {
        if (isLinear) {
            flattenEvolutionChainForLinearDisplay(evolutionChainResponse.chain)
        } else {
            emptyList() // No lo necesitamos si no es lineal
        }
    }

    if (branchedEvolutionData.isEmpty() && linearEvolutionPath.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp), contentAlignment = Alignment.Center
        ) {
            Text("No evolution data available for this Pokémon.")
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color1)
        ) {
            Row( // Encabezado "Línea Evolutiva"
                modifier = Modifier
                    .fillMaxWidth()
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

            // --- Decisión de visualización ---
            if (isLinear) {
                // --- Visualización Lineal con LazyRow (tu código anterior adaptado) ---
                if (linearEvolutionPath.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp,Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Top
                    ) {
                        itemsIndexed(linearEvolutionPath) { index, (chainLink, evolutionDetail) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EvolutionStageView(
                                    chainLink = chainLink,
                                    evolutionDetail = evolutionDetail,
                                    color = color2,
                                    colorTexto = colorTexto,
                                    onClick = onPokemonClick
                                )
                                if (index < linearEvolutionPath.size - 1) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Evolves to",
                                        modifier = Modifier
//                                            .padding(horizontal = 3.dp)
                                            .size(18.dp)
                                            .align(Alignment.CenterVertically),
                                        tint = colorTexto
                                    )
                                }
                            }
                        }
                    }
                } else { // Aunque sea lineal, si el path está vacío (ej. solo 1 pokemon sin evolución)
                    branchedEvolutionData.firstOrNull()?.let {
                        EvolutionStepDisplay( // Mostrar al menos el primer Pokémon
                            step = it,
                            onPokemonClick = onPokemonClick,
                            cardColor = color2
                        )
                    }
                }
            } else {
                // --- Visualización Ramificada con LazyColumn (código de la respuesta anterior) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
//                        .verticalScroll(rememberScrollState())
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
//                    items(branchedEvolutionData.size) { index ->
//                        val step = branchedEvolutionData[index]
//                        EvolutionStepDisplay(
//                            step = step,
//                            onPokemonClick = onPokemonClick,
//                            cardColor = color2
//                        )
//                    }
                    branchedEvolutionData.forEach {
                        EvolutionStepDisplay(
                            step = it,
                            onPokemonClick = onPokemonClick,
                            cardColor = color2
                        )
                    }

                }
            }
        }
    }
}
*/
/*
@OptIn(ExperimentalLayoutApi::class) // Necesario para FlowRow
@Composable
fun EvolutionStepDisplay(
    step: EvolutionStep,
    onPokemonClick: (String) -> Unit,
    cardColor: Color // Color para las tarjetas de EvolutionStageView
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mostrar el Pokémon "desde" el que se evoluciona
        EvolutionStageView(
            chainLink = step.fromPokemon,
            evolutionDetail = step.fromPokemonEvolutionDetail,
            onClick = onPokemonClick,
            color = cardColor,
            modifier = Modifier.padding(bottom = if (step.toEvolutions.isNotEmpty()) 8.dp else 0.dp) // Añade padding solo si hay evoluciones "hacia"
        )

        // Si hay evoluciones "hacia", mostrarlas
        if (step.toEvolutions.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Evolves to",
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Usamos FlowRow para que las evoluciones se ajusten al ancho disponible
            // y pasen a la siguiente línea si no caben todas en una.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp), // Padding horizontal para el FlowRow
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), // Espacio entre ítems y centrado horizontal
                verticalArrangement = Arrangement.spacedBy(8.dp), // Espacio vertical entre filas si hay wrap
                maxItemsInEachRow = if (step.toEvolutions.size > 3) 3 else step.toEvolutions.size // Limitar items por fila
            ) {
                step.toEvolutions.forEach { (evolutionLink, evolutionDetailToReachIt) ->
                    EvolutionStageView(
                        chainLink = evolutionLink,
                        evolutionDetail = evolutionDetailToReachIt,
                        onClick = onPokemonClick,
                        color = cardColor,
                        // El modifier de EvolutionStageView ya tiene un padding horizontal de 4.dp,
                        // FlowRow se encarga del espaciado entre ellos.
                        // Podemos darle un wrapContentWidth para que no intente ocupar más de lo necesario
                        // individualmente, aunque FlowRow gestiona el espacio.
                        modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally)

                    )
                }
            }
        }
        // Añadir un Spacer al final de cada EvolutionStepDisplay si no es el último en la LazyColumn
        // Esto se maneja mejor con Arrangement.spacedBy en la LazyColumn principal.
    }
}
*/
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


