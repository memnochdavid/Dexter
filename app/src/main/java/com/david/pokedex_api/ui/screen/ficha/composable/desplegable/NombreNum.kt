package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.david.pokedex_api.util.formatPokemonName

@Composable
fun NombreNum(nombre: String, numero: Int, genus: String?, colorTexto: Color) { // <--- Añadir genus
    Column( // Usamos Column para apilar los textos verticalmente si es necesario
        modifier = Modifier.wrapContentHeight(), // Añadir padding vertical
        horizontalAlignment = Alignment.CenterHorizontally // Centrar el contenido
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ){
            val formattedName = remember(nombre) {
                formatPokemonName(nombre)
            }

            Text(
                text = formattedName,
                color = colorTexto,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center // Centrar el nombre si es largo
            )
            Text(
                text = "#${numero.toString().padStart(3, '0')}",
                color = colorTexto,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )

        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ){
            // Mostrar el genus si está disponible
            genus?.let {
                Text(
                    text = it, // El genus ya debería estar en español
                    color = colorTexto,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall, // Un estilo más pequeño para el genus
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}

@SuppressLint("DefaultLocale")
@Composable
fun AlturaPeso(altura: Double, peso: Double, colorTexto: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) { // Agrupar verticalmente
        Text(
            text = "Altura: ${String.format("%.1f", altura / 10.0)} m",
            color = colorTexto,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Peso: ${String.format("%.1f", peso / 10.0)} kg",
            color = colorTexto,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun NombreNumAlturaPeso(
    colorFondo: Color,
    colorTexto: Color,
    nombre: String,
    numero: Int,
    genus: String?, // <--- AÑADIR NUEVO PARÁMETRO GENUS
    altura: Double,
    peso: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {
        // Combinar NombreNum y AlturaPeso en una sola Row si el espacio es limitado,
        // o mantenerlos separados si prefieres más espacio.
        // Aquí los pongo en una Column dentro de la Card para mejor organización.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp), // Añadir padding vertical dentro de la Card
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre elementos
        ) {
            NombreNum(
                nombre = nombre,
                numero = numero,
                genus = genus, // <--- PASAR GENUS
                colorTexto = colorTexto
            )
            AlturaPeso(
                altura = altura,
                peso = peso,
                colorTexto = colorTexto
            )
        }
    }
}