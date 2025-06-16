package com.david.pokedex_api.ui.composables


import androidx.activity.result.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.ChainLink
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.EvolutionDetail
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.util.muestraDesc
import com.david.pokedex_api.util.muestraEvos
import com.david.pokedex_api.util.muestraStats
import com.david.pokedex_api.util.vistaDatos
import kotlinx.coroutines.delay

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
    evolutionDetail: EvolutionDetail?, // Puede ser null para la base de la evolución
    onClick: (String) -> Unit,
    color: Color = Color.Black,
    colorTexto: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val pokemonId = getPokemonIdFromSpeciesUrl(chainLink.species.url)
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Top,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        ElevatedCard(
            onClick = { pokemonId?.let { onClick(chainLink.species.name) } },
            modifier = Modifier
                .wrapContentSize()
                .background(Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(color)
                    .padding(8.dp)
            ) {
                if (pokemonId != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(getPokemonSpriteUrl(pokemonId))
                            .crossfade(true)
                            .build(),
                        contentDescription = chainLink.species.name,
                        modifier = Modifier
                            .size(72.dp),
//                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Spacer(modifier = Modifier.size(72.dp)) // Placeholder
                }
                Text(
                    text = chainLink.species.name.replaceFirstChar { it.titlecase() },
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colorTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        // Mostrar detalles de la evolución (cómo evoluciona a esta etapa)
        evolutionDetail?.let { detail ->
            val trigger = detail.trigger.name.replace("-", " ").replaceFirstChar { it.titlecase() }
            var condition = trigger // Inicializa 'condition' con el trigger

            detail.minLevel?.let { level ->
                condition += " (Nivel $level)" // Reasigna: condition = condition + " (Nivel $level)"
            }
            detail.item?.name?.let { itemName ->
                condition += "\nUsando ${itemName.replace("-", " ").replaceFirstChar { it.titlecase() }}"
            }
            detail.heldItem?.name?.let { heldItemName ->
                condition += "\nCon ${heldItemName.replace("-", " ").replaceFirstChar { it.titlecase() }}"
            }
            detail.minHappiness?.let { happiness ->
                condition += "\nMin. Felicidad: $happiness"
            }
            detail.timeOfDay?.takeIf { it.isNotEmpty() }?.let { time ->
                condition += "\nDurante ${time.replaceFirstChar { it.titlecase() }}"
            }
            detail.knownMoveType?.name?.let { moveType ->
                condition += "\nConociendo un mov. tipo ${moveType.replaceFirstChar { it.titlecase() }}"
            }
            detail.minAffection?.let { affection ->
                condition += "\nMin. Afecto: $affection"
            }
            detail.minBeauty?.let { beauty ->
                condition += "\nMin. Belleza: $beauty"
            }
            // Añade más condiciones de EvolutionDetail aquí, siempre reasignando a 'condition'
            // Ejemplo:
            // detail.location?.name?.let { locationName ->
            //     condition += "\nEn ${locationName.replace("-", " ").replaceFirstChar { it.titlecase() }}"
            // }
            // detail.gender?.let { genderId ->
            //     val genderName = if (genderId == 1) "Hembra" else if (genderId == 2) "Macho" else ""
            //     if (genderName.isNotEmpty()) condition += "\nSiendo $genderName"
            // }
            // detail.relativePhysicalStats?.let { relativeStats ->
            //     val comparison = when (relativeStats) {
            //         1 -> "Ataque > Defensa"
            //         -1 -> "Ataque < Defensa"
            //         0 -> "Ataque = Defensa"
            //         else -> ""
            //     }
            //     if (comparison.isNotEmpty()) condition += "\nCon $comparison"
            // }
            // // ... y así sucesivamente para todos los campos relevantes de EvolutionDetail

            Text(
                text = condition,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
                color = colorTexto // Asegúrate de que CardBorder esté definido en tu tema o localmente
            )
        }
    }
}

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
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ), // Centra si hay pocos ítems
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
                                            .size(20.dp)
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


