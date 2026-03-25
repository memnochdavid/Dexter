package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.david.pokedex_api.api.model.DisplayableEncounter
import com.david.pokedex_api.util.Lottie

@Composable
fun PokemonEncountersView(
    encounters: List<DisplayableEncounter>,
    isLoading: Boolean,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorFondo)
    ) {
        Text(
            text = "Encuentros",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = colorTexto,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(48.dp))
            }
        } else if (encounters.isEmpty()) {
            Text(
                text = "No se encuentra en estado salvaje",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = colorTexto.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = encounters, key = { it.locationName }) { encounter ->
                    EncounterLocationCard(
                        encounter = encounter,
                        colorTexto = colorTexto
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun EncounterLocationCard(
    encounter: DisplayableEncounter,
    colorTexto: Color
) {
    var isExpanded by rememberSaveable(key = "enc_${encounter.locationName}") { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorTexto.copy(alpha = 0.08f))
            .clickable { isExpanded = !isExpanded }
            .padding(10.dp)
    ) {
        // Nombre de la ubicacion
        Text(
            text = encounter.locationName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = colorTexto
        )

        // Resumen: cuantas versiones
        Text(
            text = "${encounter.versions.size} versiones",
            style = MaterialTheme.typography.bodySmall,
            color = colorTexto.copy(alpha = 0.6f)
        )

        // Detalle expandible
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                encounter.versions.forEach { version ->
                    VersionEncounterRow(version = version, colorTexto = colorTexto)
                }
            }
        }
    }
}

@Composable
private fun VersionEncounterRow(
    version: com.david.pokedex_api.api.model.DisplayableVersionEncounter,
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
                text = version.versionName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorTexto
            )
            // Barra de probabilidad
            ChanceIndicator(
                chance = version.maxChance,
                colorTexto = colorTexto
            )
        }

        Spacer(Modifier.height(4.dp))

        version.methods.forEach { method ->
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
