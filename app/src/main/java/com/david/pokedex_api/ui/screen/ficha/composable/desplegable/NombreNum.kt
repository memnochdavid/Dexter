package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.painter.Painter
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.david.pokedex_api.R
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NombreNumAlturaPeso(
    colorFondo: Color,
    colorTexto: Color,
    nombre: String,
    numero: Int,
    genus: String?,
    formName: String? = null,
    altura: Double,
    peso: Double,
    tipo: String,
    cryUrl: String? = null,
    generationNum: Int? = null,
    regionTag: String? = null,
    isLegendary: Boolean = false,
    isMythical: Boolean = false,
    isMega: Boolean = false,
    isGigamax: Boolean = false,
    hasFemale: Boolean = false,
    isFemale: Boolean = false,
    onToggleGender: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    // nombre ya es el nombre localizado de la especie (ej. "Unown", "Vivillon").
    // No pasar por transformPokemonNameToResourceName porque mapea a nombres de
    // recurso gráfico (ej. "unown" → "unown_a", "vivillon" → "vivillon_vergel").
    val displayName = nombre
    val formattedNum = remember(numero) { "#${numero.toString().padStart(3, '0')}" }

    val darkerFondo = Color(
        colorFondo.red * 0.82f,
        colorFondo.green * 0.82f,
        colorFondo.blue * 0.82f,
        colorFondo.alpha
    )

    // ¿Hay algún badge que mostrar?
    val hasBadges = generationNum != null || isLegendary || isMythical || regionTag != null || isMega || isGigamax

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(colorFondo, darkerFondo)))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Línea 1: Nombre + Número
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                color = colorTexto,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = formattedNum,
                color = colorTexto.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Línea 2: Genus + Forma
        val genusFormText = remember(genus, formName) {
            when {
                !genus.isNullOrBlank() && !formName.isNullOrBlank() -> "$genus (Forma $formName)"
                !genus.isNullOrBlank() -> genus
                !formName.isNullOrBlank() -> "Forma $formName"
                else -> null
            }
        }
        if (genusFormText != null) {
            Text(
                text = genusFormText,
                color = colorTexto.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Línea 3: Badges (solo los que apliquen)
        if (hasBadges) {
            Spacer(Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (generationNum != null) {
                    InfoBadge(
                        text = "G-$generationNum",
                        accentColor = colorTexto.copy(alpha = 0.6f)
                    )
                }
                if (isLegendary) {
                    InfoBadge(
                        text = "Legendario",
                        accentColor = Color(0xFFFFD700),
                        icon = painterResource(id = R.drawable.ic_legendary)
                    )
                }
                if (isMythical) {
                    InfoBadge(
                        text = "Singular",
                        accentColor = Color(0xFFCB8FF7),
                        icon = painterResource(id = R.drawable.ic_mythical)
                    )
                }
                if (regionTag != null) {
                    InfoBadge(
                        text = regionTag,
                        accentColor = Color(0xFF4ECDC4),
                        icon = painterResource(id = R.drawable.ic_region)
                    )
                }
                if (isMega) {
                    InfoBadge(
                        text = "Mega",
                        accentColor = Color(0xFFFF6B6B),
                        icon = painterResource(id = R.drawable.ic_mega)
                    )
                }
                if (isGigamax) {
                    InfoBadge(
                        text = "Gigamax",
                        accentColor = Color(0xFFFF9F43),
                        icon = painterResource(id = R.drawable.ic_gigamax)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Línea 4: Altura + Peso + Botones (género + cry)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(
                label = "Altura",
                value = String.format(Locale.getDefault(), "%.1f m", altura / 10.0),
                colorTexto = colorTexto
            )
            Spacer(Modifier.width(12.dp))
            StatChip(
                label = "Peso",
                value = String.format(Locale.getDefault(), "%.1f kg", peso / 10.0),
                colorTexto = colorTexto
            )

            Spacer(Modifier.weight(1f))

            // Botones de género (solo si hay dimorfismo sexual)
            if (hasFemale) {
                GenderButton(
                    symbol = "\u2642",
                    isSelected = !isFemale,
                    colorTexto = colorTexto,
                    selectedColor = Color(0xFF6CB4EE),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleGender(false)
                    }
                )
                Spacer(Modifier.width(4.dp))
                GenderButton(
                    symbol = "\u2640",
                    isSelected = isFemale,
                    colorTexto = colorTexto,
                    selectedColor = Color(0xFFFF6B8A),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleGender(true)
                    }
                )
            }

            // Cry button
            if (cryUrl != null) {
                Spacer(Modifier.width(if (hasFemale) 8.dp else 0.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colorTexto.copy(alpha = 0.12f))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            playCry(context, cryUrl)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cry_logo),
                        contentDescription = "Escuchar cry",
                        modifier = Modifier.size(16.dp),
                        tint = colorTexto.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(
    text: String,
    accentColor: Color,
    icon: Painter? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(accentColor.copy(alpha = 0.12f))
            .border(width = 1.dp, color = accentColor.copy(alpha = 0.35f), shape = shape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = accentColor
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = text.uppercase(),
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun GenderButton(
    symbol: String,
    isSelected: Boolean,
    colorTexto: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) selectedColor.copy(alpha = 0.3f)
    else colorTexto.copy(alpha = 0.08f)
    val textColor = if (isSelected) selectedColor
    else colorTexto.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, colorTexto: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorTexto.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = colorTexto.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            color = colorTexto,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun playCry(context: Context, url: String) {
    val player = ExoPlayer.Builder(context).build()
    player.setMediaItem(MediaItem.fromUri(url))
    player.prepare()
    player.play()
    player.addListener(object : androidx.media3.common.Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                player.release()
            }
        }
    })
}

fun adaptaNombre(nombre: String): String {
    val partes = nombre.split("_")
    val isUnown = partes.firstOrNull()?.equals("unown", ignoreCase = true) == true
    return partes.joinToString(" ") { parte ->
        when {
            !isUnown && parte.equals("m", ignoreCase = true) -> "\u2642"
            !isUnown && parte.equals("f", ignoreCase = true) -> "\u2640"
            else -> parte.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}
