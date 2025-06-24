package com.david.pokedex_api.ui.screen.lista

// Importa tus otros composables necesarios como FilterChips, PokemonListItem, Lottie, etc.
// import com.david.pokedex_api.ui.theme.color_boton_busqueda // Si está definido en otro lado
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.camera.composable.SimpleCameraView
import com.david.pokedex_api.camera.composable.takePhoto
import com.david.pokedex_api.camera.viewModel.PokemonVisionViewModel
import com.david.pokedex_api.getGenerationIdFromUrl
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
import com.david.pokedex_api.ui.screen.lista.composable.PokemonListItemCard
import com.david.pokedex_api.ui.screen.lista.composable.PokemonSearchMenu
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.color_agua_light
import com.david.pokedex_api.ui.theme.color_boton_busqueda
import com.david.pokedex_api.ui.theme.color_menu_busqueda2
import com.david.pokedex_api.util.GifAnimado
import com.david.pokedex_api.util.Lottie
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//ANTERIOR AL SOPORTE DE LA CÁMARA

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun GenerationPagerScreen(
    pokemonViewModel: PokemonViewModel = viewModel(),
//    pokemonVisionViewModel: PokemonVisionViewModel,
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
                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp), // Espacio para FAB

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


/*
//CON SOPORTE PARA LA CAMARA
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GenerationPagerScreen(
    pokemonViewModel: PokemonViewModel = viewModel(),
    pokemonVisionViewModel: PokemonVisionViewModel, // Se inyecta o se obtiene del NavGraph
    onNavigateToDetails: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Estados del ViewModel Original (Pokedex) ---
    val generations by pokemonViewModel.generations.observeAsState(emptyList())
    val isLoadingGenerations by pokemonViewModel.isLoadingGenerations.observeAsState(false)
    val pokemonByGenerationCache by pokemonViewModel.pokemonByGenerationCache.observeAsState(emptyMap())
    val isLoadingAnyPokemon by pokemonViewModel.isLoadingPokemonForCurrentGeneration.observeAsState(false)
    val error by pokemonViewModel.error.observeAsState()
    val availablePokemonTypes by pokemonViewModel.pokemonTypes.observeAsState(ALL_POKEMON_TYPES)

    // --- Estados para Permisos de Cámara ---
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var userAttemptedToOpenCam by rememberSaveable { mutableStateOf(false) }

    // --- Estados para la funcionalidad de la cámara ---
    var showCameraView by rememberSaveable { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCameraReadyForCapture by remember { mutableStateOf(false) }
    var isTakingPhoto by remember { mutableStateOf(false) } // Para el feedback inmediato de la toma de foto
    val cameraExecutor: ExecutorService by remember { mutableStateOf(Executors.newSingleThreadExecutor()) }

    // --- Estados del ViewModel de Visión (Cámara) ---
    val identifiedPokemonNameOrId by pokemonVisionViewModel.identifiedPokemonNameOrId.collectAsStateWithLifecycle()
    val isLlmLoading by pokemonVisionViewModel.isLlmLoading.collectAsStateWithLifecycle()
    val llmError by pokemonVisionViewModel.llmError.collectAsStateWithLifecycle()
    // val llmPokemonResponse by pokemonVisionViewModel.llmPokemonResponse.collectAsStateWithLifecycle() // Para mostrar la descripción completa si es necesario

    // --- Estados para el BottomSheet de Búsqueda (Pokedex) ---
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedType1 by rememberSaveable { mutableStateOf(NO_TYPE_SELECTED) }
    var selectedType2 by rememberSaveable { mutableStateOf(NO_TYPE_SELECTED) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // --- Launcher para Permisos de Cámara ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (isGranted) {
                if (userAttemptedToOpenCam) showCameraView = true
            } else {
                Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_LONG).show()
            }
            userAttemptedToOpenCam = false
        }
    )

    // --- LaunchedEffects para Carga de Datos (Pokedex) ---
    LaunchedEffect(key1 = Unit) {
        if (generations.isEmpty() && !isLoadingGenerations) {
            pokemonViewModel.fetchGenerations()
        }
    }
    LaunchedEffect(key1 = generations, key2 = pokemonByGenerationCache.size) {
        if (generations.isNotEmpty()) {
            generations.forEach { generationResource ->
                val generationId = generationResource.getGenerationIdFromUrl()
                if (generationId != null && !pokemonByGenerationCache.containsKey(generationId) && !isLoadingAnyPokemon) {
                    pokemonViewModel.fetchPokemonForGeneration(generationId)
                }
            }
        }
    }
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            pokemonViewModel.clearError()
        }
    }
    LaunchedEffect(hasCameraPermission, showCameraView) {
        if (showCameraView && !hasCameraPermission) {
            showCameraView = false
            Toast.makeText(context, "Permiso de cámara revocado.", Toast.LENGTH_SHORT).show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            Log.d("GenerationPagerScreen", "Cerrando cameraExecutor.")
            cameraExecutor.shutdown()
        }
    }

    // Lista de todos los Pokémon cargados para buscar el identificado por el LLM
    val allLoadedPokemonSummaries = remember(pokemonByGenerationCache) {
        pokemonByGenerationCache.values.flatten().distinctBy { it.id }
    }
    LaunchedEffect(identifiedPokemonNameOrId) {
        identifiedPokemonNameOrId?.let { identifier ->
            Log.i("VisionNav", "LLM identificó: '$identifier'")
            val trimmedIdentifier = identifier.trim() // Limpiar espacios al inicio/final

            val foundPokemon: PokemonSummary? = allLoadedPokemonSummaries.find { summary ->
                // Intenta coincidir por ID si el identificador es un número
                val isIdMatch = summary.id.toString() == trimmedIdentifier

                // Intenta coincidir por nombre (ignorando mayúsculas/minúsculas y posibles acentos/caracteres especiales)
                // Para una mejor coincidencia de nombres, podrías normalizar ambas cadenas:
                // val normalizedSummaryName = summary.name.normalizeForSearch()
                // val normalizedIdentifier = trimmedIdentifier.normalizeForSearch()
                // val isNameMatch = normalizedSummaryName.equals(normalizedIdentifier, ignoreCase = true)
                // Por ahora, una comparación simple ignorando mayúsculas:
                val isNameMatch = summary.name.equals(trimmedIdentifier, ignoreCase = true)

                isIdMatch || isNameMatch
            }

            if (foundPokemon != null) {
                Log.i("VisionNav", "Pokémon encontrado en caché: ${foundPokemon.name}, ID: ${foundPokemon.id}. Navegando...")
                Toast.makeText(context, "¡Es ${foundPokemon.name}!", Toast.LENGTH_SHORT).show()
                onNavigateToDetails(foundPokemon.id.toString())
            } else {
                Log.w("VisionNav", "Pokémon '$trimmedIdentifier' identificado por LLM no encontrado en la caché local o no cargado.")
                // Opcional: Mostrar la respuesta completa del LLM si no se pudo parsear/encontrar
                // val fullLlmResponse = pokemonVisionViewModel.llmPokemonResponse.value // Asumiendo que aún lo guardas
                // val messageToShow = if (fullLlmResponse != null && fullLlmResponse != trimmedIdentifier) {
                //    "LLM dijo: \"$fullLlmResponse\".\nNo se encontró '$trimmedIdentifier' en la Pokédex."
                // } else {
                //    "Pokémon '$trimmedIdentifier' no encontrado en tu Pokédex actual."
                // }
                val messageToShow = "Pokémon '$trimmedIdentifier' no encontrado en tu Pokédex."
                Toast.makeText(context, messageToShow, Toast.LENGTH_LONG).show()
            }
            // Limpiar los estados del ViewModel para evitar re-navegación o mostrar mensajes antiguos
            pokemonVisionViewModel.clearIdentifiedPokemon()
            pokemonVisionViewModel.clearLlmResponse() // Si aún usas llmPokemonResponse para el mensaje completo
        }
    }

    // Efecto para manejar errores generales del LLM (que no sean "no encontrado")
    LaunchedEffect(llmError) {
        llmError?.let { errorMsg ->
            // Evitar mostrar el error si ya hemos navegado o estamos a punto de hacerlo
            if (identifiedPokemonNameOrId == null) {
                Log.e("VisionNav", "Error del LLM: $errorMsg")
                Toast.makeText(context, "Análisis de Pokémon fallido: $errorMsg", Toast.LENGTH_LONG).show()
            }
            pokemonVisionViewModel.clearLlmError()
        }
    }


    // --- Lógica de Filtros (Pokedex) ---
    val filtersAreActive = searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED || selectedType2 != NO_TYPE_SELECTED

    val allGenerationsLoadedInCache = remember(generations, pokemonByGenerationCache) {
        generations.isNotEmpty() && generations.all { gen ->
            val genId = gen.getGenerationIdFromUrl()
            genId != null && pokemonByGenerationCache.containsKey(genId)
        }
    }

    val globallyFilteredPokemonList: List<PokemonSummary> by remember(filtersAreActive, allLoadedPokemonSummaries, searchQuery, selectedType1, selectedType2) {
        derivedStateOf {
            if (filtersAreActive) {
                allLoadedPokemonSummaries.filter { pokemonSummary ->
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

    // --- Lógica de UI ---
    if (showCameraView) {
        // --- UI CUANDO LA CÁMARA ESTÁ ACTIVA ---
        Box(modifier = Modifier.fillMaxSize()) {
            SimpleCameraView(
                executor = cameraExecutor,
                onImageCaptureReady = { captureInstance ->
                    imageCapture = captureInstance
                    isCameraReadyForCapture = true
                    Log.d("GenerationPagerScreen", "ImageCapture instancia está lista desde SimpleCameraView.")
                },
                onError = { exception ->
                    Log.e("GenerationPagerScreen", "Error desde SimpleCameraView: ${exception.message}", exception)
                    Toast.makeText(context, "Error de Cámara: ${exception.message}", Toast.LENGTH_LONG).show()
                    showCameraView = false
                    isCameraReadyForCapture = false
                }
            )

            FloatingActionButton(
                onClick = {
                    if (isCameraReadyForCapture && imageCapture != null && !isTakingPhoto && !isLlmLoading) {
                        Log.d("GenerationPagerScreen", "FAB Tomar Foto: Iniciando captura.")
                        isTakingPhoto = true // Podrías usar isLlmLoading para un feedback más general

                        // Limpiar estados ANTES de la nueva foto/análisis
                        pokemonVisionViewModel.clearIdentifiedPokemon()
                        pokemonVisionViewModel.clearLlmResponse()
                        pokemonVisionViewModel.clearLlmError()
                        pokemonVisionViewModel.clearLabelingError() // Si lo usas

                        takePhoto(
                            context = context,
                            imageCapture = imageCapture!!,
                            executor = cameraExecutor,
                            onImageCaptured = { bitmap ->
                                Log.d("GenerationPagerScreen", "Foto capturada, enviando a ViewModel.")
                                pokemonVisionViewModel.analyzeImageForPokemon(bitmap) // Esto activará el flujo de identificación
                                showCameraView = false // Ocultar cámara después de la captura
                                isTakingPhoto = false
                                isCameraReadyForCapture = false // Resetear, se re-evaluará si la cámara se abre de nuevo
                            },
                            onError = { exception ->
                                Log.e("GenerationPagerScreen", "Error al tomar foto: ${exception.message}", exception)
                                Toast.makeText(context, "Error al tomar foto: ${exception.message}", Toast.LENGTH_LONG).show()
                                isTakingPhoto = false
                                showCameraView = false // Cerrar cámara en error de captura también
                            }
                        )
                    } else {
                        var message = "Esperando cámara..."
                        if (isTakingPhoto) message = "Procesando foto..."
                        else if (isLlmLoading) message = "Analizando Pokémon..."
                        else if (imageCapture == null) message = "Configurando ImageCapture..."
                        else if (!isCameraReadyForCapture) message = "Cámara no lista aún..."
                        Log.d("GenerationPagerScreen", "FAB Tomar Foto: No se puede tomar foto. Razón: $message")
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .size(80.dp),
                containerColor = if (isCameraReadyForCapture && !isTakingPhoto && !isLlmLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                contentColor = Color.White
            ) {
                if (isTakingPhoto || isLlmLoading) { // Mostrar progreso si se está tomando foto O analizando
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = "Tomar Foto",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Botón para cerrar la cámara (similar al FAB original)
            FloatingActionButton(
                onClick = {
                    Log.d("GenerationPagerScreen", "FAB Cerrar Cámara presionado.")
                    showCameraView = false
                    isCameraReadyForCapture = false // Resetear estado al cerrar manualmente
                    imageCapture = null // Limpiar la instancia de imageCapture
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(56.dp),
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Close, "Cerrar Cámara")
            }
        } // Fin del Box(modifier = Modifier.fillMaxSize()) para la CameraView
    } else {
        // --- UI CUANDO LA CÁMARA NO ESTÁ ACTIVA (Pokedex normal) ---
        if (isLoadingGenerations && generations.isEmpty() && error == null) {
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
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No se pudieron cargar las generaciones. Por favor, inténtalo de nuevo más tarde.\nError: $error",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else if (generations.isEmpty() && !isLoadingGenerations && error == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background_app)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay generaciones disponibles para mostrar.",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        } else {
            // Cuando las generaciones están cargadas o hay datos parciales.
            val pagerState = rememberPagerState(pageCount = { generations.size.coerceAtLeast(0) })

            Scaffold(
                floatingActionButton = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    Log.d("GenerationPagerScreen", "FAB Abrir Cámara: Permiso OK, mostrando cámara.")
                                    showCameraView = true
                                } else {
                                    Log.d("GenerationPagerScreen", "FAB Abrir Cámara: Solicitando permiso.")
                                    userAttemptedToOpenCam = true
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Filled.AccountCircle, "Identificar Pokémon con Cámara")
                        }

                        FloatingActionButton(
                            onClick = { showBottomSheet = true },
                            containerColor = color_boton_busqueda.copy(alpha = 0.8f),
                            contentColor = Color.Transparent,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(50.dp))
                        ) {
                            Lottie(rawResId = R.raw.search, modifier = Modifier.fillMaxSize())
                        }
                    }
                },
                floatingActionButtonPosition = FabPosition.End
            ) { paddingValuesFromScaffold ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background_app)
                ) {
                    if (filtersAreActive) {
                        val listToDisplayWhenFiltersActive = globallyFilteredPokemonList
                        val stillLoadingPokemonsForSearch = isLoadingAnyPokemon && !allGenerationsLoadedInCache
                        val searchPerformedButNoResults = !isLoadingAnyPokemon && listToDisplayWhenFiltersActive.isEmpty() && (searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED || selectedType2 != NO_TYPE_SELECTED)

                        if (stillLoadingPokemonsForSearch && listToDisplayWhenFiltersActive.isEmpty()) {
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
                                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                                        color = Color.White, modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        } else if (searchPerformedButNoResults) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValuesFromScaffold),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                FloatingActionButton(
                                    onClick = { showBottomSheet = true },
                                    containerColor = Color.Transparent,
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(top = 32.dp)
                                    ) {
                                        GifAnimado(drawableId = R.drawable.missingno, modifier = Modifier.size(300.dp))
                                        Text(
                                            "Sin resultados para la búsqueda actual.",
                                            textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold, color = CardBorder, fontSize = 30.sp,
                                        )
                                        if (!allGenerationsLoadedInCache) {
                                            Text(
                                                "(Algunos Pokémon podrían no estar cargados aún)",
                                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                                                color = Color.LightGray, modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (listToDisplayWhenFiltersActive.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValuesFromScaffold),
                                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp + 56.dp + 16.dp),
                            ) {
                                itemsIndexed(
                                    items = listToDisplayWhenFiltersActive,
                                    key = { _, pokemonSummaryItem -> pokemonSummaryItem.id }
                                ) { _, pokemonSummaryItem ->
                                    PokemonListItemCard(
                                        pokemonSummary = pokemonSummaryItem,
                                        onItemClick = { onNavigateToDetails(pokemonSummaryItem.id.toString()) }
                                    )
                                }
                                if (isLoadingAnyPokemon && !allGenerationsLoadedInCache && listToDisplayWhenFiltersActive.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(color = color_agua_light)
                                            Text(" Completando resultados...", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }
                                }
                            }
                        } else if (isLoadingAnyPokemon && allGenerationsLoadedInCache && listToDisplayWhenFiltersActive.isEmpty()) {
                            // Este caso es cuando la búsqueda está activa, todos los pokémon base están cargados,
                            // pero la lista filtrada está vacía y aún podríamos estar "cargando" (aunque es menos probable aquí
                            // si allGenerationsLoadedInCache es true). Se muestra un Lottie.
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
                        // --- VISTA DE PAGER POR GENERACIONES (CUANDO LOS FILTROS NO ESTÁN ACTIVOS) ---
                        if (generations.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValuesFromScaffold)
                            ) { pageIndex ->
                                val currentGenerationResource = generations.getOrNull(pageIndex)
                                val generationId = currentGenerationResource?.getGenerationIdFromUrl()

                                LaunchedEffect(key1 = generationId, key2 = pokemonByGenerationCache.containsKey(generationId)) {
                                    if (generationId != null && !pokemonByGenerationCache.containsKey(generationId) && !isLoadingAnyPokemon) {
                                        Log.d("GenerationPagerScreen", "Pager: Page $pageIndex (Gen ID: $generationId), fetching Pokémon on demand.")
                                        pokemonViewModel.fetchPokemonForGeneration(generationId)
                                    }
                                }

                                val pokemonListForThisGeneration = pokemonByGenerationCache[generationId]

                                if (isLoadingAnyPokemon && pokemonListForThisGeneration == null) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(200.dp))
                                    }
                                } else if (pokemonListForThisGeneration != null) {
                                    if (pokemonListForThisGeneration.isEmpty() && !isLoadingAnyPokemon) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
                                                GifAnimado(drawableId = R.drawable.missingno, modifier = Modifier.size(200.dp))
                                                Text(
                                                    "No hay Pokémon para mostrar en ${currentGenerationResource?.name ?: "esta generación"}.",
                                                    textAlign = TextAlign.Center, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp + 56.dp + 16.dp + 56.dp + 16.dp)
                                        ) {
                                            itemsIndexed(
                                                items = pokemonListForThisGeneration,
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
                                            if (pokemonViewModel.isFetchingForGenerationId(generationId)) { // Usando la función del ViewModel
                                                item {
                                                    Row(modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                                                        CircularProgressIndicator(color = color_agua_light)
                                                        Text(" Cargando más...", color = Color.White, modifier = Modifier.padding(start = 8.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (!isLoadingAnyPokemon && generationId != null) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            "Datos no disponibles para ${currentGenerationResource?.name ?: "esta generación"}. Intenta de nuevo.",
                                            color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            } // Fin HorizontalPager
                        } else if (!isLoadingGenerations) { // Si no hay generaciones y no se están cargando
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValuesFromScaffold), contentAlignment = Alignment.Center) {
                                Text("No hay generaciones para mostrar.", textAlign = TextAlign.Center, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } // Fin Column principal del Scaffold

                // Diálogo de carga general del LLM (si no estamos en la cámara y el LLM está procesando)
                if (isLlmLoading && !showCameraView) {
                    Dialog(onDismissRequest = { /* No se puede descartar mientras carga */ }) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(100.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Analizando Pokémon...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState,
                        shape = BottomSheetDefaults.HiddenShape,
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
            } // Fin Scaffold
        } // Fin del else (cuando las generaciones están cargadas o hay datos parciales)
    } // Fin del else (cuando la cámara NO está activa)
} // Fin GenerationPagerScreen Composable

*/




