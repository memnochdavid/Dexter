package com.david.pokedex_api.ui.screen.ficha.composable

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.LiveSprites
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.MuestraDesc
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.MuestraStatsBase
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonAbilitiesList
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.PokemonEvolutionChainView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonMovesList
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonRegionalFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.PokemonSpecialFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonTypeInteractionsTable
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.PokemonFormsView
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion.descripcionesMegasGigas
import com.david.pokedex_api.ui.theme.color_progress_bar
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class
) // Añadir ExperimentalFoundationApi
@Composable
fun DetallesDesplegables(
    pokemon: PokemonDetailResponse,
    evolutionChainDetailResponse: EvolutionChainDetailResponse?,
    isLoadingEvolutionChain: Boolean,
    onEvolutionPokemonClick: (pokemonName: String) -> Unit,
    description: String?,
    pokemonApiService: PokeApiService,
    modifier: Modifier = Modifier
) {
    // selectedContent sigue siendo la fuente de verdad para el estado lógico
    var selectedContent by remember { mutableStateOf<ContentPage?>(ContentPage.DESC) }

    val density = LocalDensity.current
    val buttonMeasures = remember { mutableStateListOf<Pair<Dp, Dp>>() }
    val rowPaddingHorizontal = 16.dp

    val contentPages = remember { ContentPage.entries.toList() } // Lista de páginas
    val pagerState = rememberPagerState(
        initialPage = 0, // Set initial page
        pageCount = { contentPages.size }
    )


    // Sincronizar selectedContent cuando el Pager cambia por swipe
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .map { pageIndex -> contentPages.getOrNull(pageIndex) }
            .collect { page ->
                if (page != null && selectedContent != page) {
                    selectedContent = page
                }
            }
    }

    // Sincronizar Pager cuando selectedContent cambia por clic en botón
    LaunchedEffect(selectedContent) {
        val targetPageIndex = selectedContent?.let { contentPages.indexOf(it) } ?: -1
        if (targetPageIndex != -1 && targetPageIndex != pagerState.currentPage) {
            launch { // Usar launch para la corrutina de scroll
                pagerState.animateScrollToPage(targetPageIndex)
            }
        }
    }

    val selectedIndexFromContent = remember(selectedContent) { // Renombrado para claridad
        selectedContent?.let { contentPages.indexOf(it) } ?: -1
    }

    val indicatorWidth by animateDpAsState(
        targetValue = if (selectedIndexFromContent != -1 && selectedIndexFromContent < buttonMeasures.size) {
            buttonMeasures[selectedIndexFromContent].second
        } else {
            0.dp
        },
        animationSpec = spring(),
        label = "indicatorWidth"
    )

    val indicatorOffsetX by animateDpAsState(
        targetValue = if (selectedIndexFromContent != -1 && selectedIndexFromContent < buttonMeasures.size) {
            buttonMeasures[selectedIndexFromContent].first + rowPaddingHorizontal
        } else {
            rowPaddingHorizontal
        },
        animationSpec = spring(),
        label = "indicatorOffsetX"
    )

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Row de IconButtons (sin cambios en su lógica interna) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = rowPaddingHorizontal),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            contentPages.forEachIndexed { index, page -> // Iterar sobre la lista de páginas
                if (index >= buttonMeasures.size) {
                    buttonMeasures.add(Pair(0.dp, 0.dp))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { layoutCoordinates ->
                            val positionInRow =
                                with(density) { layoutCoordinates.boundsInParent().left.toDp() }
                            val width = with(density) { layoutCoordinates.size.width.toDp() }
                            buttonMeasures[index] = Pair(positionInRow, width)
                        }
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedContent =
                                page // Esto disparará el LaunchedEffect para mover el Pager
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        TextoMenu(
                            when (page) {
                                ContentPage.DESC -> "Desc."
                                ContentPage.EVOS -> "Evo."
                                ContentPage.STATS -> "Stats"
                                ContentPage.MOVES -> "Mov."
                                ContentPage.ABILITY -> "Hab."
                                ContentPage.INTER -> "Tipos"
                                ContentPage.FORM -> "Form."
                            },
                            colorFondo = if (selectedContent == page) getPokemonTypeColor(pokemon.types[0].type.name) else Color.Transparent,
                            colorTexto = if ((esTipoColorOscuro(pokemon.types[0].type.name)) && selectedContent == page) {
                                Color.White
                            } else {
                                CardBorder
                            }
                        )
                    }
                }
            }
        }

        // --- Indicador (sin cambios) ---
        if (selectedContent != null && selectedIndexFromContent != -1 && selectedIndexFromContent < buttonMeasures.size) {
            Box(
                Modifier // El Modifier del Box del indicador
                    .padding(top = 0.dp)
                    .height(3.dp)
                    .width(indicatorWidth)
                    .offset(x = indicatorOffsetX)
                    .background(getPokemonTypeColor(pokemon.types[0].type.name)) // Asegúrate que esta función y pokemon.types[0] sean seguros
            )
        }

        // --- HorizontalPager para el Contenido ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f) // O la altura que desees para el área de contenido
                .padding(vertical = if (selectedContent == null) 0.dp else 8.dp),
            // userScrollEnabled = true // Habilitado por defecto, puedes deshabilitarlo si es necesario
        ) { pageIndex ->
            // El contenido se renderiza aquí basado en la página actual del Pager
            // No necesitas AnimatedContent aquí, HorizontalPager maneja las transiciones de swipe.
            // Si quieres animaciones personalizadas entre páginas, es más complejo con Pager,
            // pero el swipe por sí mismo ya es una transición.

            val targetPage = contentPages.getOrNull(pageIndex)

            // Contenedor para cada página, si necesitas padding o alineación específica
            Box(
                modifier = Modifier.fillMaxSize(), // Cada página llena el espacio del Pager
                contentAlignment = Alignment.TopCenter // Puedes ajustar la alineación si es necesario
                // Puedes añadir padding aquí si es común a todas las páginas
                // .padding(horizontal = 16.dp)
            ) {
                when (targetPage) {
                    ContentPage.STATS -> {
                        MuestraStatsBase(
                            stats = pokemon.stats,
                            colorFondo = getPokemonTypeColor(pokemon.types[0].type.name),
                            colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                Color.White
                            } else {
                                CardBorder
                            },
                        )
                    }

                    ContentPage.EVOS -> {
                        if (isLoadingEvolutionChain && evolutionChainDetailResponse == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = color_progress_bar)
                            }
                        } else if (evolutionChainDetailResponse != null) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize() // Esto le da restricciones máximas
                                    .padding(bottom = 8.dp) // Padding en la parte inferior del contenido scrolleable
                            ) {
                                item {

                                    PokemonEvolutionChainView(
                                        evolutionChainResponse = evolutionChainDetailResponse,
                                        onPokemonClick = onEvolutionPokemonClick,
                                        // Asumo que tienes estas funciones de color o pásalas como parámetros
                                        color1 = getPokemonTypeColor(
                                            pokemon.types.firstOrNull()?.type?.name ?: "normal"
                                        ),
                                        color2 = getPokemonTypeColorClear(
                                            pokemon.types.firstOrNull()?.type?.name ?: "normal"
                                        ),
                                        colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                            Color.White
                                        } else {
                                            CardBorder
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.5f)
                                        // .wrapContentHeight() // No es estrictamente necesario aquí, Column maneja la altura
                                    )
                                }
                                item {
                                    Spacer(Modifier.height(16.dp)) // Espacio entre las dos vistas
                                }
                                item {
                                    PokemonSpecialFormsView(
                                        pokemonSpeciesUrl = pokemon.species.url,
//                                        pokemonName = pokemon.name, // Nombre base para evitar auto-clics
                                        pokemonApiService = pokemonApiService,
                                        onFormClick = { formName ->
                                            onEvolutionPokemonClick(formName) // Navegar al hacer clic en una forma
                                        },
                                        // Asumo que tienes estas funciones de color o pásalas como parámetros
                                        cardColor = getPokemonTypeColor(
                                            pokemon.types.firstOrNull()?.type?.name ?: "normal"
                                        ),
                                        itemCardColor = getPokemonTypeColorClear(
                                            pokemon.types.firstOrNull()?.type?.name ?: "normal"
                                        ),
                                        colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                            Color.White
                                        } else {
                                            CardBorder
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth() // Que ocupe el ancho disponible
                                            .fillMaxHeight(0.5f)
                                        // .wrapContentHeight() // No es estrictamente necesario aquí
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No se pudieron cargar los datos de evolución.")
                            }
                        }
                    }

                    ContentPage.DESC -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            //descripción
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(0.55f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                description?.let { descText ->
                                    MuestraDesc(
                                        numPokemon = pokemon.id.toString(),
                                        desc = descText,
                                        colorFondo = getPokemonTypeColor(pokemon.types[0].type.name),
                                        colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                            Color.White
                                        } else {
                                            CardBorder
                                        },
                                    )
                                } ?: Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val descFormaEspecial =
                                        descripcionesMegasGigas.find { it.pokeId == pokemon.id }
                                    MuestraDesc(
                                        numPokemon = pokemon.id.toString(),
                                        desc = descFormaEspecial?.desc
                                            ?: "No hay descripción disponible.",
                                        colorFondo = getPokemonTypeColor(pokemon.types[0].type.name),
                                        colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                            Color.White
                                        } else {
                                            CardBorder
                                        },
                                    )
                                }
                            }
                            //sprites
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(0.45f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiveSprites(
                                    pokemon = pokemon,
                                    colorTexto = CardBorder,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    ContentPage.MOVES -> {
                        PokemonMovesList(
                            moves = pokemon.moves,
                            cardBackgroundColor = getPokemonTypeColor(pokemon.types[0].type.name),
                            pokemonApiService = pokemonApiService,
                            textColor = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                Color.White
                            } else {
                                CardBorder
                            },
                            modifier = Modifier.fillMaxHeight() // FillMaxHeight dentro del Box de la página
                        )
                    }

                    ContentPage.ABILITY -> {
                        PokemonAbilitiesList(
                            abilities = pokemon.abilities,
                            backgroundColor = getPokemonTypeColor(pokemon.types[0].type.name),
                            textColor = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                Color.White
                            } else {
                                CardBorder
                            },
                            pokemonApiService = pokemonApiService,
                            modifier = Modifier.fillMaxWidth() // FillMaxHeight dentro del Box de la página
                        )
                    }

                    ContentPage.INTER -> {
                        PokemonTypeInteractionsTable(
                            pokemonTypes = pokemon.types,
                            pokemonApiService = pokemonApiService,
                            tableBackgroundColor = getPokemonTypeColor(pokemon.types[0].type.name),
                            textColor = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                Color.White
                            } else {
                                CardBorder
                            },
                            // modifier = Modifier.fillMaxHeight() // Si es necesario
                        )
                    }

                    ContentPage.FORM -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize() // Fill the space provided by the HorizontalPager's page
                                .padding(bottom = 8.dp), // Optional padding for scollable content
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp) // Space between items
                        ) {
                            // Item for Regional Forms
                            item {
                                PokemonRegionalFormsView(
                                    pokemonSpeciesUrl = pokemon.species.url,
                                    basePokemonName = pokemon.name,
                                    pokemonApiService = pokemonApiService,
                                    onFormClick = { pokemonNameClicked -> // The lambda gives the full pokemon name for navigation
                                        onEvolutionPokemonClick(pokemonNameClicked)
                                    },
                                    cardColor = getPokemonTypeColor(pokemon.types[0].type.name),
                                    colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                        Color.White
                                    } else {
                                        CardBorder // Make sure CardBorder is defined, e.g., MaterialTheme.colorScheme.outline
                                    },
                                    itemCardColor = getPokemonTypeColorClear(pokemon.types[0].type.name),
                                    // Modifier to ensure it takes appropriate width within LazyColumn
                                    modifier = Modifier.fillMaxWidth(0.95f) // Example: 95% of the width
                                )
                            }

                            // Item for Other Forms (from /pokemon endpoint)
                            item {
                                PokemonFormsView(
                                    pokemon = pokemon, // Or pokemon.name
                                    pokemonApiService = pokemonApiService,
                                    onFormClick = { basePokemonName, formApiName ->
                                        // You might want to navigate to the specific form.
                                        // If formApiName is a full Pokémon name (e.g., "charizard-mega-x"),
                                        // you can use it directly.
                                        // Otherwise, you might need a way to resolve formApiName to a navigable entity.
                                        // For now, let's assume onEvolutionPokemonClick can handle the formApiName
                                        // or you have a different navigation handler for these forms.
                                        onEvolutionPokemonClick(formApiName) // Or adjust as per your navigation logic
                                    },
                                    cardColor = getPokemonTypeColor(pokemon.types[0].type.name),
                                    itemCardColor = getPokemonTypeColorClear(pokemon.types[0].type.name),
                                    colorTexto = if (esTipoColorOscuro(pokemon.types[0].type.name)) {
                                        Color.White
                                    } else {
                                        CardBorder
                                    },
                                    modifier = Modifier.fillMaxWidth(0.95f) // Example: 95% of the width
                                )
                            }

                        }
                    }

                    null -> { // En caso de que targetPage sea null (no debería pasar con pageCount definido)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Contenido no disponible")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun TextoMenu(title: String, colorFondo: Color, colorTexto: Color) {
    Box(
        modifier = Modifier
            .background(colorFondo)
            .fillMaxSize()
            .clip(RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = colorTexto,
            style = TextStyle(
                fontSize = 12.sp
            ),
//            modifier = Modifier.fillMaxSize()
        )
    }
}

enum class ContentPage {
    DESC, EVOS, STATS, MOVES, ABILITY, INTER, FORM
}