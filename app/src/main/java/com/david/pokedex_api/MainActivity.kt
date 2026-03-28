package com.david.pokedex_api

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.david.pokedex_api.ui.screen.regiones.RegionBrowserScreen
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.color_boton_busqueda
import com.david.pokedex_api.ui.theme.color_menu_busqueda2


object Routes {
    const val POKEMON_LIST = "pokemon_list"
    const val MOVE_BROWSER = "move_browser"
    const val ITEM_BROWSER = "item_browser"
    const val REGION_BROWSER = "region_browser"
    const val EXTRAS_BROWSER = "extras_browser"
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp(
    pokemonViewModel: PokemonViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current
    var showBottomSheet by remember { mutableStateOf(false) }

    val isDetailRoute = currentRoute == Routes.POKEMON_DETAILS

    Scaffold(
        floatingActionButton = {
            if (!isDetailRoute) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showBottomSheet = true
                    },
                    containerColor = color_boton_busqueda.copy(alpha = 0.85f),
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(50.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Menú",
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.POKEMON_LIST,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.POKEMON_LIST) {
                GenerationPagerScreen(
                    pokemonViewModel = pokemonViewModel,
                    onNavigateToDetails = { pokemonName ->
                        pokemonViewModel.resetDetailState()
                        navController.navigate(Routes.pokemonDetails(pokemonName))
                    }
                )
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
                route = Routes.POKEMON_DETAILS,
                arguments = listOf(navArgument("pokemonName") { type = NavType.StringType })
            ) { backStackEntry ->
                val pokemonName = backStackEntry.arguments?.getString("pokemonName")
                if (pokemonName != null) {
                    PokemonDetailScreen(
                        pokemonViewModel = pokemonViewModel,
                        pokemonName = pokemonName,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    Text("Error: Pokémon name not found.", modifier = Modifier.padding(16.dp))
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
        }
    }

    // BottomSheet contextual (solo fuera de la ficha)
    if (showBottomSheet && !isDetailRoute) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = color_menu_busqueda2,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
                // --- Listas: navegación + búsqueda ---
                Column {
                    // Navegación entre secciones
                    NavigationRow(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(4.dp))

                    // Búsqueda/filtros según la ruta actual
                    when (currentRoute) {
                        Routes.POKEMON_LIST -> {
                            val sq by pokemonViewModel.pokemonSearchQuery.collectAsState()
                            val t1 by pokemonViewModel.pokemonSelectedType1.collectAsState()
                            val t2 by pokemonViewModel.pokemonSelectedType2.collectAsState()
                            PokemonSearchMenu(
                                searchQuery = sq,
                                selectedType1 = t1,
                                selectedType2 = t2,
                                availableTypes = ALL_POKEMON_TYPES,
                                onSearchQueryChanged = { pokemonViewModel.pokemonSearchQuery.value = it },
                                onType1Changed = { pokemonViewModel.pokemonSelectedType1.value = it },
                                onType2Changed = { pokemonViewModel.pokemonSelectedType2.value = it }
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
                    .background(if (selected) background_app.copy(alpha = 0.5f) else Color.Transparent)
                    .clickable { onNavigate(item.route) }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = item.iconResId),
                    contentDescription = item.label,
                    modifier = Modifier.size(22.dp),
                    tint = if (selected) CardBorder else CardBorder.copy(alpha = 0.5f)
                )
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) CardBorder else CardBorder.copy(alpha = 0.5f),
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) background_app.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pokemonViewModel.selectedDetailSection.value = section.name
                                onSectionSelected()
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = section.iconRes),
                            contentDescription = section.label,
                            modifier = Modifier.size(24.dp),
                            tint = contentColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = section.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
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
