package com.david.pokedex_api.ui.screen.lista

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.getGenerationIdFromUrl
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
import com.david.pokedex_api.ui.screen.lista.composable.PokemonGridItemCard
import com.david.pokedex_api.ui.screen.lista.composable.PokemonListItemCard
import com.david.pokedex_api.ui.screen.lista.composable.PokemonSearchMenu
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.background_app_gradient
import com.david.pokedex_api.ui.theme.color_agua_light
import com.david.pokedex_api.ui.theme.color_boton_busqueda
import com.david.pokedex_api.ui.theme.color_menu_busqueda2
import com.david.pokedex_api.util.GifAnimado
import com.david.pokedex_api.util.Lottie

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenerationPagerScreen(
    pokemonViewModel: PokemonViewModel = viewModel(),
    onNavigateToDetails: (String) -> Unit
) {
    val generations by pokemonViewModel.generations.observeAsState(emptyList())
    val pokemonByGenerationCache by pokemonViewModel.pokemonByGenerationCache.observeAsState(emptyMap())
    val isLoadingAnyPokemon by pokemonViewModel.isLoadingPokemonForCurrentGeneration.observeAsState(false)

    val filters by pokemonViewModel.pokemonFilters.collectAsState()
    val specialForms by pokemonViewModel.specialFormsSummaries.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        if (generations.isEmpty()) pokemonViewModel.fetchGenerations()
    }

    val isSearching = filters.searchQuery.isNotBlank() || filters.selectedType1 != NO_TYPE_SELECTED || filters.selectedType2 != NO_TYPE_SELECTED ||
        filters.showMegas || filters.showGigamax || filters.showRegionals || filters.showLegendaries || filters.showMythicals ||
        filters.evoChainLength > 0

    // Cuando el usuario activa búsqueda/filtros, cargar todas las generaciones en segundo plano
    LaunchedEffect(isSearching) {
        if (isSearching) {
            pokemonViewModel.ensureAllGenerationsLoaded()
        }
    }
    val filteredList = remember(pokemonByGenerationCache, filters, specialForms) {
        if (!isSearching) emptyList()
        else {
            val showingOnlySpecialForms = filters.showMegas || filters.showGigamax || filters.showRegionals
            val showingLegendaryMythical = filters.showLegendaries || filters.showMythicals
            val allBasePokemon = pokemonByGenerationCache.values.flatten().distinctBy { it.id }

            val basePokemon = if (!showingOnlySpecialForms && !showingLegendaryMythical) {
                allBasePokemon
            } else if (showingLegendaryMythical && !showingOnlySpecialForms) {
                // Filtrar base pokemon por legendario/singular
                allBasePokemon.filter { p ->
                    (filters.showLegendaries && p.id in pokemonViewModel.legendaryIds) ||
                    (filters.showMythicals && p.id in pokemonViewModel.mythicalIds)
                }
            } else {
                emptyList()
            }
            val megaForms = if (filters.showMegas) specialForms.filter { it.name.contains("-mega") } else emptyList()
            val gmaxForms = if (filters.showGigamax) specialForms.filter { it.name.contains("-gmax") } else emptyList()
            val regionalForms = if (filters.showRegionals) specialForms.filter { it.name.let { n ->
                n.contains("-alola") || n.contains("-galar") || n.contains("-hisui") || n.contains("-paldea")
            }} else emptyList()

            (basePokemon + megaForms + gmaxForms + regionalForms).distinctBy { it.id }.filter { p ->
                (filters.searchQuery.isBlank() || p.name.contains(filters.searchQuery, true)) &&
                (filters.selectedType1 == NO_TYPE_SELECTED || p.types.any { it.equals(filters.selectedType1, true) }) &&
                (filters.selectedType2 == NO_TYPE_SELECTED || p.types.any { it.equals(filters.selectedType2, true) }) &&
                (filters.evoChainLength == 0 ||
                    (filters.evoChainLength < 3 && p.evoChainLength == filters.evoChainLength) ||
                    (filters.evoChainLength >= 3 && p.evoChainLength >= 3))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(background_app_gradient)) {
        if (isSearching) {
            if (filteredList.isEmpty() && !isLoadingAnyPokemon) {
                NoResultsView()
            } else {
                PokemonLazyList(filteredList, onNavigateToDetails, pokemonViewModel, filters.isGridView)
            }
        } else if (generations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { generations.size })

            // Carga lazy: solo la generación visible + adyacentes (prefetch)
            val currentPage = pagerState.currentPage
            LaunchedEffect(currentPage, generations) {
                val pagesToLoad = listOf(currentPage - 1, currentPage, currentPage + 1)
                pagesToLoad.forEach { page ->
                    val genId = generations.getOrNull(page)?.getGenerationIdFromUrl()
                    if (genId != null) {
                        pokemonViewModel.fetchPokemonForGeneration(genId)
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val genId = generations.getOrNull(page)?.getGenerationIdFromUrl()
                val list = pokemonByGenerationCache[genId]
                if (list == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(64.dp)) }
                } else {
                    PokemonLazyList(list, onNavigateToDetails, pokemonViewModel, filters.isGridView)
                }
            }
        }
    }
}

@Composable
fun PokemonLazyList(
    list: List<PokemonSummary>,
    onNavigateToDetails: (String) -> Unit,
    pokemonViewModel: PokemonViewModel,
    isGridView: Boolean = false
) {
    // collectAsState aqui en vez de en GenerationPagerScreen:
    // solo recompone el LazyColumn, no el pager ni el screen entero
    val recalledId by pokemonViewModel.recalledPokemonId.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridColumns = if (isLandscape) 5 else 3

    // Padding inferior dinámico: barra de navegación del sistema + espacio para el bottom bar de la app
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPadding = navBarBottom + 80.dp

    Crossfade(
        targetState = isGridView,
        animationSpec = tween(300),
        label = "listGridCrossfade"
    ) { grid ->
        if (grid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = list, key = { it.id }) { pokemon ->
                    PokemonGridItemCard(
                        pokemonSummary = pokemon,
                        isRecalled = recalledId == pokemon.id,
                        onRecallAndNavigate = {
                            pokemonViewModel.recalledPokemonId.value = pokemon.id
                            onNavigateToDetails(pokemon.id.toString())
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, bottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = list, key = { it.id }) { pokemon ->
                    PokemonListItemCard(
                        pokemonSummary = pokemon,
                        isRecalled = recalledId == pokemon.id,
                        onRecallAndNavigate = {
                            pokemonViewModel.recalledPokemonId.value = pokemon.id
                            onNavigateToDetails(pokemon.id.toString())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NoResultsView() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        GifAnimado(drawableId = R.drawable.missingno, modifier = Modifier.size(200.dp))
        Text("Sin resultados", color = CardBorder, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
