package com.david.pokedex_api

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import com.david.pokedex_api.api.model.NamedApiResource
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.extras.ExtrasBrowserScreen
import com.david.pokedex_api.ui.screen.extras.ExtrasSearchMenu
import com.david.pokedex_api.ui.screen.ficha.PokemonDetailScreen
import com.david.pokedex_api.ui.screen.ficha.composable.SectionPage
import com.david.pokedex_api.ui.screen.items.ItemBrowserScreen
import com.david.pokedex_api.ui.screen.items.ItemSearchMenu
import com.david.pokedex_api.ui.screen.items.SearchMenu
import com.david.pokedex_api.ui.screen.lista.GenerationPagerScreen
import com.david.pokedex_api.ui.screen.lista.composable.PokemonSearchMenu
import com.david.pokedex_api.ui.screen.movimientos.MoveBrowserScreen
import com.david.pokedex_api.ui.screen.movimientos.MoveSearchMenu
import com.david.pokedex_api.ui.screen.camara.CameraIdentifyScreen
import com.david.pokedex_api.ui.screen.regiones.RegionBrowserScreen
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.background_app_bottom
import com.david.pokedex_api.ui.theme.color_boton_busqueda
import com.david.pokedex_api.ui.theme.color_menu_busqueda2

// CompositionLocals para shared element transitions
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

object Routes {
    const val POKEMON_LIST = "pokemon_list"
    const val MOVE_BROWSER = "move_browser"
    const val ITEM_BROWSER = "item_browser"
    const val REGION_BROWSER = "region_browser"
    const val EXTRAS_BROWSER = "extras_browser"
    const val CAMERA_IDENTIFY = "camera_identify"
    const val POKEMON_DETAILS = "pokemon_details/{pokemonName}"

    fun pokemonDetails(pokemonName: String) = "pokemon_details/$pokemonName"
}

private data class NavItem(
    val route: String,
    val label: String,
    val iconResId: Int
)

private val navItems = listOf(
    NavItem(Routes.POKEMON_LIST, "Pokémon", R.drawable.normal2),
    NavItem(Routes.MOVE_BROWSER, "Movimientos", R.drawable.lucha2),
    NavItem(Routes.ITEM_BROWSER, "Items", R.drawable.pokeball_icon),
    NavItem(Routes.REGION_BROWSER, "Regiones", R.drawable.ic_location),
    NavItem(Routes.EXTRAS_BROWSER, "Extras", R.drawable.ic_info)
)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PokedexApp()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PokedexApp(
    pokemonViewModel: PokemonViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    // Detectar retorno de la ficha: restaurar card recalled
    var wasInDetail by remember { mutableStateOf(false) }
    LaunchedEffect(currentRoute) {
        if (wasInDetail && currentRoute != Routes.POKEMON_DETAILS) {
            delay(400) // esperar a que el shared element termine
            pokemonViewModel.recalledPokemonId.value = null
        }
        wasInDetail = currentRoute == Routes.POKEMON_DETAILS
    }
    var showBottomSheet by remember { mutableStateOf(false) }

    val isDetailRoute = currentRoute == Routes.POKEMON_DETAILS
    val isCameraRoute = currentRoute == Routes.CAMERA_IDENTIFY

    val pokemonFilters by pokemonViewModel.pokemonFilters.collectAsState()
    val isGridView = pokemonFilters.isGridView
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Tamaño de botones: mas compacto en pokemon_list (8 items) vs resto (6 items)
    val navButtonSize = if (currentRoute == Routes.POKEMON_LIST) 34.dp else 40.dp
    val navIconSize = if (currentRoute == Routes.POKEMON_LIST) 18.dp else 20.dp

    // Composable de botones de navegacion reutilizable
    @Composable
    fun NavBarButtons() {
        // 5 iconos de navegacion
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            Box(
                modifier = Modifier
                    .size(navButtonSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) Color.White.copy(alpha = 0.22f)
                        else Color.Transparent
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = item.iconResId),
                    contentDescription = item.label,
                    modifier = Modifier.size(navIconSize),
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Separador sutil
        Box(
            modifier = if (isLandscape) {
                Modifier.padding(vertical = 4.dp).height(1.dp).width(24.dp)
            } else {
                Modifier.padding(horizontal = 2.dp).width(1.dp).height(24.dp)
            }.background(Color.White.copy(alpha = 0.2f))
        )

        // Boton camara (solo en pokemon list)
        if (currentRoute == Routes.POKEMON_LIST) {
            Box(
                modifier = Modifier
                    .size(navButtonSize)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(Routes.CAMERA_IDENTIFY) {
                            launchSingleTop = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera_identify),
                    contentDescription = "Identificar Pokemon",
                    modifier = Modifier.size(navIconSize),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Toggle lista/grid (solo en pokemon list)
        if (currentRoute == Routes.POKEMON_LIST) {
            Box(
                modifier = Modifier
                    .size(navButtonSize)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pokemonViewModel.pokemonFilters.value = pokemonFilters.copy(isGridView = !isGridView)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isGridView) R.drawable.ic_view_list else R.drawable.ic_view_grid
                    ),
                    contentDescription = if (isGridView) "Vista lista" else "Vista grid",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Boton busqueda/filtros
        Box(
            modifier = Modifier
                .size(navButtonSize)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showBottomSheet = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                modifier = Modifier.size(navIconSize),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (!isDetailRoute && !isCameraRoute && !isLandscape) {
                val bottomPadH = if (currentRoute == Routes.POKEMON_LIST) 12.dp else 24.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(background_app_bottom)
                        .navigationBarsPadding()
                        .padding(horizontal = bottomPadH, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(color_boton_busqueda.copy(alpha = 0.92f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavBarButtons()
                    }
                }
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            val sharedTransitionScope = this

            Row(modifier = Modifier.fillMaxSize()) {
                // Barra lateral izquierda (solo landscape, no en ficha)
                if (isLandscape && !isDetailRoute && !isCameraRoute) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(color_boton_busqueda.copy(alpha = 0.92f))
                            .displayCutoutPadding()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            NavBarButtons()
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.POKEMON_LIST,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isCameraRoute) Modifier
                            else if (isLandscape) Modifier
                                .statusBarsPadding()
                                .navigationBarsPadding()
                            else Modifier
                                .statusBarsPadding()
                        )
                ) {
                composable(
                    route = Routes.POKEMON_LIST,
                    exitTransition = { fadeOut(tween(300)) },
                    popEnterTransition = { fadeIn(tween(300)) }
                ) {
                    CompositionLocalProvider(
                        LocalSharedTransitionScope provides sharedTransitionScope,
                        LocalAnimatedVisibilityScope provides this@composable
                    ) {
                        GenerationPagerScreen(
                            pokemonViewModel = pokemonViewModel,
                            onNavigateToDetails = { pokemonName ->
                                pokemonViewModel.resetDetailState()
                                navController.navigate(Routes.pokemonDetails(pokemonName))
                            }
                        )
                    }
                }
                composable(Routes.MOVE_BROWSER) {
                    MoveBrowserScreen(pokemonViewModel = pokemonViewModel)
                }
                composable(Routes.ITEM_BROWSER) {
                    ItemBrowserScreen(pokemonViewModel = pokemonViewModel)
                }
                composable(Routes.REGION_BROWSER) {
                    RegionBrowserScreen(pokemonViewModel = pokemonViewModel)
                }
                composable(Routes.EXTRAS_BROWSER) {
                    ExtrasBrowserScreen(pokemonViewModel = pokemonViewModel)
                }
                composable(
                    route = Routes.CAMERA_IDENTIFY,
                    enterTransition = { fadeIn(tween(300)) },
                    exitTransition = { fadeOut(tween(300)) },
                    popEnterTransition = { fadeIn(tween(300)) },
                    popExitTransition = { fadeOut(tween(300)) }
                ) {
                    CameraIdentifyScreen(
                        onNavigateToDetails = { pokemonName ->
                            pokemonViewModel.resetDetailState()
                            navController.navigate(Routes.pokemonDetails(pokemonName)) {
                                popUpTo(Routes.CAMERA_IDENTIFY) { inclusive = true }
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Routes.POKEMON_DETAILS,
                    arguments = listOf(navArgument("pokemonName") { type = NavType.StringType }),
                    enterTransition = { fadeIn(tween(300)) },
                    exitTransition = { fadeOut(tween(300)) },
                    popEnterTransition = { fadeIn(tween(300)) },
                    popExitTransition = { fadeOut(tween(300)) }
                ) { backStackEntry ->
                    val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                    if (pokemonName != null) {
                        CompositionLocalProvider(
                            LocalSharedTransitionScope provides sharedTransitionScope,
                            LocalAnimatedVisibilityScope provides this@composable
                        ) {
                            Box(Modifier.systemBarsPadding().displayCutoutPadding()) {
                                PokemonDetailScreen(
                                    pokemonViewModel = pokemonViewModel,
                                    pokemonName = pokemonName,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    } else {
                        Text("Error: Pokémon name not found.", modifier = Modifier.padding(16.dp))
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            } // NavHost
            } // Row
        } // SharedTransitionLayout
    } // Scaffold

    // BottomSheet contextual (solo fuera de la ficha)
    if (showBottomSheet && !isDetailRoute && !isCameraRoute) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = color_menu_busqueda2,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
                // --- Filtros de búsqueda según la ruta actual ---
                Column {
                    when (currentRoute) {
                        Routes.POKEMON_LIST -> {
                            val pf by pokemonViewModel.pokemonFilters.collectAsState()
                            PokemonSearchMenu(
                                searchQuery = pf.searchQuery,
                                selectedType1 = pf.selectedType1,
                                selectedType2 = pf.selectedType2,
                                availableTypes = ALL_POKEMON_TYPES,
                                showMegas = pf.showMegas,
                                showGigamax = pf.showGigamax,
                                showRegionals = pf.showRegionals,
                                showLegendaries = pf.showLegendaries,
                                showMythicals = pf.showMythicals,
                                evoChainLength = pf.evoChainLength,
                                onSearchQueryChanged = { v -> pokemonViewModel.pokemonFilters.update { it.copy(searchQuery = v) } },
                                onType1Changed = { v -> pokemonViewModel.pokemonFilters.update { it.copy(selectedType1 = v) } },
                                onType2Changed = { v -> pokemonViewModel.pokemonFilters.update { it.copy(selectedType2 = v) } },
                                onShowMegasChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(showMegas = v) }
                                    if (v) pokemonViewModel.fetchSpecialForms()
                                },
                                onShowGigamaxChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(showGigamax = v) }
                                    if (v) pokemonViewModel.fetchSpecialForms()
                                },
                                onShowRegionalsChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(showRegionals = v) }
                                    if (v) pokemonViewModel.fetchSpecialForms()
                                },
                                onShowLegendariesChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(showLegendaries = v) }
                                },
                                onShowMythicalsChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(showMythicals = v) }
                                },
                                onEvoChainLengthChanged = { v ->
                                    pokemonViewModel.pokemonFilters.update { it.copy(evoChainLength = v) }
                                }
                            )
                        }
                        Routes.MOVE_BROWSER -> {
                            val sq by pokemonViewModel.moveSearchQuery.collectAsState()
                            val st by pokemonViewModel.moveSelectedType.collectAsState()
                            val dc by pokemonViewModel.moveSelectedDamageClass.collectAsState()
                            val moveSummaries by pokemonViewModel.moveSummaries.collectAsState()
                            val isLoading by pokemonViewModel.isLoadingMoveSummaries.collectAsState()
                            MoveSearchMenu(
                                searchQuery = sq,
                                onSearchQueryChanged = { pokemonViewModel.moveSearchQuery.value = it },
                                selectedType = st,
                                onTypeChanged = { pokemonViewModel.moveSelectedType.value = it },
                                selectedDamageClass = dc,
                                onDamageClassChanged = { pokemonViewModel.moveSelectedDamageClass.value = it },
                                loadedCount = moveSummaries.size,
                                isLoading = isLoading
                            )
                        }
                        Routes.ITEM_BROWSER -> {
                            ItemSearchMenu(pokemonViewModel = pokemonViewModel)
                        }
                        Routes.REGION_BROWSER -> {
                            val sq by pokemonViewModel.regionSearchQuery.collectAsState()
                            SearchMenu(
                                title = "Filtrar Regiones",
                                query = sq,
                                onQueryChanged = { pokemonViewModel.regionSearchQuery.value = it },
                                placeholder = "Ej: Kanto, Johto..."
                            )
                        }
                        Routes.EXTRAS_BROWSER -> {
                            ExtrasSearchMenu(pokemonViewModel = pokemonViewModel)
                        }
                    }
                }
        }
    }
}

@Composable
private fun NavigationRow(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onNavigate(item.route) }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = item.iconResId),
                    contentDescription = item.label,
                    modifier = Modifier.size(22.dp),
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.45f)
                )
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DetailSectionSheet(
    pokemonViewModel: PokemonViewModel,
    isDarkType: Boolean,
    onSectionSelected: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedSection by pokemonViewModel.selectedDetailSection.collectAsState()
    val availableSectionNames by pokemonViewModel.availableDetailSections.collectAsState()
    val haptic = LocalHapticFeedback.current
    val contentColor = if (isDarkType) Color.White else CardBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Botón volver
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onNavigateBack() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Volver a la lista", color = contentColor, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(4.dp))

        // Grid de categorías (3 columnas) - solo secciones con contenido
        val sections = if (availableSectionNames.isNotEmpty()) {
            SectionPage.entries.filter { it.name in availableSectionNames }
        } else {
            SectionPage.entries
        }
        val columns = 3
        val rows = sections.chunked(columns)

        rows.forEach { rowSections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowSections.forEach { section ->
                    val isSelected = section.name == selectedSection
                    Text(
                        text = section.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) background_app.copy(alpha = 0.7f)
                                else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pokemonViewModel.selectedDetailSection.value = section.name
                                onSectionSelected()
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    )
                }
                repeat(columns - rowSections.size) {
                    Spacer(Modifier.weight(1f).padding(4.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

fun NamedApiResource.getGenerationIdFromUrl(): Int? {
    return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
}
