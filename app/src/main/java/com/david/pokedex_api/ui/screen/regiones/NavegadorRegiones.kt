package com.david.pokedex_api.ui.screen.regiones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.DisplayableAreaPokemon
import com.david.pokedex_api.api.model.DisplayableLocation
import com.david.pokedex_api.api.model.DisplayableLocationArea
import com.david.pokedex_api.api.model.DisplayableRegion
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.Lottie

private enum class RegionNav { REGIONS, LOCATIONS, AREAS }

@Composable
fun RegionBrowserScreen(pokemonViewModel: PokemonViewModel) {
    val regions by pokemonViewModel.regions.collectAsState()
    val isLoadingRegions by pokemonViewModel.isLoadingRegions.collectAsState()
    val locations by pokemonViewModel.regionLocations.collectAsState()
    val isLoadingLocations by pokemonViewModel.isLoadingLocations.collectAsState()
    val areas by pokemonViewModel.locationAreas.collectAsState()
    val isLoadingAreas by pokemonViewModel.isLoadingAreas.collectAsState()

    var currentNav by rememberSaveable { mutableStateOf(RegionNav.REGIONS.name) }
    var selectedRegionName by rememberSaveable { mutableStateOf("") }
    var selectedLocationName by rememberSaveable { mutableStateOf("") }
    var selectedLocationDisplay by rememberSaveable { mutableStateOf("") }
    var selectedRegionDisplay by rememberSaveable { mutableStateOf("") }

    val searchQuery by pokemonViewModel.regionSearchQuery.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchRegions() }

    val filteredRegions = remember(regions, searchQuery) {
        if (searchQuery.isBlank()) regions
        else regions.filter { it.localizedName.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
    }
    val filteredLocations = remember(locations, searchQuery) {
        if (searchQuery.isBlank()) locations
        else locations.filter { it.localizedName.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
    }
    val filteredAreas = remember(areas, searchQuery) {
        if (searchQuery.isBlank()) areas
        else areas.filter { area ->
            area.localizedName.contains(searchQuery, true) || area.name.contains(searchQuery, true) ||
            area.pokemonEncounters.any { it.pokemonName.contains(searchQuery, true) }
        }
    }

    Column(Modifier.fillMaxSize().background(background_app)) {
        // Top bar con navegacion
        val title = when (RegionNav.valueOf(currentNav)) {
            RegionNav.REGIONS -> "Regiones"
            RegionNav.LOCATIONS -> selectedRegionDisplay
            RegionNav.AREAS -> selectedLocationDisplay
        }
        val showBack = currentNav != RegionNav.REGIONS.name

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color_menu_busqueda2)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = {
                    currentNav = when (RegionNav.valueOf(currentNav)) {
                        RegionNav.AREAS -> RegionNav.LOCATIONS.name
                        RegionNav.LOCATIONS -> RegionNav.REGIONS.name
                        else -> RegionNav.REGIONS.name
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = CardBorder)
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CardBorder,
                modifier = Modifier.weight(1f),
                textAlign = if (showBack) TextAlign.Start else TextAlign.Center
            )
        }

        when (RegionNav.valueOf(currentNav)) {
            RegionNav.REGIONS -> {
                if (isLoadingRegions && regions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
                    }
                } else if (filteredRegions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = filteredRegions, key = { it.id }) { region ->
                            RegionCard(region) {
                                selectedRegionName = region.name
                                selectedRegionDisplay = region.localizedName
                                pokemonViewModel.fetchLocationsForRegion(region.name)
                                currentNav = RegionNav.LOCATIONS.name
                            }
                        }
                    }
                }
            }

            RegionNav.LOCATIONS -> {
                if (isLoadingLocations && locations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(64.dp))
                    }
                } else if (filteredLocations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isNotBlank()) "Sin resultados" else "No hay ubicaciones", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = filteredLocations, key = { it.id }) { location ->
                            LocationCard(location) {
                                selectedLocationName = location.name
                                selectedLocationDisplay = location.localizedName
                                pokemonViewModel.fetchLocationAreas(location.name)
                                currentNav = RegionNav.AREAS.name
                            }
                        }
                    }
                }
            }

            RegionNav.AREAS -> {
                if (isLoadingAreas && areas.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(64.dp))
                    }
                } else if (filteredAreas.isEmpty() || filteredAreas.all { it.pokemonEncounters.isEmpty() }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isNotBlank()) "Sin resultados" else "Sin datos de encuentros", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredAreas.forEach { area ->
                            if (area.pokemonEncounters.isNotEmpty()) {
                                item(key = area.name) {
                                    AreaCard(area)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionCard(region: DisplayableRegion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color_dragon_card_foto.copy(alpha = 0.4f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(region.localizedName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CardBorder)
            if (region.generation != null) {
                Text(region.generation, style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.6f))
            }
        }
        Text("${region.locationCount} zonas", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
    }
}

@Composable
private fun LocationCard(location: DisplayableLocation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color_planta_card_foto.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(location.localizedName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = CardBorder, modifier = Modifier.weight(1f))
        if (location.areaCount > 0) {
            Text("${location.areaCount} areas", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun AreaCard(area: DisplayableLocationArea) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color_agua_card_foto.copy(alpha = 0.3f))
            .padding(10.dp)
    ) {
        Text(area.localizedName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CardBorder, modifier = Modifier.padding(bottom = 6.dp))

        area.pokemonEncounters.forEach { pokemon ->
            PokemonEncounterRow(pokemon)
        }
    }
}

@Composable
private fun PokemonEncounterRow(pokemon: DisplayableAreaPokemon) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pokemon.spriteUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(pokemon.spriteUrl).crossfade(true).size(48).build(),
                contentDescription = pokemon.pokemonName,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(pokemon.pokemonName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = CardBorder, modifier = Modifier.weight(1f))
        Text(pokemon.method, style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
        Spacer(Modifier.width(6.dp))
        val lvl = if (pokemon.minLevel == pokemon.maxLevel) "Nv.${pokemon.minLevel}" else "Nv.${pokemon.minLevel}-${pokemon.maxLevel}"
        Text(lvl, style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
        Spacer(Modifier.width(6.dp))
        Text("${pokemon.maxChance}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CardBorder.copy(alpha = 0.9f))
    }
}
