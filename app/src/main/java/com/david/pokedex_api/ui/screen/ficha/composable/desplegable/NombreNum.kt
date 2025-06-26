package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
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
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChipForColumn
import com.david.pokedex_api.util.formatPokemonName
import java.util.Locale

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
//                formatPokemonName(nombre)
                nombre
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
    tipo: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colorFondo
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
//                .padding(vertical = 8.dp), // Añadir padding vertical dentro de la Card
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
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
                    nombre = adaptaNombre(transformPokemonNameToResourceName(nombre)),
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
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth(0.4f)
//                    .padding(vertical = 8.dp), // Añadir padding vertical dentro de la Card
//                horizontalAlignment = Alignment.CenterHorizontally,
////                verticalArrangement = Arrangement.spacedBy(4.dp) // Espacio entre elementos
//            ){
//                PokemonTypeChipForColumn(
//                    typeName = tipo,
//                    modifier = Modifier.wrapContentWidth()
//                )
//            }
        }

    }
}

fun adaptaNombre(nombre: String): String {
    val partes = nombre.split("_")

    // Función auxiliar interna para capitalizar una parte
    fun capitalizarParte(parte: String): String {
        if (parte.isEmpty()) return ""
        // Si la parte es una sola letra como "x", la queremos en mayúscula "X"
        if (parte.length == 1) return parte.uppercase(Locale.getDefault())
        return parte.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    return when (partes.size) {
        1 -> { // Añadido caso para una sola parte
            capitalizarParte(partes[0])
        }
        2 -> {
            if(partes[1].equals("macho")){
                capitalizarParte(partes[0]) + " ♂"
            }
            else if(partes[1].equals("hembra")){
                capitalizarParte(partes[0]) + " ♀"
            }
            else{
                capitalizarParte(partes[0]) + " " +
                        capitalizarParte(partes[1])
            }
        }
        3 -> { // Este es el caso para "mega_charizard_x"
            capitalizarParte(partes[0]) + " " +
                    if(!partes[1].equals("de")) capitalizarParte(partes[1]) else partes[1] + " " +
                    capitalizarParte(partes[2]) // Asegúrate que esta parte se capitalice
        }
        4 -> { // Para "tauros_de_paldea_combatiente"
            capitalizarParte(partes[0]) + " " +
                    // Aquí mantienes la lógica original de no capitalizar las partes intermedias
                    partes[1] + " " +
                    partes[2] + " " +
                    capitalizarParte(partes[3])
        }
        else -> {
            // Para el caso 'else', si el nombre original tiene guiones bajos,
            // podríamos querer capitalizar todas las partes también, o devolverlo tal cual.
            // Opción 1: Devolverlo tal cual (como lo tenías)
            // nombre
            // Opción 2: Intentar capitalizar todas las partes
            partes.joinToString(" ") { capitalizarParte(it) }
        }
    }
}