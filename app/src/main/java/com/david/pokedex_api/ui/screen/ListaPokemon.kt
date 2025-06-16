package com.david.pokedex_api.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.model.content.CircleShape
import com.david.pokedex_api.R
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.getGenerationIdFromUrl
import com.david.pokedex_api.ui.composables.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.composables.Lottie
import com.david.pokedex_api.ui.composables.NO_TYPE_SELECTED
import com.david.pokedex_api.ui.composables.PokemonListItemCard
import com.david.pokedex_api.ui.composables.PokemonSearchMenu
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.GifAnimado

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun GenerationPagerScreen(
    pokemonViewModel: PokemonViewModel = viewModel(),
    onNavigateToDetails: (String) -> Unit
) {
    val generations by pokemonViewModel.generations.observeAsState(emptyList())
    val isLoadingGenerations by pokemonViewModel.isLoadingGenerations.observeAsState(false)
    val pokemonByGenerationCache by pokemonViewModel.pokemonByGenerationCache.observeAsState(emptyMap())
    val isLoadingAnyPokemon by pokemonViewModel.isLoadingPokemonForCurrentGeneration.observeAsState(false)
    val error by pokemonViewModel.error.observeAsState()
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedType1 by rememberSaveable { mutableStateOf(NO_TYPE_SELECTED) }
    var selectedType2 by rememberSaveable { mutableStateOf(NO_TYPE_SELECTED) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val availablePokemonTypes by pokemonViewModel.pokemonTypes.observeAsState(ALL_POKEMON_TYPES)

    // 1. Cargar la lista de generaciones
    LaunchedEffect(key1 = Unit) {
        if (generations.isEmpty()) {
            pokemonViewModel.fetchGenerations()
        }
    }

    // 2. Carga PROACTIVA de Pokémon para TODAS las generaciones una vez que la lista de generaciones esté disponible.
    // Esto es crucial para que `allLoadedPokemon` tenga datos para filtrar globalmente.
    LaunchedEffect(key1 = generations, key2 = pokemonByGenerationCache.size) { // Observar el tamaño del caché también
        if (generations.isNotEmpty()) {
            generations.forEach { generationResource ->
                val generationId = generationResource.getGenerationIdFromUrl()
                // Carga si el ID es válido, no está en caché Y no se está cargando activamente para este ID (el ViewModel debería manejar esto)
                if (generationId != null && !pokemonByGenerationCache.containsKey(generationId)) {
                    // Para evitar spam de logs si muchas generaciones se disparan a la vez:
                    // Log.d("GenerationPagerScreen", "Proactive fetch triggered for Gen ID: $generationId")
                    pokemonViewModel.fetchPokemonForGeneration(generationId)
                }
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Log.e("GenerationPagerScreen", "Error: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            pokemonViewModel.clearError()
        }
    }

    val filtersAreActive = searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED || selectedType2 != NO_TYPE_SELECTED

    val allLoadedPokemon = remember(pokemonByGenerationCache) {
        // Esta lista SIEMPRE contendrá todos los Pokémon de todas las generaciones que se hayan cargado en el caché.
        Log.d("GenerationPagerScreen", "Recalculating allLoadedPokemon. Cache size: ${pokemonByGenerationCache.size}, Total Pokémon: ${pokemonByGenerationCache.values.flatten().size}")
        pokemonByGenerationCache.values.flatten().distinctBy { it.id }
    }

    val globallyFilteredPokemonList = remember(allLoadedPokemon, searchQuery, selectedType1, selectedType2) {
        // Esta lista solo se calcula y se usa cuando los filtros están activos.
        if (filtersAreActive) {
            Log.d("GenerationPagerScreen", "Filtering ${allLoadedPokemon.size} Pokémon globally. Query: '$searchQuery', Type1: '$selectedType1', Type2: '$selectedType2'")
            val filtered = allLoadedPokemon.filter { pokemonSummary ->
                val nameMatches = if (searchQuery.isBlank()) {
                    true
                } else {
                    pokemonSummary.name.contains(searchQuery, ignoreCase = true)
                }
                val type1Matches = if (selectedType1 == NO_TYPE_SELECTED) {
                    true
                } else {
                    pokemonSummary.types.any { it.equals(selectedType1, ignoreCase = true) }
                }
                val type2Matches = if (selectedType2 == NO_TYPE_SELECTED) {
                    true
                } else {
                    if (selectedType1 == selectedType2 && type1Matches) {
                        true // Ya coincide si T1 y T2 son iguales y T1 coincide
                    } else {
                        pokemonSummary.types.any { it.equals(selectedType2, ignoreCase = true) }
                    }
                }
                nameMatches && type1Matches && type2Matches
            }
            Log.d("GenerationPagerScreen", "Global filter resulted in ${filtered.size} Pokémon.")
            filtered
        } else {
            emptyList() // No se usa si los filtros no están activos
        }
    }

    if (isLoadingGenerations && generations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(background_app),
            contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
        }
    } else if (generations.isEmpty() && !isLoadingGenerations && error != null) {
//        Box(modifier = Modifier.fillMaxSize().background(background_app), contentAlignment = Alignment.Center) {
//            Text("Could not load generations. Please try again.", textAlign = TextAlign.Center, color = Color.White)
//        }
    } else {
        val pagerState = rememberPagerState(pageCount = { generations.size })

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = color_boton_busqueda.copy(alpha = 0.8f),
                    contentColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(50.dp)) // Puedes añadir otros modificadores aquí si es necesario
                ) {
                    Lottie(rawResId = R.raw.search, modifier = Modifier.fillMaxSize())
                }
            }
        ) { paddingValuesFromScaffold ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background_app)
                // El padding del Scaffold se aplicará al contenido específico (LazyColumn o HorizontalPager)
            ) {
                if (filtersAreActive) {
                    // --- VISTA DE LISTA ÚNICA FILTRADA ---
                    val listToDisplayWhenFiltersActive = globallyFilteredPokemonList

                    // Muestra carga si se está cargando CUALQUIER Pokémon Y la lista filtrada global está vacía
                    // Y (el caché general de pokémon está vacío O allLoadedPokemon está vacío,
                    // para cubrir el caso en que se activan filtros antes de que se haya cargado algo)
                    if (isLoadingAnyPokemon && listToDisplayWhenFiltersActive.isEmpty() && (allLoadedPokemon.isEmpty() || pokemonByGenerationCache.isEmpty())){
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.Center
                        ) {
                            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                        }
                    } else if (listToDisplayWhenFiltersActive.isEmpty()) {
                        // Si los filtros están activos y la lista filtrada está vacía (y no estamos en el estado de carga anterior)
                        // Muestra "no encontrado"
                        Box(
                            modifier = Modifier.fillMaxSize().padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            FloatingActionButton( // Permite reabrir filtros si no hay resultados
                                onClick = { showBottomSheet = true },
                                containerColor = Color.Transparent, // Fondo transparente
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp) // Sin elevación
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
//                                    Lottie(
//                                        rawResId = R.raw.notfound, // Lottie para "no encontrado"
//                                        modifier = Modifier.size(300.dp),
//                                    )
                                    GifAnimado(
                                        drawableId = R.drawable.missingno,
                                        modifier = Modifier.size(300.dp)
                                    )
                                    Text(
                                        "Sin resultados para la búsqueda actual",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CardBorder,
                                        fontSize = 30.sp,
                                    )
                                }
                            }
                        }
                    } else {
                        // Si los filtros están activos y hay resultados, muestra la lista única.
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(paddingValuesFromScaffold),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp) // Espacio para FAB
                        ) {
                            itemsIndexed(
                                items = listToDisplayWhenFiltersActive,
                                key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                            ) { _, pokemonSummaryItem ->
                                PokemonListItemCard(
                                    pokemonSummary = pokemonSummaryItem,
                                    onItemClick = {
                                        Log.d("Navigation", "Navigating to details for ID: ${pokemonSummaryItem.id}")
                                        onNavigateToDetails(pokemonSummaryItem.id.toString())
                                    }
                                )
                            }
                            // Opcional: Indicador de carga al final si aún se están cargando más pokémon en segundo plano
                            // que podrían aparecer en la lista filtrada si coinciden.
                            if (isLoadingAnyPokemon && listToDisplayWhenFiltersActive.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) { CircularProgressIndicator(color = color_agua_light) }
                                }
                            }
                        }
                    }
                } else {
                    // --- VISTA DE PAGER POR GENERACIÓN (SIN FILTROS ACTIVOS) ---
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValuesFromScaffold) // Aplicar padding del Scaffold aquí
                    ) { pageIndex ->
                        val currentGenerationResource = generations.getOrNull(pageIndex)
                        val generationId = currentGenerationResource?.getGenerationIdFromUrl()

                        LaunchedEffect(key1 = generationId, key2 = pokemonByGenerationCache.containsKey(generationId)) {
                            // Carga Pokémon para esta generación si aún no están en caché y no se está cargando ya
                            if (generationId != null && !pokemonByGenerationCache.containsKey(generationId) && !isLoadingAnyPokemon) {
                                Log.d("GenerationPagerScreen", "Pager: Page $pageIndex (Gen ID: $generationId), fetching Pokémon.")
                                pokemonViewModel.fetchPokemonForGeneration(generationId)
                            }
                        }

                        val rawPokemonListForThisGeneration = pokemonByGenerationCache[generationId]
                        // Cuando no hay filtros activos, la lista a mostrar es simplemente la de la generación actual.
                        val displayPokemonListForPager = rawPokemonListForThisGeneration ?: emptyList()

                        if (isLoadingAnyPokemon && rawPokemonListForThisGeneration == null) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                            }
                        } else if (rawPokemonListForThisGeneration != null) { // Asegura que la generación ha sido procesada (incluso si está vacía)
                            if (displayPokemonListForPager.isEmpty()) {
                                // Se cargó la generación, pero está vacía o hubo un error silencioso para esta gen
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    FloatingActionButton(
                                        onClick = { showBottomSheet = true },
                                        containerColor = Color.Transparent,
                                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Lottie(
                                                rawResId = R.raw.notfound,
                                                modifier = Modifier.size(300.dp),
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp)
                                ) {
                                    itemsIndexed(
                                        items = displayPokemonListForPager,
                                        key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                                    ) { _, pokemonSummaryItem ->
                                        PokemonListItemCard(
                                            pokemonSummary = pokemonSummaryItem,
                                            onItemClick = {
                                                Log.d("Navigation", "Navigating to Pager details for ID: ${pokemonSummaryItem.id}")
                                                onNavigateToDetails(pokemonSummaryItem.id.toString())
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (generationId != null) {
                            // Estado inicial para una página/generación que aún no se ha intentado cargar.
//                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                                Text(
//                                    "Desliza o espera para cargar Pokémon de ${currentGenerationResource?.name ?: "esta generación"}.",
//                                    textAlign = TextAlign.Center,
//                                    color = Color.White,
//                                    modifier = Modifier.padding(16.dp)
//                                )
//                            }
                        }
                        // Considera un 'else' aquí si generationId es null, aunque no debería ocurrir si 'generations' está poblada.
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    shape =  BottomSheetDefaults.HiddenShape ,  //RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    dragHandle = { },
                    containerColor = color_menu_busqueda2,
                    scrimColor = Color.Transparent
                ) {
                    PokemonSearchMenu(
                        searchQuery = searchQuery,
                        selectedType1 = selectedType1,
                        selectedType2 = selectedType2,
                        availableTypes = availablePokemonTypes,
                        onSearchQueryChanged = { searchQuery = it },
                        onType1Changed = { newType -> selectedType1 = newType },
                        onType2Changed = { newType -> selectedType2 = newType }
                    )
                }
            }
        }
    }
}
