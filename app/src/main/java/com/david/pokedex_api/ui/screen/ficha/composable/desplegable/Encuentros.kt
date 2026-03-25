package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.GameEncounterGroup
import com.david.pokedex_api.api.model.GameEncounterLocation
import com.david.pokedex_api.util.Lottie

@Composable
fun PokemonEncountersView(
    encounters: List<GameEncounterGroup>,
    isLoading: Boolean,
    colorFondo: Color,
    colorTexto: Color,
    colorDropdown: Color = colorFondo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorFondo)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
            }
        } else if (encounters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encuentra en estado salvaje",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = colorTexto.copy(alpha = 0.7f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            var selectedVersion by rememberSaveable { mutableStateOf(encounters.first().versionName) }
            val currentLocations = remember(selectedVersion, encounters) {
                encounters.find { it.versionName == selectedVersion }?.locations ?: emptyList()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Titulo
                Text(
                    text = "Ubicaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )

                // Selector de juego
                EncounterVersionSelector(
                    versions = encounters.map { it.versionName },
                    selectedVersion = selectedVersion,
                    onVersionSelected = { selectedVersion = it },
                    colorDropdown = colorDropdown,
                    colorTexto = colorTexto
                )

                Spacer(Modifier.height(10.dp))

                // Ubicaciones del juego seleccionado
                if (currentLocations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin ubicaciones para este juego",
                            color = colorTexto.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items = currentLocations, key = { it.locationName }) { location ->
                            LocationEncounterRow(location = location, colorTexto = colorTexto)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EncounterVersionSelector(
    versions: List<String>,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
    colorDropdown: Color,
    colorTexto: Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedVersion,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                focusedContainerColor = colorDropdown.copy(alpha = 0.5f),
                unfocusedContainerColor = colorDropdown.copy(alpha = 0.3f),
                focusedBorderColor = colorTexto.copy(alpha = 0.3f),
                unfocusedBorderColor = colorTexto.copy(alpha = 0.15f),
                focusedTextColor = colorTexto,
                unfocusedTextColor = colorTexto,
                focusedTrailingIconColor = colorTexto,
                unfocusedTrailingIconColor = colorTexto.copy(alpha = 0.6f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colorDropdown)
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = version,
                            fontWeight = if (version == selectedVersion) FontWeight.Bold else FontWeight.Normal,
                            color = colorTexto
                        )
                    },
                    onClick = {
                        onVersionSelected(version)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationEncounterRow(
    location: GameEncounterLocation,
    colorTexto: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorTexto.copy(alpha = 0.05f))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = location.locationName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorTexto,
                modifier = Modifier.weight(1f)
            )
            ChanceIndicator(chance = location.maxChance, colorTexto = colorTexto)
        }

        Spacer(Modifier.height(4.dp))

        location.methods.forEach { method ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = method.methodName,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                val levelText = if (method.minLevel == method.maxLevel) {
                    "Nv. ${method.minLevel}"
                } else {
                    "Nv. ${method.minLevel}-${method.maxLevel}"
                }
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorTexto.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${method.chance}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorTexto.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun ChanceIndicator(chance: Int, colorTexto: Color) {
    val barColor = when {
        chance >= 50 -> Color(0xFF4CAF50)
        chance >= 20 -> Color(0xFFF6BD00)
        chance >= 5 -> Color(0xFFFF9800)
        else -> Color(0xFFE53935)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(colorTexto.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (chance / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
        Text(
            text = "$chance%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorTexto.copy(alpha = 0.8f)
        )
    }
}
