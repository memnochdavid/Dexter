package com.david.pokedex_api.ui.screen.ficha.composable.background

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Selector de overlay animado según el tipo del Pokémon.
 * Se renderiza encima del degradado existente.
 * Los tipos sin implementar no muestran nada (solo el degradado base).
 */
@Composable
fun TypeAnimatedOverlay(
    typeName: String,
    modifier: Modifier = Modifier
) {
    when (typeName.lowercase()) {
        "fire" -> FireOverlay(modifier)
        "water" -> WaterOverlay(modifier)
        "electric" -> ElectricOverlay(modifier)
        "ice" -> IceOverlay(modifier)
        "grass" -> GrassOverlay(modifier)
        "ghost" -> GhostOverlay(modifier)
        "psychic" -> PsychicOverlay(modifier)
        "dragon" -> DragonOverlay(modifier)
        "dark" -> DarkOverlay(modifier)
        "fairy" -> FairyOverlay(modifier)
        "poison" -> PoisonOverlay(modifier)
        "fighting" -> FightingOverlay(modifier)
        "ground" -> GroundOverlay(modifier)
        "rock" -> RockOverlay(modifier)
        "steel" -> SteelOverlay(modifier)
        "flying" -> FlyingOverlay(modifier)
        "bug" -> BugOverlay(modifier)
        // Aquí se irán añadiendo el resto de tipos:
        // "electric" -> ElectricOverlay(modifier)
        // ...
    }
}
