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
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.unit.sp
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
    val currentTab by pokemonViewModel.itemCurrentTab.collectAsState()
    val tabs = listOf("Items", "Bayas")

    Column(
        modifier = Modifier.fillMaxSize().background(background_app_gradient)
    ) {
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = color_menu_busqueda2,
            contentColor = CardBorder
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = currentTab == index,
                    onClick = { pokemonViewModel.itemCurrentTab.value = index },
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
    val selectedCategory by pokemonViewModel.itemSelectedCategory.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchItemList() }

    val allItems = remember(itemSummaries) { itemSummaries.values.sortedBy { it.id } }
    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        allItems.filter { item ->
            (searchQuery.isBlank() || item.localizedName.contains(searchQuery, true) || item.name.contains(searchQuery, true)) &&
            (selectedCategory == "Todas" || item.category?.equals(selectedCategory, true) == true)
        }
    }

    Box(Modifier.fillMaxSize().background(background_app_gradient)) {
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
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 100.dp),
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
    val selectedType by pokemonViewModel.berrySelectedType.collectAsState()

    LaunchedEffect(Unit) { pokemonViewModel.fetchBerryList() }

    val allBerries = remember(berrySummaries) { berrySummaries.values.sortedBy { it.id } }
    val filteredBerries = remember(allBerries, searchQuery, selectedType) {
        allBerries.filter { berry ->
            (searchQuery.isBlank() || berry.localizedName.contains(searchQuery, true) || berry.name.contains(searchQuery, true)) &&
            (selectedType == "Sin tipo" || berry.naturalGiftType?.equals(selectedType, true) == true)
        }
    }

    Box(Modifier.fillMaxSize().background(background_app_gradient)) {
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
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 100.dp),
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
            .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color_menu_busqueda2, color_menu_busqueda1)
            ))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                if (query.isNotBlank()) {
                    FilledTonalButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onQueryChanged("") },
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
                value = query, onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Filled.Search, "Buscar", tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Filled.Clear, "Limpiar", tint = Color.White.copy(alpha = 0.8f)) } },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f), unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.White.copy(alpha = 0.4f), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f), unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSearchMenu(pokemonViewModel: PokemonViewModel) {
    val haptic = LocalHapticFeedback.current
    val currentTab by pokemonViewModel.itemCurrentTab.collectAsState()
    val itemQuery by pokemonViewModel.itemSearchQuery.collectAsState()
    val berryQuery by pokemonViewModel.berrySearchQuery.collectAsState()
    val selectedCategory by pokemonViewModel.itemSelectedCategory.collectAsState()
    val selectedType by pokemonViewModel.berrySelectedType.collectAsState()
    val itemSummaries by pokemonViewModel.itemSummaries.collectAsState()

    val query = if (currentTab == 0) itemQuery else berryQuery
    val onQueryChanged: (String) -> Unit = { q ->
        if (currentTab == 0) pokemonViewModel.itemSearchQuery.value = q
        else pokemonViewModel.berrySearchQuery.value = q
    }

    val categories = remember(itemSummaries) {
        listOf("Todas") + itemSummaries.values.mapNotNull { it.category }.distinct().sorted()
    }

    val hasFilters = query.isNotBlank() || (currentTab == 0 && selectedCategory != "Todas") || (currentTab == 1 && selectedType != "Sin tipo")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(color_menu_busqueda2, color_menu_busqueda1)
            ))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    if (currentTab == 0) "Filtrar Items" else "Filtrar Bayas",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White
                )
                if (hasFilters) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onQueryChanged("")
                            if (currentTab == 0) pokemonViewModel.itemSelectedCategory.value = "Todas"
                            else pokemonViewModel.berrySelectedType.value = "Sin tipo"
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
                value = query, onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (currentTab == 0) "Buscar item..." else "Buscar baya...") },
                leadingIcon = { Icon(Icons.Filled.Search, "Buscar", tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChanged("") }) { Icon(Icons.Filled.Clear, "Limpiar", tint = Color.White.copy(alpha = 0.8f)) } },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f), unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                    focusedBorderColor = Color.White.copy(alpha = 0.4f), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.5f), unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
                )
            )

            if (currentTab == 0) {
                // Filtro de categoría para items
                ItemCategoryFilter(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChanged = { pokemonViewModel.itemSelectedCategory.value = it }
                )
            } else {
                // Filtro de tipo para bayas
                BerryTypeFilter(
                    selectedType = selectedType,
                    onTypeChanged = { pokemonViewModel.berrySelectedType.value = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemCategoryFilter(
    categories: List<String>,
    selectedCategory: String,
    onCategoryChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedCategory == "Todas") "Categoría" else selectedCategory,
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
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat, fontWeight = if (cat == selectedCategory) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onCategoryChanged(cat); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BerryTypeFilter(
    selectedType: String,
    onTypeChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val hasType = selectedType != "Sin tipo"
    val typeColor = if (hasType) getPokemonTypeColor(selectedType) else Color.Transparent
    val bgColor = if (hasType) typeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    val borderColor = if (hasType) typeColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (!hasType) "Tipo" else pokemonTypeNameTranslator(selectedType).replaceFirstChar { it.titlecase() },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                focusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Todos los tipos") },
                onClick = { onTypeChanged("Sin tipo"); expanded = false }
            )
            ALL_POKEMON_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { PokemonTypeChip(typeName = type, modifier = Modifier.fillMaxWidth()) },
                    onClick = { onTypeChanged(type); expanded = false },
                    modifier = Modifier.background(getPokemonTypeColor(type))
                )
            }
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
