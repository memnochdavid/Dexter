package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.david.pokedex_api.util.formatPokemonName

@Composable
fun NombreNum(nombre: String, numero: Int, colorTexto: Color) {
    Text(
        text = "#${numero.toString().padStart(3, '0')}",
        color = colorTexto, // Asegúrate que CardBorder esté definido
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyLarge
    )

    // El estado 'displayName' no es necesario si calculamos el nombre directamente
    // var displayName by remember { mutableStateOf("") }

    val formattedName = remember(nombre) { // Recalcular solo si 'nombre' cambia
        formatPokemonName(nombre)
    }

    Text(
        text = formattedName,
        color = colorTexto, // Asegúrate que CardBorder esté definido
        fontWeight = FontWeight.Medium,
        style = MaterialTheme.typography.headlineMedium,
        // Considera añadir un maxLines y overflow si los nombres pueden ser muy largos
        // maxLines = 2,
        // overflow = TextOverflow.Ellipsis
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun AlturaPeso(altura: Double, peso: Double, colorTexto: Color){
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

@Composable
fun NombreNumAlturaPeso(
    colorFondo: Color,
    colorTexto: Color,
    nombre: String,
    numero: Int,
    altura: Double,
    peso: Double,
    modifier: Modifier = Modifier){

    Card(
        modifier = modifier,
        colors =  CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ){
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ){
            NombreNum(nombre = nombre, numero = numero, colorTexto = colorTexto)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ){
            AlturaPeso(altura = altura, peso = peso, colorTexto = colorTexto)
        }
    }
}