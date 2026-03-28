package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun NombreNumAlturaPeso(
    colorFondo: Color,
    colorTexto: Color,
    nombre: String,
    numero: Int,
    genus: String?,
    altura: Double,
    peso: Double,
    tipo: String,
    modifier: Modifier = Modifier
) {
    val displayName = remember(nombre) {
        adaptaNombre(transformPokemonNameToResourceName(nombre))
    }
    val formattedNum = remember(numero) { "#${numero.toString().padStart(3, '0')}" }

    Column(
        modifier = modifier
            .background(colorFondo)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre + Numero
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

        // Genus
        if (!genus.isNullOrBlank()) {
            Text(
                text = genus,
                color = colorTexto.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Altura + Peso en chips compactos
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
            Spacer(Modifier.width(16.dp))
            StatChip(
                label = "Peso",
                value = String.format(Locale.getDefault(), "%.1f kg", peso / 10.0),
                colorTexto = colorTexto
            )
        }
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

fun adaptaNombre(nombre: String): String {
    val partes = nombre.split("_")
    return partes.joinToString(" ") { parte ->
        when {
            parte.equals("m", ignoreCase = true) -> "\u2642"
            parte.equals("f", ignoreCase = true) -> "\u2640"
            else -> parte.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}

