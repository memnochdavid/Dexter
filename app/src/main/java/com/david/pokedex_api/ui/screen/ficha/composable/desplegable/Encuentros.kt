package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.GameEncounterGroup
import com.david.pokedex_api.api.model.GameEncounterLocation
import com.david.pokedex_api.ui.screen.ficha.composable.getGameCoverResId
import com.david.pokedex_api.ui.screen.ficha.composable.translateGameVersion
import com.david.pokedex_api.util.Lottie

@Composable
fun PokemonEncountersView(
    encounters: List<GameEncounterGroup>,
    wikiDexLocations: Map<String, String> = emptyMap(),
    isLoading: Boolean,
    colorFondo: Color,
    colorTexto: Color,
    colorDropdown: Color = colorFondo,
    colorAccent: Color = colorFondo,
    modifier: Modifier = Modifier
) {
    // Merge: WikiDex (primario, español) + PokeAPI (fallback, puede tener inglés)
    val allVersions = remember(encounters, wikiDexLocations) {
        val wikiVersions = wikiDexLocations.keys.toList()
        val pokeApiExtra = encounters.map { it.versionName }.filter { it !in wikiDexLocations }
        wikiVersions + pokeApiExtra
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorFondo)
    ) {
        if (isLoading && wikiDexLocations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
            }
        } else if (allVersions.isEmpty()) {
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
            var selectedVersion by rememberSaveable { mutableStateOf(allVersions.first()) }
            val effectiveSelected = if (selectedVersion in allVersions) selectedVersion else allVersions.first()

            // Prioridad: WikiDex español → PokeAPI (fallback)
            val wikiDexText = remember(effectiveSelected, wikiDexLocations) {
                wikiDexLocations[effectiveSelected]
            }
            val pokeApiLocations = remember(effectiveSelected, encounters) {
                encounters.find { it.versionName == effectiveSelected }?.locations ?: emptyList()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp)
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
                        .padding(bottom = 8.dp)
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    // Columna izquierda: selector de juego con caratulas
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        items(allVersions, key = { it }) { version ->
                            val isSelected = version == effectiveSelected
                            val coverRes = getGameCoverResId(version)
                            val haptic = LocalHapticFeedback.current

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(
                                        if (isSelected) Modifier.border(
                                            2.dp,
                                            colorAccent,
                                            RoundedCornerShape(8.dp)
                                        ) else Modifier
                                    )
                                    .background(
                                        if (isSelected) colorDropdown.copy(alpha = 0.6f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedVersion = version
                                    }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (coverRes != 0) {
                                    Image(
                                        painter = painterResource(id = coverRes),
                                        contentDescription = translateGameVersion(version),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                Text(
                                    text = translateGameVersion(version),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorTexto.copy(alpha = if (isSelected) 1f else 0.7f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Columna derecha: contenido de ubicaciones
                    Column(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxSize()
                            .padding(top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = translateGameVersion(effectiveSelected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorTexto,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (wikiDexText != null) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colorTexto.copy(alpha = 0.05f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = wikiDexText,
                                            fontSize = 15.sp,
                                            lineHeight = 22.sp,
                                            color = colorTexto
                                        )
                                    }
                                }
                            }
                        } else if (pokeApiLocations.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(items = pokeApiLocations, key = { it.locationName }) { location ->
                                    LocationEncounterRow(location = location, colorTexto = colorTexto)
                                }
                            }
                        } else {
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
                        }
                    }
                }
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
