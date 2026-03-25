package com.david.pokedex_api

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
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
import com.david.pokedex_api.ui.screen.ficha.PokemonDetailScreen
import com.david.pokedex_api.ui.screen.lista.GenerationPagerScreen
import com.david.pokedex_api.ui.screen.items.ItemBrowserScreen
import com.david.pokedex_api.ui.screen.movimientos.MoveBrowserScreen
import com.david.pokedex_api.ui.screen.regiones.RegionBrowserScreen
import com.david.pokedex_api.ui.theme.CardBorder
import com.david.pokedex_api.ui.theme.background_app
import com.david.pokedex_api.ui.theme.color_menu_busqueda2


object Routes {
    const val POKEMON_LIST = "pokemon_list"
    const val MOVE_BROWSER = "move_browser"
    const val ITEM_BROWSER = "item_browser"
    const val REGION_BROWSER = "region_browser"
    const val POKEMON_DETAILS = "pokemon_details/{pokemonName}"

    fun pokemonDetails(pokemonName: String) = "pokemon_details/$pokemonName"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val iconResId: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.POKEMON_LIST, "Pokémon", R.drawable.normal2),
    BottomNavItem(Routes.MOVE_BROWSER, "Movimientos", R.drawable.lucha2),
    BottomNavItem(Routes.ITEM_BROWSER, "Items", R.drawable.pokeball_icon),
    BottomNavItem(Routes.REGION_BROWSER, "Regiones", R.drawable.ic_location)
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

    val showBottomBar = currentRoute in listOf(Routes.POKEMON_LIST, Routes.MOVE_BROWSER, Routes.ITEM_BROWSER, Routes.REGION_BROWSER)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = color_menu_busqueda2,
                    contentColor = CardBorder,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = item.iconResId),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CardBorder,
                                unselectedIconColor = CardBorder.copy(alpha = 0.5f),
                                selectedTextColor = CardBorder,
                                unselectedTextColor = CardBorder.copy(alpha = 0.5f),
                                indicatorColor = background_app.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
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
                        navController.navigate(Routes.pokemonDetails(pokemonName))
                    }
                )
            }
            composable(Routes.MOVE_BROWSER) {
                MoveBrowserScreen(
                    pokemonViewModel = pokemonViewModel
                )
            }
            composable(Routes.ITEM_BROWSER) {
                ItemBrowserScreen(
                    pokemonViewModel = pokemonViewModel
                )
            }
            composable(Routes.REGION_BROWSER) {
                RegionBrowserScreen(
                    pokemonViewModel = pokemonViewModel
                )
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
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}


fun NamedApiResource.getGenerationIdFromUrl(): Int? {
    return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
}
