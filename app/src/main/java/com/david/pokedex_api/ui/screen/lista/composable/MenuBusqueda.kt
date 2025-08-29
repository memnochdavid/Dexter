package com.david.pokedex_api.ui.screen.lista.composable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.david.pokedex_api.ui.screen.comun.NO_TYPE_SELECTED
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColor
import com.david.pokedex_api.ui.screen.comun.getTextColorForTypeBackground
import com.david.pokedex_api.ui.screen.comun.pokemonTypeNameTranslator
import com.david.pokedex_api.ui.theme.*


@Composable
fun PokemonSearchMenu(
    searchQuery: String,
    selectedType1: String, // Nombre del tipo, ej: "fire", o NO_TYPE_SELECTED
    selectedType2: String, // Nombre del tipo, ej: "water", o NO_TYPE_SELECTED
    availableTypes: List<String>, // Lista de todos los nombres de tipos disponibles
    onSearchQueryChanged: (String) -> Unit,
    onType1Changed: (String) -> Unit, // Devuelve el nombre del tipo o NO_TYPE_SELECTED
    onType2Changed: (String) -> Unit, // Devuelve el nombre del tipo o NO_TYPE_SELECTED
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // Moderate bouncing
            stiffness = Spring.StiffnessMedium // Moderate stiffness
        )
    )

    Box( // Contenedor externo para el fondo y la forma
        modifier = modifier // El modifier principal se aplica a este Box
            .fillMaxWidth()
            .background(
                color = color_menu_busqueda1,
                shape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
            )
    ) {
        Column( // Contenedor interno para el contenido y su padding
            modifier = Modifier
                .fillMaxWidth()
                // Padding para todo el contenido DENTRO del fondo
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp), // Ajusta el padding inferior según necesites
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,

            ){
                Text(
                    "Filtrar Pokémon",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(.8f)
                )

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isPressed = true
                        try {
                            onSearchQueryChanged("")
                            onType1Changed(NO_TYPE_SELECTED)
                            onType2Changed(NO_TYPE_SELECTED)
                        } finally {
                            isPressed = false // Reset isPressed in finally block
                        }
                    },
                    modifier = Modifier.size(35.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color_fuego_card,
                        contentColor = blanco80
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpiar filtros",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            NameSearchBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    // enabled = selectedType1 != NO_TYPE_SELECTED
                )
            }
            // No necesitas un Spacer al final si el padding inferior de la Column es suficiente
        }
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
        label = { Text("Buscar por nombre") },
        placeholder = { Text("Ej: Pikachu") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            // Personaliza colores si es necesario
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), // Alfa estándar para deshabilitado
            cursorColor = MaterialTheme.colorScheme.secondary,

            // --- Colores del Contenedor (Fondo del campo) ---
            // Estos son los nombres correctos para el fondo del campo de texto en M3
            focusedContainerColor = Color.White.copy(alpha = 0.7f), // O el color que desees, ej: MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
            disabledContainerColor = Color.Transparent,

            // --- Colores del Borde ---
            focusedBorderColor = Color.White.copy(alpha = 0.7f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
            disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), // Alfa estándar para borde deshabilitado

            // --- Colores de la Etiqueta (Label) ---
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),

            // --- Colores del Placeholder ---
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), // Hice el placeholder enfocado un poco más visible
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),

            // --- Colores de los Iconos (leading/trailing) ---
            focusedLeadingIconColor = CardBorder,
            unfocusedLeadingIconColor = CardBorder,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),

            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),

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

    // Colores para el OutlinedTextField
    val textFieldBackgroundColor = if (selectedType != NO_TYPE_SELECTED) {
        getPokemonTypeColor(selectedType).copy(alpha = 0.7f) // Fondo sutil con el color del tipo
    } else {
        Color.White.copy(alpha = 0.5f) // Fondo sutil en blanco
    }
    val textFieldTextColor = if (selectedType != NO_TYPE_SELECTED) {
        // Usa el color principal del tipo si contrasta bien con el fondo alfa,
        // o calcula uno específico. Para un fondo con alfa bajo, el color onSurface suele ir bien.
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textFieldBorderAndLabelColor = if (selectedType != NO_TYPE_SELECTED) {
        getPokemonTypeColor(selectedType) // Borde y label con el color principal del tipo
    } else {
        Color.White.copy(alpha = 0.7f) // Color por defecto para el borde/label
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = pokemonTypeNameTranslator(selectedType).replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled)
            },
            shape = RoundedCornerShape(8.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = textFieldBackgroundColor,
                unfocusedContainerColor = textFieldBackgroundColor,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                focusedTextColor = textFieldTextColor,
                unfocusedTextColor = textFieldTextColor,
                disabledTextColor = textFieldTextColor.copy(alpha = 0.38f),
                focusedBorderColor = textFieldBorderAndLabelColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                focusedLabelColor = textFieldBorderAndLabelColor,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                focusedTrailingIconColor = textFieldTextColor,
                unfocusedTrailingIconColor = textFieldTextColor,
                disabledTrailingIconColor = textFieldTextColor.copy(alpha = 0.38f)
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