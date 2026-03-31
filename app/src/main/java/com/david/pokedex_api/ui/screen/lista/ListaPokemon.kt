package com.david.pokedex_api.ui.screen.lista

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.getGenerationIdFromUrl
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
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

    val searchQuery by pokemonViewModel.pokemonSearchQuery.collectAsState()
    val selectedType1 by pokemonViewModel.pokemonSelectedType1.collectAsState()
    val selectedType2 by pokemonViewModel.pokemonSelectedType2.collectAsState()
    val showMegas by pokemonViewModel.pokemonShowMegas.collectAsState()
    val showGigamax by pokemonViewModel.pokemonShowGigamax.collectAsState()
    val specialForms by pokemonViewModel.specialFormsSummaries.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        if (generations.isEmpty()) pokemonViewModel.fetchGenerations()
    }

    LaunchedEffect(generations) {
        generations.forEach { gen ->
            val id = gen.getGenerationIdFromUrl()
            if (id != null && !pokemonByGenerationCache.containsKey(id)) {
                pokemonViewModel.fetchPokemonForGeneration(id)
            }
        }
    }

    val isSearching = searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED || selectedType2 != NO_TYPE_SELECTED || showMegas || showGigamax
    val filteredList = remember(pokemonByGenerationCache, searchQuery, selectedType1, selectedType2, showMegas, showGigamax, specialForms) {
        if (!isSearching) emptyList()
        else {
            val basePokemon = if (!showMegas && !showGigamax) {
                pokemonByGenerationCache.values.flatten().distinctBy { it.id }
            } else {
                emptyList()
            }
            val megaForms = if (showMegas) specialForms.filter { it.name.contains("-mega") } else emptyList()
            val gmaxForms = if (showGigamax) specialForms.filter { it.name.contains("-gmax") } else emptyList()

            (basePokemon + megaForms + gmaxForms).distinctBy { it.id }.filter { p ->
                (searchQuery.isBlank() || p.name.contains(searchQuery, true)) &&
                (selectedType1 == NO_TYPE_SELECTED || p.types.any { it.equals(selectedType1, true) }) &&
                (selectedType2 == NO_TYPE_SELECTED || p.types.any { it.equals(selectedType2, true) })
            }
        }
    }

    val recalledId by pokemonViewModel.recalledPokemonId.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(background_app_gradient)) {
        if (isSearching) {
            if (filteredList.isEmpty() && !isLoadingAnyPokemon) {
                NoResultsView()
            } else {
                PokemonLazyList(filteredList, recalledId, onNavigateToDetails, pokemonViewModel)
            }
        } else if (generations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { generations.size })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val genId = generations.getOrNull(page)?.getGenerationIdFromUrl()
                val list = pokemonByGenerationCache[genId]
                if (list == null) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(64.dp)) }
                } else {
                    PokemonLazyList(list, recalledId, onNavigateToDetails, pokemonViewModel)
                }
            }
        }
    }
}

@Composable
fun PokemonLazyList(
    list: List<PokemonSummary>,
    recalledId: Int?,
    onNavigateToDetails: (String) -> Unit,
    pokemonViewModel: PokemonViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
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

@Composable
fun NoResultsView() {
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        GifAnimado(drawableId = R.drawable.missingno, modifier = Modifier.size(200.dp))
        Text("Sin resultados", color = CardBorder, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
