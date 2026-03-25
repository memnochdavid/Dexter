package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MuestraDesc(
    numPokemon: String,
    desc: String,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier)
{

    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorFondo),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item{
                Text(
                    text = "Descripción",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorTexto,
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorFondo),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        modifier = Modifier
                            .width(300.dp)
                            .padding(start = 15.dp, end = 15.dp, bottom = 5.dp),
                        text = desc,
                        textAlign = TextAlign.Center,
                        color = colorTexto,
                        fontSize = 18.sp,
                    )
                }
            }
        }
    }
}