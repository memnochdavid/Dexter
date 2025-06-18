package com.david.pokedex_api.ui.screen.lista

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
import androidx.compose.runtime.derivedStateOf
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
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.getGenerationIdFromUrl
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
import com.david.pokedex_api.util.Lottie
import com.david.pokedex_api.ui.screen.lista.composable.PokemonListItemCard
import com.david.pokedex_api.ui.screen.lista.composable.PokemonSearchMenu
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.GifAnimado
import kotlin.collections.get
/*
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


    val globallyFilteredPokemonList by remember(filtersAreActive) {
        // Este bloque de 'remember' se ejecuta solo cuando 'filtersAreActive' cambia.
        // 'filtersAreActive' es un Boolean, que es un tipo estable.
        derivedStateOf {
            // Este bloque de 'derivedStateOf' se SUSCRIBE a los cambios de:
            //  - filtersAreActive (aunque ya está en el 'remember' exterior)
            //  - allLoadedPokemon
            //  - searchQuery
            //  - selectedType1
            //  - selectedType2
            // y RECALCULA su valor (el filtrado) solo cuando uno de estos estados leídos DENTRO de él cambia,
            // Y ADEMÁS, solo si el propio 'globallyFilteredPokemonList' es leído en alguna parte de la composición.

            if (filtersAreActive) { // Lectura de filtersAreActive
                Log.d("GenerationPagerScreen", "Filtering ${allLoadedPokemon.size} Pokémon globally. Query: '$searchQuery', Type1: '$selectedType1', Type2: '$selectedType2'")
                allLoadedPokemon.filter { pokemonSummary -> // Lectura de allLoadedPokemon
                    val nameMatches = if (searchQuery.isBlank()) { // Lectura de searchQuery
                        true
                    } else {
                        pokemonSummary.name.contains(searchQuery, ignoreCase = true)
                    }
                    val type1Matches = if (selectedType1 == NO_TYPE_SELECTED) { // Lectura de selectedType1
                        true
                    } else {
                        pokemonSummary.types.any { it.equals(selectedType1, ignoreCase = true) }
                    }
                    val type2Matches = if (selectedType2 == NO_TYPE_SELECTED) { // Lectura de selectedType2
                        true
                    } else {
                        // Lectura de selectedType1 y type1Matches (que depende de selectedType1)
                        if (selectedType1 == selectedType2 && type1Matches) {
                            true
                        } else {
                            // Lectura de selectedType2
                            pokemonSummary.types.any { it.equals(selectedType2, ignoreCase = true) }
                        }
                    }
                    nameMatches && type1Matches && type2Matches
                }
            } else {
                emptyList()
            }
        }
    }



    if (isLoadingGenerations && generations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app),
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
        val allLoadedPokemon = remember(pokemonByGenerationCache) {
            val flattened = pokemonByGenerationCache.values.flatten().distinctBy { it.id }
//            Log.d("PagerScreenAllLoaded", "Aegislash (681) in allLoadedPokemon: ${flattened.any { it.id == 681 && it.name.contains("Aegislash", ignoreCase = true) }}")
//            Log.d("PagerScreenAllLoaded", "Toxtricity (849) in allLoadedPokemon: ${flattened.any { it.id == 849 && it.name.contains("Toxtricity", ignoreCase = true) }}")
            flattened
        }

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
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(50.dp)) // Puedes añadir otros modificadores aquí si es necesario
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.Center
                        ) {
                            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                        }
                    } else if (listToDisplayWhenFiltersActive.isEmpty()) {
                        // Si los filtros están activos y la lista filtrada está vacía (y no estamos en el estado de carga anterior)
                        // Muestra "no encontrado"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp) // Espacio para FAB
                        ) {
                            itemsIndexed(
                                items = listToDisplayWhenFiltersActive,
                                key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                            ) { _, pokemonSummaryItem ->
                                PokemonListItemCard(
                                    pokemonSummary = pokemonSummaryItem,
                                    onItemClick = {
                                        Log.d(
                                            "Navigation",
                                            "Navigating to details for ID: ${pokemonSummaryItem.id}"
                                        )
                                        onNavigateToDetails(pokemonSummaryItem.id.toString())
                                    }
                                )
                            }
                            // Opcional: Indicador de carga al final si aún se están cargando más pokémon en segundo plano
                            // que podrían aparecer en la lista filtrada si coinciden.
                            if (isLoadingAnyPokemon && listToDisplayWhenFiltersActive.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
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
                                                Log.d(
                                                    "Navigation",
                                                    "Navigating to Pager details for ID: ${pokemonSummaryItem.id}"
                                                )
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
*/
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
    LaunchedEffect(key1 = generations, key2 = pokemonByGenerationCache.size) {
        if (generations.isNotEmpty()) {
            generations.forEach { generationResource ->
                val generationId = generationResource.getGenerationIdFromUrl()
                if (generationId != null && !pokemonByGenerationCache.containsKey(generationId)) {
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
        pokemonByGenerationCache.values.flatten().distinctBy { it.id }
    }

    val allGenerationsLoadedInCache = remember(generations, pokemonByGenerationCache) {
        generations.isNotEmpty() && generations.all { gen ->
            val genId = gen.getGenerationIdFromUrl()
            genId != null && pokemonByGenerationCache.containsKey(genId)
        }
    }

    val globallyFilteredPokemonList: List<PokemonSummary> by remember(filtersAreActive, allLoadedPokemon, searchQuery, selectedType1, selectedType2) {
        // Este derivedStateOf se recalculará si alguna de sus claves de remember (filtersAreActive, allLoadedPokemon, etc.) cambia.
        // O si uno de los estados leídos DENTRO del bloque derivedStateOf cambia, y `globallyFilteredPokemonList` es leído.
        derivedStateOf {
            if (filtersAreActive) {
                // Log.d("GenerationPagerScreen", "Filtering ${allLoadedPokemon.size} Pokémon globally. Query: '$searchQuery', Type1: '$selectedType1', Type2: '$selectedType2'")
                allLoadedPokemon.filter { pokemonSummary ->
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
                        if (selectedType1 == selectedType2 && type1Matches) { // Si ambos tipos son iguales y el tipo 1 coincide, ya es suficiente
                            true
                        } else {
                            pokemonSummary.types.any { it.equals(selectedType2, ignoreCase = true) }
                        }
                    }
                    nameMatches && type1Matches && type2Matches
                }
            } else {
                emptyList()
            }
        }
    }
    if (isLoadingGenerations && generations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app),
            contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
        }
    } else if (generations.isEmpty() && !isLoadingGenerations && error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app)
                .padding(16.dp), // Added padding for better text display
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No se pudieron cargar las generaciones. Por favor, inténtalo de nuevo más tarde.\nError: $error",
                textAlign = TextAlign.Center,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { generations.size })

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = color_boton_busqueda.copy(alpha = 0.8f),
                    contentColor = Color.Transparent, // El contenido del Lottie ya tiene sus colores
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(50.dp))
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

                    // Condición mejorada para el estado de carga/feedback durante la búsqueda
                    val stillLoadingPokemonsForSearch = isLoadingAnyPokemon && !allGenerationsLoadedInCache
                    val searchPerformedButNoResults = !isLoadingAnyPokemon && listToDisplayWhenFiltersActive.isEmpty() && (searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED || selectedType2 != NO_TYPE_SELECTED)


                    if (stillLoadingPokemonsForSearch && listToDisplayWhenFiltersActive.isEmpty()) {
                        // Muestra carga si:
                        // 1. Se están cargando Pokémon Y
                        // 2. NO todas las generaciones están en el caché Y
                        // 3. La lista filtrada global está actualmente vacía (porque aún no se han cargado suficientes datos)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                                Text(
                                    "Cargando más Pokémon para tu búsqueda...",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                    } else if (searchPerformedButNoResults) {
                        // Si los filtros están activos, no se está cargando nada, y la lista filtrada está vacía
                        // Muestra "no encontrado"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.TopCenter // Alineado arriba para mejor visualización
                        ) {
                            // FloatingActionButton para reabrir filtros fácilmente
                            FloatingActionButton(
                                onClick = { showBottomSheet = true },
                                containerColor = Color.Transparent,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(top = 32.dp) // Añade un poco de espacio superior
                                ) {
                                    GifAnimado(
                                        drawableId = R.drawable.missingno,
                                        modifier = Modifier.size(300.dp)
                                    )
                                    Text(
                                        "Sin resultados para la búsqueda actual.",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = CardBorder,
                                        fontSize = 30.sp,
                                    )
                                    if (!allGenerationsLoadedInCache) {
                                        Text(
                                            "(Algunos Pokémon podrían no estar cargados aún)",
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (listToDisplayWhenFiltersActive.isNotEmpty()) {
                        // Si los filtros están activos y hay resultados, muestra la lista única.
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp) // Espacio para FAB
                        ) {
                            itemsIndexed(
                                items = listToDisplayWhenFiltersActive,
                                key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                            ) { _, pokemonSummaryItem ->
                                PokemonListItemCard(
                                    pokemonSummary = pokemonSummaryItem,
                                    onItemClick = {
                                        Log.d(
                                            "Navigation",
                                            "Navigating to details for ID: ${pokemonSummaryItem.id}"
                                        )
                                        onNavigateToDetails(pokemonSummaryItem.id.toString())
                                    }
                                )
                            }
                            // Indicador de carga al final si aún se están cargando más pokémon
                            // Y no todas las generaciones se han cargado (podrían afectar los resultados del filtro)
                            if (isLoadingAnyPokemon && !allGenerationsLoadedInCache && listToDisplayWhenFiltersActive.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = color_agua_light)
                                        Text(
                                            " Completando resultados...",
                                            color = Color.White,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else if (isLoadingAnyPokemon && allGenerationsLoadedInCache && listToDisplayWhenFiltersActive.isEmpty()) {
                        // Caso especial: todas las generaciones cargadas, se está cargando algo (quizás un re-fetch), y no hay resultados
                        // Podría ser un loader simple si esta condición es posible en tu flujo.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold),
                            contentAlignment = Alignment.Center
                        ) {
                            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                        }
                    }

                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValuesFromScaffold) // Aplicar padding del Scaffold aquí
                    ) { pageIndex ->
                        val currentGenerationResource = generations.getOrNull(pageIndex)
                        val generationId = currentGenerationResource?.getGenerationIdFromUrl()

                        // Considera si este LaunchedEffect sigue siendo necesario con la carga proactiva.
                        // Si la carga proactiva ya está trayendo todas las generaciones, este podría
                        // ser redundante o para casos donde la carga proactiva falló para alguna generación específica.
                        LaunchedEffect(key1 = generationId, key2 = pokemonByGenerationCache.containsKey(generationId)) {
                            if (generationId != null && !pokemonByGenerationCache.containsKey(generationId) && !isLoadingAnyPokemon /* Considera si necesitas !isLoadingAnyPokemon aquí */) {
                                // Log.d("GenerationPagerScreen", "Pager: Page $pageIndex (Gen ID: $generationId), fetching Pokémon on demand.")
                                pokemonViewModel.fetchPokemonForGeneration(generationId)
                            }
                        }

                        val pokemonListForThisGeneration = pokemonByGenerationCache[generationId]

                        if (isLoadingAnyPokemon && pokemonListForThisGeneration == null) {
                            // Muestra carga si se está cargando CUALQUIER Pokémon y esta generación específica aún no tiene datos.
                            // Esto cubre el caso donde la carga proactiva está en progreso para esta gen o una carga bajo demanda.
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                            }
                        } else if (pokemonListForThisGeneration != null) {
                            // La generación ha sido cargada (o intentada cargar) y tenemos una lista (podría estar vacía)
                            if (pokemonListForThisGeneration.isEmpty()) {
                                // Se cargó, pero no hay Pokémon (o hubo un error silencioso para esta gen en el ViewModel)
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        GifAnimado( // O un Lottie/Imagen diferente
                                            drawableId = R.drawable.missingno, // Quizás un Snorlax durmiendo o algo así
                                            modifier = Modifier.size(200.dp)
                                        )
                                        Text(
                                            "No hay Pokémon para mostrar en ${currentGenerationResource?.name ?: "esta generación"}.",
                                            textAlign = TextAlign.Center,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp) // Espacio para FAB
                                ) {
                                    itemsIndexed(
                                        items = pokemonListForThisGeneration,
                                        key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                                    ) { _, pokemonSummaryItem ->
                                        PokemonListItemCard(
                                            pokemonSummary = pokemonSummaryItem,
                                            onItemClick = {
                                                Log.d(
                                                    "Navigation",
                                                    "Navigating to Pager details for ID: ${pokemonSummaryItem.id}"
                                                )
                                                onNavigateToDetails(pokemonSummaryItem.id.toString())
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
