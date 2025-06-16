package com.david.pokedex_api

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.NamedApiResource
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.GenerationPagerScreen
import com.david.pokedex_api.ui.screen.PokemonDetailScreen


object Routes {
    const val POKEMON_LIST = "pokemon_list"
    const val POKEMON_DETAILS = "pokemon_details/{pokemonName}"

    fun pokemonDetails(pokemonName: String) = "pokemon_details/$pokemonName"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PokedexApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp(
    pokemonViewModel: PokemonViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {

    NavHost(navController = navController, startDestination = Routes.POKEMON_LIST) {
        composable(Routes.POKEMON_LIST) {
            GenerationPagerScreen(
                pokemonViewModel = pokemonViewModel,
                onNavigateToDetails = { pokemonName ->
                    navController.navigate(Routes.pokemonDetails(pokemonName))
                }
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
                // Manejar caso de nombre nulo, quizás volver o mostrar error
                Text("Error: Pokémon name not found.", modifier = Modifier.padding(16.dp))
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}




fun getReadableGenerationName(apiName: String): String {
    // apiName es como "generation-i", "generation-ii", etc.
    // Transforma "generation-i" a "Generation I"
    if (apiName.startsWith("generation-")) {
        val parts = apiName.split("-")
        if (parts.size == 2) {
            val numberRoman = parts[1].uppercase() // Convierte "i" a "I", "ii" a "II"
            return "Generation $numberRoman"
        }
    }
    // Fallback si el formato no es el esperado
    return apiName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        .replace("-", " ")
}
fun NamedApiResource.getGenerationIdFromUrl(): Int? {
    return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
}