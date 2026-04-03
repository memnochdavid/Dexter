package com.david.pokedex_api.ui.screen.lista.composable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getTextColorForTypeBackground
import com.david.pokedex_api.ui.screen.comun.pokemonTypeNameTranslator
import com.david.pokedex_api.ui.theme.*


@Composable
fun PokemonSearchMenu(
    searchQuery: String,
    selectedType1: String,
    selectedType2: String,
    availableTypes: List<String>,
    showMegas: Boolean = false,
    showGigamax: Boolean = false,
    showRegionals: Boolean = false,
    showLegendaries: Boolean = false,
    showMythicals: Boolean = false,
    onSearchQueryChanged: (String) -> Unit,
    onType1Changed: (String) -> Unit,
    onType2Changed: (String) -> Unit,
    onShowMegasChanged: (Boolean) -> Unit = {},
    onShowGigamaxChanged: (Boolean) -> Unit = {},
    onShowRegionalsChanged: (Boolean) -> Unit = {},
    onShowLegendariesChanged: (Boolean) -> Unit = {},
    onShowMythicalsChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasActiveFilters = searchQuery.isNotBlank() || selectedType1 != NO_TYPE_SELECTED ||
            selectedType2 != NO_TYPE_SELECTED || showMegas || showGigamax || showRegionals ||
            showLegendaries || showMythicals

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(color_menu_busqueda2, color_menu_busqueda1)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header: titulo + boton limpiar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Filtrar Pokémon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (hasActiveFilters) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSearchQueryChanged("")
                            onType1Changed(NO_TYPE_SELECTED)
                            onType2Changed(NO_TYPE_SELECTED)
                            onShowMegasChanged(false)
                            onShowGigamaxChanged(false)
                            onShowRegionalsChanged(false)
                            onShowLegendariesChanged(false)
                            onShowMythicalsChanged(false)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Limpiar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Limpiar", fontSize = 12.sp)
                    }
                }
            }

            // ── Barra de búsqueda ──
            NameSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Selectores de tipo ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypeSelector(
                    label = "Tipo 1",
                    selectedType = selectedType1,
                    availableTypes = availableTypes,
                    onTypeSelected = onType1Changed,
                    modifier = Modifier.weight(1f)
                )
                TypeSelector(
                    label = "Tipo 2",
                    selectedType = selectedType2,
                    availableTypes = availableTypes,
                    onTypeSelected = onType2Changed,
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Chips Mega / Gigamax / Regionales ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipToggle(
                    label = "Mega",
                    checked = showMegas,
                    onCheckedChange = onShowMegasChanged,
                    activeColor = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
                FilterChipToggle(
                    label = "Gigamax",
                    checked = showGigamax,
                    onCheckedChange = onShowGigamaxChanged,
                    activeColor = Color(0xFFFF9F43),
                    modifier = Modifier.weight(1f)
                )
                FilterChipToggle(
                    label = "Regionales",
                    checked = showRegionals,
                    onCheckedChange = onShowRegionalsChanged,
                    activeColor = Color(0xFF4ECDC4),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Chips Legendarios / Singulares ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChipToggle(
                    label = "Legendarios",
                    checked = showLegendaries,
                    onCheckedChange = onShowLegendariesChanged,
                    activeColor = Color(0xFFB8860B),
                    modifier = Modifier.weight(1f)
                )
                FilterChipToggle(
                    label = "Singulares",
                    checked = showMythicals,
                    onCheckedChange = onShowMythicalsChanged,
                    activeColor = Color(0xFF9B59B6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FilterChipToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val bgColor = if (checked) activeColor else Color.White.copy(alpha = 0.1f)
    val borderColor = if (checked) activeColor else Color.White.copy(alpha = 0.3f)
    val textColor = if (checked) Color.White else Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun NameSearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        modifier = modifier,
        placeholder = { Text("Buscar por nombre...") },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Buscar",
                tint = Color.White.copy(alpha = 0.7f))
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar",
                        tint = Color.White.copy(alpha = 0.8f))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.9f),
            cursorColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedBorderColor = Color.White.copy(alpha = 0.4f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.4f),
            focusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.5f),
            focusedTrailingIconColor = Color.White.copy(alpha = 0.8f),
            unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(
    label: String,
    selectedType: String,
    availableTypes: List<String>,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    val hasType = selectedType != NO_TYPE_SELECTED
    val typeColor = if (hasType) getPokemonTypeColor(selectedType) else Color.Transparent
    val bgColor = if (hasType) typeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
    val borderColor = if (hasType) typeColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = pokemonTypeNameTranslator(selectedType).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = bgColor,
                unfocusedContainerColor = bgColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = Color.White.copy(alpha = 0.7f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                focusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.5f),
            ),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
        ) {
            // Opción para "Sin tipo"
            val noTypeItemColor =
                getPokemonTypeColor(NO_TYPE_SELECTED) // Usa tu color por defecto para "Sin tipo"
            val noTypeItemTextColor = getTextColorForTypeBackground(noTypeItemColor)
            DropdownMenuItem(
                text = {
                    Text(
                        NO_TYPE_SELECTED.replaceFirstChar { it.titlecase() },
                        color = noTypeItemTextColor,
                        fontWeight = if (selectedType == NO_TYPE_SELECTED) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onTypeSelected(NO_TYPE_SELECTED)
                    expanded = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(noTypeItemColor) // Fondo con el color de "Sin tipo"
            )

            // Resto de los tipos disponibles
            // EL ERROR ESTABA AQUÍ: HABÍA UN BUCLE ANIDADO INNECESARIAMENTE
            availableTypes.filter { it != NO_TYPE_SELECTED }.forEach { typeName ->
                // Este es el único bucle necesario para los tipos
                val itemBackgroundColor = getPokemonTypeColor(typeName) // Color de fondo del tipo
                val itemTextColor =
                    getTextColorForTypeBackground(itemBackgroundColor) // Color de texto para contraste
                val isCurrentlySelected = typeName == selectedType

                DropdownMenuItem(
                    text = {
//                        Text(
//                            text = typeName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
//                            color = itemTextColor,
//                            fontWeight = if (isCurrentlySelected) FontWeight.Bold else FontWeight.Normal
//                        )
                        PokemonTypeChip(
                            typeName,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    onClick = {
                        onTypeSelected(typeName)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(itemBackgroundColor) // Aplicar el color del tipo como fondo
                    // Opcional: podrías añadir un borde o un overlay si está seleccionado,
                    // además del fontWeight.Bold, para mayor claridad.
                )
            } // Fin del único bucle forEach
        }
    }
}