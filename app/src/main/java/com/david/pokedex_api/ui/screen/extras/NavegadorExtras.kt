package com.david.pokedex_api.ui.screen.extras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.DisplayableContestType
import com.david.pokedex_api.api.model.DisplayableNature
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.Lottie

@Composable
fun ExtrasBrowserScreen(pokemonViewModel: PokemonViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Naturalezas", "Concursos")

    Column(Modifier.fillMaxSize().background(background_app_gradient)) {
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = color_menu_busqueda2,
            contentColor = CardBorder
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = { currentTab = index },
                    text = { Text(title, fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (currentTab) {
            0 -> NaturesTab(pokemonViewModel)
            1 -> ContestsTab(pokemonViewModel)
        }
    }
}

@Composable
private fun NaturesTab(pokemonViewModel: PokemonViewModel) {
    val natures by pokemonViewModel.natures.collectAsState()
    val isLoading by pokemonViewModel.isLoadingNatures.collectAsState()
    val searchQuery by pokemonViewModel.extrasSearchQuery.collectAsState()
    val statFilter by pokemonViewModel.natureStatFilter.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchNatures() }

    val filteredNatures = remember(natures, searchQuery, statFilter) {
        natures.filter { nature ->
            (searchQuery.isBlank() || nature.localizedName.contains(searchQuery, true) || nature.name.contains(searchQuery, true)) &&
            (statFilter == "Todos" ||
                nature.increasedStat?.equals(statFilter, true) == true ||
                nature.decreasedStat?.equals(statFilter, true) == true)
        }
    }

    if (isLoading && natures.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
        }
    } else if (filteredNatures.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sin resultados", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color_menu_busqueda2)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Naturaleza", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = CardBorder, modifier = Modifier.weight(1f))
                    Text("Sube", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color_stat_attack, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Baja", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color_stat_hp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }

            items(items = filteredNatures, key = { it.id }) { nature ->
                NatureRow(nature)
            }
        }
    }
}

@Composable
private fun NatureRow(nature: DisplayableNature) {
    val isNeutral = nature.increasedStat == null && nature.decreasedStat == null
    val bgColor = if (isNeutral) CardBorder.copy(alpha = 0.05f) else color_planta_card_foto.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            nature.localizedName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = CardBorder,
            modifier = Modifier.weight(1f)
        )
        Text(
            nature.increasedStat ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (nature.increasedStat != null) FontWeight.Bold else FontWeight.Normal,
            color = if (nature.increasedStat != null) color_stat_attack else CardBorder.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            nature.decreasedStat ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (nature.decreasedStat != null) FontWeight.Bold else FontWeight.Normal,
            color = if (nature.decreasedStat != null) color_stat_hp else CardBorder.copy(alpha = 0.4f),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ContestsTab(pokemonViewModel: PokemonViewModel) {
    val contestTypes by pokemonViewModel.contestTypes.collectAsState()
    val isLoading by pokemonViewModel.isLoadingContests.collectAsState()
    val searchQuery by pokemonViewModel.extrasSearchQuery.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchContestTypes() }

    val filteredContests = remember(contestTypes, searchQuery) {
        if (searchQuery.isBlank()) contestTypes
        else contestTypes.filter { it.localizedName.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
    }

    if (isLoading && contestTypes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
        }
    } else if (filteredContests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (searchQuery.isNotBlank()) "Sin resultados" else "No hay datos de concursos", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = filteredContests, key = { it.id }) { contest ->
                ContestTypeCard(contest)
            }
        }
    }
}

@Composable
private fun ContestTypeCard(contest: DisplayableContestType) {
    val bgColor = when (contest.name.lowercase()) {
        "cool" -> color_fuego_card_foto
        "beauty" -> color_agua_card_foto
        "cute" -> color_hada_card_foto
        "smart" -> color_planta_card_foto
        "tough" -> color_roca_card_foto
        else -> color_normal_card_foto
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                contest.localizedName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CardBorder
            )
            if (contest.color.isNotBlank()) {
                Text(
                    contest.color,
                    style = MaterialTheme.typography.bodySmall,
                    color = CardBorder.copy(alpha = 0.6f)
                )
            }
        }
        if (contest.berryFlavor != null) {
            Text(
                "Sabor de baya: ${contest.berryFlavor}",
                style = MaterialTheme.typography.bodyMedium,
                color = CardBorder.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtrasSearchMenu(pokemonViewModel: PokemonViewModel) {
    val haptic = LocalHapticFeedback.current
    val searchQuery by pokemonViewModel.extrasSearchQuery.collectAsState()
    val statFilter by pokemonViewModel.natureStatFilter.collectAsState()

    val stats = listOf("Todos", "attack", "defense", "special-attack", "special-defense", "speed")
    val statDisplayName = { stat: String ->
        when (stat.lowercase()) {
            "attack" -> "Ataque"
            "defense" -> "Defensa"
            "special-attack" -> "At. Especial"
            "special-defense" -> "Def. Especial"
            "speed" -> "Velocidad"
            else -> "Stat"
        }
    }

    val hasFilters = searchQuery.isNotBlank() || statFilter != "Todos"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color_menu_busqueda2, color_menu_busqueda1)
            ))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Filtrar Extras", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                if (hasFilters) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pokemonViewModel.extrasSearchQuery.value = ""
                            pokemonViewModel.natureStatFilter.value = "Todos"
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f), contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Clear, "Limpiar", Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Limpiar", fontSize = 12.sp)
                    }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { pokemonViewModel.extrasSearchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre...") },
                leadingIcon = { Icon(Icons.Filled.Search, "Buscar", tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { pokemonViewModel.extrasSearchQuery.value = "" }) { Icon(Icons.Filled.Clear, "Limpiar", tint = Color.White.copy(alpha = 0.8f)) } },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f), unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.White.copy(alpha = 0.4f), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f), unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                )
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = if (statFilter == "Todos") "Stat" else statDisplayName(statFilter),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color.White.copy(alpha = 0.2f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                        unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    stats.forEach { stat ->
                        DropdownMenuItem(
                            text = { Text(if (stat == "Todos") "Todos los stats" else statDisplayName(stat), fontWeight = if (stat == statFilter) FontWeight.Bold else FontWeight.Normal) },
                            onClick = { pokemonViewModel.natureStatFilter.value = stat; expanded = false }
                        )
                    }
                }
            }
        }
    }
}
