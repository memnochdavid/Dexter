package com.david.pokedex_api.ui.screen.movimientos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.MoveSummary
import com.david.pokedex_api.api.viewModel.PokemonViewModel
import com.david.pokedex_api.ui.screen.comun.*
import com.david.pokedex_api.ui.screen.ficha.composable.desplegable.MoveDetailItem
import com.david.pokedex_api.ui.theme.*
import com.david.pokedex_api.util.Lottie

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveBrowserScreen(
    pokemonViewModel: PokemonViewModel
) {
    val moveSummaries by pokemonViewModel.moveSummaries.collectAsState()
    val isLoadingList by pokemonViewModel.isLoadingMoveList.collectAsState()
    val isLoadingSummaries by pokemonViewModel.isLoadingMoveSummaries.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(NO_TYPE_SELECTED) }
    var selectedDamageClass by rememberSaveable { mutableStateOf("Todos") }
    var showBottomSheet by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        pokemonViewModel.fetchMoveList()
    }

    val allMoves = remember(moveSummaries) {
        moveSummaries.values.sortedBy { it.id }
    }

    val isSearching = searchQuery.isNotBlank() || selectedType != NO_TYPE_SELECTED || selectedDamageClass != "Todos"

    val filteredMoves = remember(allMoves, searchQuery, selectedType, selectedDamageClass) {
        if (!isSearching) allMoves
        else allMoves.filter { move ->
            (searchQuery.isBlank() || move.localizedName.contains(searchQuery, true) || move.name.contains(searchQuery, true)) &&
            (selectedType == NO_TYPE_SELECTED || move.typeName?.equals(selectedType, true) == true) &&
            (selectedDamageClass == "Todos" || move.damageClass?.equals(selectedDamageClass, true) == true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showBottomSheet = true },
                containerColor = color_boton_busqueda.copy(alpha = 0.85f),
                modifier = Modifier.size(65.dp).clip(RoundedCornerShape(50.dp))
            ) {
                Lottie(rawResId = R.raw.search, modifier = Modifier.fillMaxSize())
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background_app)
                .padding(padding)
        ) {
            if (isLoadingList && allMoves.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(96.dp))
                }
            } else if (filteredMoves.isEmpty() && !isLoadingSummaries) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Sin resultados", color = CardBorder, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = filteredMoves, key = { it.id }) { move ->
                        MoveBrowserCard(move = move)
                    }
                    if (isLoadingSummaries) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(32.dp))
                                    Text("Cargando movimientos...", color = CardBorder, style = MaterialTheme.typography.bodyMedium)
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
                containerColor = color_menu_busqueda2,
                dragHandle = null
            ) {
                MoveSearchMenu(
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { searchQuery = it },
                    selectedType = selectedType,
                    onTypeChanged = { selectedType = it },
                    selectedDamageClass = selectedDamageClass,
                    onDamageClassChanged = { selectedDamageClass = it },
                    loadedCount = allMoves.size,
                    isLoading = isLoadingSummaries
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveSearchMenu(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedType: String,
    onTypeChanged: (String) -> Unit,
    selectedDamageClass: String,
    onDamageClassChanged: (String) -> Unit,
    loadedCount: Int,
    isLoading: Boolean
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color_menu_busqueda1,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filtrar Movimientos" + if (isLoading) " ($loadedCount...)" else " ($loadedCount)",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(.8f)
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSearchQueryChanged("")
                        onTypeChanged(NO_TYPE_SELECTED)
                        onDamageClassChanged("Todos")
                    },
                    modifier = Modifier.size(35.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color_fuego_card,
                        contentColor = blanco80
                    ),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar filtros",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar por nombre") },
                placeholder = { Text("Ej: Lanzallamas") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.primary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedContainerColor = Color.White.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                    focusedBorderColor = Color.White.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLeadingIconColor = CardBorder,
                    unfocusedLeadingIconColor = CardBorder,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoveTypeFilter(
                    selectedType = selectedType,
                    onTypeChanged = onTypeChanged,
                    modifier = Modifier.weight(1f)
                )
                DamageClassFilter(
                    selectedClass = selectedDamageClass,
                    onClassChanged = onDamageClassChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveTypeFilter(
    selectedType: String,
    onTypeChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val bgColor = if (selectedType != NO_TYPE_SELECTED) {
        getPokemonTypeColor(selectedType).copy(alpha = 0.7f)
    } else {
        Color.White.copy(alpha = 0.5f)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (selectedType == NO_TYPE_SELECTED) "Tipo" else pokemonTypeNameTranslator(selectedType).replaceFirstChar { it.titlecase() },
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(8.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor,
                focusedBorderColor = if (selectedType != NO_TYPE_SELECTED) getPokemonTypeColor(selectedType) else Color.White.copy(0.7f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        ) {
            DropdownMenuItem(
                text = { Text("Todos los tipos") },
                onClick = { onTypeChanged(NO_TYPE_SELECTED); expanded = false }
            )
            ALL_POKEMON_TYPES.forEach { type ->
                DropdownMenuItem(
                    text = {
                        PokemonTypeChip(typeName = type, modifier = Modifier.fillMaxWidth())
                    },
                    onClick = { onTypeChanged(type); expanded = false },
                    modifier = Modifier.background(getPokemonTypeColor(type))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DamageClassFilter(
    selectedClass: String,
    onClassChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val classes = listOf("Todos", "physical", "special", "status")

    val displayName = { cls: String ->
        when (cls.lowercase()) {
            "physical" -> "Fisico"
            "special" -> "Especial"
            "status" -> "Estado"
            else -> "Clase"
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayName(selectedClass),
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(8.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = Color.White.copy(alpha = 0.5f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                focusedBorderColor = Color.White.copy(alpha = 0.7f),
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            classes.forEach { cls ->
                val iconResId: Int? = when (cls.lowercase()) {
                    "physical" -> R.drawable.fisico
                    "special" -> R.drawable.especial
                    "status" -> R.drawable.estado
                    else -> null
                }
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (iconResId != null) {
                                Image(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = cls,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                displayName(cls),
                                fontWeight = if (cls == selectedClass) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = { onClassChanged(cls); expanded = false }
                )
            }
        }
    }
}

@Composable
fun MoveBrowserCard(move: MoveSummary) {
    var isExpanded by rememberSaveable(key = "move_browser_${move.id}") { mutableStateOf(false) }

    val bgColor = move.typeName?.let { getPokemonTypeColorClear(it) }
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val textColor = move.typeName?.let { if (esTipoColorOscuro(it)) Color.White else Color.Black }
        ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = move.localizedName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val iconResId: Int? = when (move.damageClass?.lowercase()) {
                "physical" -> R.drawable.fisico
                "special" -> R.drawable.especial
                "status" -> R.drawable.estado
                else -> null
            }
            if (iconResId != null) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = move.damageClass,
                    modifier = Modifier.size(22.dp).padding(start = 4.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "#${move.id}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MoveDetailItem(label = "Pot.", value = move.power?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = textColor)
            MoveDetailItem(label = "PP", value = move.pp?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = textColor)
            MoveDetailItem(label = "Prec.", value = move.accuracy?.let { "$it%" } ?: "-", modifier = Modifier.weight(1f), colorTexto = textColor)

            Box(
                modifier = Modifier.weight(1.5f),
                contentAlignment = Alignment.Center
            ) {
                if (move.typeName != null) {
                    PokemonTypeChip(typeName = move.typeName, modifier = Modifier.height(28.dp))
                } else {
                    Text("-", style = MaterialTheme.typography.bodySmall, color = textColor, textAlign = TextAlign.Center)
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            if (!move.description.isNullOrBlank()) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = move.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}
