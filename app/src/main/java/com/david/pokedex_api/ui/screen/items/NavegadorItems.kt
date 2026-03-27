package com.david.pokedex_api.ui.screen.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.BerrySummary
import com.david.pokedex_api.api.model.ItemSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.comun.*
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.Lottie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemBrowserScreen(pokemonViewModel: PokemonViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Items", "Bayas")

    Column(
        modifier = Modifier.fillMaxSize().background(background_app)
    ) {
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
            0 -> ItemsTab(pokemonViewModel)
            1 -> BerriesTab(pokemonViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemsTab(pokemonViewModel: PokemonViewModel) {
    val itemSummaries by pokemonViewModel.itemSummaries.collectAsState()
    val isLoading by pokemonViewModel.isLoadingItems.collectAsState()

    val searchQuery by pokemonViewModel.itemSearchQuery.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchItemList() }

    val allItems = remember(itemSummaries) { itemSummaries.values.sortedBy { it.id } }
    val filteredItems = remember(allItems, searchQuery) {
        if (searchQuery.isBlank()) allItems
        else allItems.filter { it.localizedName.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
    }

    Box(Modifier.fillMaxSize().background(background_app)) {
        if (isLoading && allItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
            }
        } else if (filteredItems.isEmpty()) {
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Sin resultados", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = filteredItems, key = { it.id }) { item ->
                    ItemCard(item)
                }
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(32.dp))
                                Text("Cargando items...", color = CardBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BerriesTab(pokemonViewModel: PokemonViewModel) {
    val berrySummaries by pokemonViewModel.berrySummaries.collectAsState()
    val isLoading by pokemonViewModel.isLoadingBerries.collectAsState()

    val searchQuery by pokemonViewModel.berrySearchQuery.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchBerryList() }

    val allBerries = remember(berrySummaries) { berrySummaries.values.sortedBy { it.id } }
    val filteredBerries = remember(allBerries, searchQuery) {
        if (searchQuery.isBlank()) allBerries
        else allBerries.filter { it.localizedName.contains(searchQuery, true) || it.name.contains(searchQuery, true) }
    }

    Box(Modifier.fillMaxSize().background(background_app)) {
        if (isLoading && allBerries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
            }
        } else if (filteredBerries.isEmpty()) {
            Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text("Sin resultados", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = filteredBerries, key = { it.id }) { berry ->
                    BerryCard(berry)
                }
                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(32.dp))
                                Text("Cargando bayas...", color = CardBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMenu(title: String, query: String, onQueryChanged: (String) -> Unit, placeholder: String) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color_menu_busqueda1, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(.8f))
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onQueryChanged("") },
                    modifier = Modifier.size(35.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color_fuego_card, contentColor = blanco80),
                    contentPadding = PaddingValues(4.dp)
                ) { Icon(Icons.Default.Delete, "Limpiar", Modifier.fillMaxSize()) }
            }
            OutlinedTextField(
                value = query, onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre") }, placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Filled.Search, "Buscar") },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Filled.Clear, "Limpiar") } },
                singleLine = true, shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.7f), unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                    focusedBorderColor = Color.White.copy(alpha = 0.7f), unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLeadingIconColor = CardBorder, unfocusedLeadingIconColor = CardBorder
                )
            )
        }
    }
}

@Composable
private fun ItemCard(item: ItemSummary) {
    var isExpanded by rememberSaveable(key = "item_${item.id}") { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color_agua_card_foto.copy(alpha = 0.3f))
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.spriteUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.spriteUrl).crossfade(true).size(64).build(),
                contentDescription = item.localizedName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(item.localizedName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CardBorder, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (item.cost != null && item.cost > 0) {
                    Text("${item.cost}₽", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
                }
            }
            if (item.category != null) {
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.6f))
            }

            AnimatedVisibility(visible = isExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                if (!item.effect.isNullOrBlank()) {
                    Text(item.effect, style = MaterialTheme.typography.bodyMedium, color = CardBorder.copy(alpha = 0.85f), modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun BerryCard(berry: BerrySummary) {
    var isExpanded by rememberSaveable(key = "berry_${berry.id}") { mutableStateOf(false) }
    val context = LocalContext.current
    val typeColor = berry.naturalGiftType?.let { getPokemonTypeColorClear(it) } ?: color_planta_card_foto

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(typeColor.copy(alpha = 0.4f))
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (berry.spriteUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(berry.spriteUrl).crossfade(true).size(64).build(),
                contentDescription = berry.localizedName,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(10.dp))
        }

        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(berry.localizedName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CardBorder, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (berry.naturalGiftType != null) {
                    PokemonTypeChipSmall(typeName = berry.naturalGiftType, colorClaro = true)
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                Text("Pot: ${berry.naturalGiftPower}", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
                Text("Crec: ${berry.growthTime}h", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
                Text("Cosecha: ${berry.maxHarvest}", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
            }

            AnimatedVisibility(visible = isExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tamaño: ${berry.size}mm | Suavidad: ${berry.smoothness}", style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.7f))
                    if (berry.flavors.isNotEmpty()) {
                        Text("Sabores:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CardBorder)
                        berry.flavors.filter { it.value > 0 }.forEach { (flavor, potency) ->
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(flavor, style = MaterialTheme.typography.bodySmall, color = CardBorder.copy(alpha = 0.8f))
                                Text("$potency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = CardBorder)
                            }
                        }
                    }
                }
            }
        }
    }
}
