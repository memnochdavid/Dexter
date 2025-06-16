package com.david.pokedex_api.ui.composables

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.david.pokedex_api.api.model.PokemonMoveSlot
import com.david.pokedex_api.api.model.VersionGroupDetail // Asegúrate que esta ruta sea correcta
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder // Asumo que tienes este color definido
import com.david.pokedex_api.R

// Función auxiliar para formatear el nombre del método de aprendizaje (opcional)
fun formatMoveLearnMethod(method: String): String {
    return when (method.lowercase()) {
        "level-up" -> "Nivel"
        "machine" -> "MT/MO"
        "tutor" -> "Tutor"
        "egg" -> "Huevo"
        // Añade más casos según necesites
        else -> method.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

// Función auxiliar para obtener el detalle de aprendizaje más relevante (puedes adaptarla)
fun getRelevantVersionGroupDetail(details: List<VersionGroupDetail>): VersionGroupDetail? {
    // Intenta encontrar el detalle para un grupo de versión más reciente o común
    // Esta es una heurística simple, podrías querer algo más sofisticado
    return details.find { it.versionGroup.name.contains("ultra-sun-ultra-moon") } // Ejemplo
        ?: details.find { it.versionGroup.name.contains("sun-moon") }
        ?: details.find { it.versionGroup.name.contains("omega-ruby-alpha-sapphire") }
        ?: details.find { it.versionGroup.name.contains("x-y") }
        ?: details.firstOrNull() // Como último recurso, toma el primero
}


@Composable
fun PokemonMovesList(
    moves: List<PokemonMoveSlot>,
    backgroundColor: Color,
    textColor: Color = CardBorder,
    pokemonApiService: PokeApiService,
    modifier: Modifier = Modifier
) {
    if (moves.isEmpty()) { /* ... */ return }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(vertical = 3.dp)
    ) {
        Text( // Título general "Movimientos"
            text = "Movimientos",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = textColor,
            // ... (otros modificadores del título) ...
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
        )

        // --- Encabezados de la tabla para los detalles ---
        // Se colocarían debajo del título general "Movimientos" y encima de la lista.
        // El padding horizontal de MovesTableHeaders debe ajustarse para que se alinee
        // con el contenido de las filas de detalles en MoveRow.
//        MovesTableHeaders(
//            textColor = textColor,
//            // Ajusta el padding para que los encabezados se alineen con las columnas de MoveRow
//            // Esto es un poco complicado porque MoveRow tiene su propio padding interno.
//            // Y LazyColumn también tiene un padding horizontal.
//            // El padding horizontal de LazyColumn es 14.dp
//            // El padding horizontal interno de la Box en MoveRow es 12.dp
//            // El padding horizontal de MoveDetailItem es 2.dp
//            // Necesitas que el contenido de los Text en MovesTableHeaders se alinee con el contenido
//            // de los Text/Chip en MoveRow.
//            // Por ahora, el padding en MovesTableHeaders (14.dp + 12.dp) intenta alinearse con el
//            // inicio de la Box de MoveRow.
//            modifier = Modifier.padding(bottom = 2.dp) // Espacio entre encabezados y la lista
//        )


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp), // Padding para los ítems de la lista
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(moves, key = { it.move.name }) { moveSlot ->
                MoveRow(
                    moveSlot = moveSlot,
                    pokemonApiService = pokemonApiService
                )
            }
        }
    }
}

@Composable
fun MoveRow(
    moveSlot: PokemonMoveSlot,
    pokemonApiService: PokeApiService
) {
    // --- Estado para los detalles del movimiento (obtenidos por API) ---
    var displayedMoveName by remember {
        mutableStateOf(
            moveSlot.translatedName ?: moveSlot.move.name.replaceFirstChar { it.titlecase() }.replace("-", " ")
        )
    }
    var moveTypeName by remember { mutableStateOf<String?>(null) }
    var movePower by remember { mutableStateOf<Int?>(null) }
    var movePp by remember { mutableStateOf<Int?>(null) }
    var moveAccuracy by remember { mutableStateOf<Int?>(null) }
    var moveDamageClassName by remember { mutableStateOf<String?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var learnIndicatorText by remember { mutableStateOf<String?>(null) }

    val colorTexto = moveTypeName?.let { type ->
        if (esTipoColorOscuro(type)) {
            Color.White
        }
        else{
            CardBorder
        }
    } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    LaunchedEffect(key1 = moveSlot.move.url) {
        if (moveSlot.translatedName == null || moveTypeName == null || movePower == null || moveDamageClassName == null) {
            isLoadingDetails = true
            try {
                val response = pokemonApiService.getMoveDetails(moveSlot.move.url)
                if (response.isSuccessful) {
                    val moveDetails = response.body()
                    moveDetails?.names?.find { it.language.name == "es" }?.let { spanishNameEntry ->
                        displayedMoveName = spanishNameEntry.name
                    }
                    moveTypeName = moveDetails?.moveType?.name
                    movePower = moveDetails?.power
                    movePp = moveDetails?.pp
                    moveAccuracy = moveDetails?.accuracy
                    moveDamageClassName = moveDetails?.damageClass?.name
                } else {
                    println("Error fetching move details: ${response.code()} for ${moveSlot.move.url}")
                    movePower = null; movePp = null; moveAccuracy = null; moveTypeName = null; moveDamageClassName = null;
                }
            } catch (e: Exception) {
                println("Exception fetching move details: ${e.message} for ${moveSlot.move.url}")
                movePower = null; movePp = null; moveAccuracy = null; moveTypeName = null; moveDamageClassName = null;
            }
            isLoadingDetails = false
        } else {
            displayedMoveName = moveSlot.translatedName ?: moveSlot.move.name.replaceFirstChar { it.titlecase() }.replace("-", " ")
        }
    }
    LaunchedEffect(moveSlot.versionGroupDetails) {
        val levelUpDetail = moveSlot.versionGroupDetails.find {
            it.moveLearnMethod.name == "level-up" && it.levelLearnedAt > 0
        }

        if (levelUpDetail != null) {
            learnIndicatorText = "Nv. ${levelUpDetail.levelLearnedAt}"
        } else {
            val machineDetail = moveSlot.versionGroupDetails.find {
                it.moveLearnMethod.name == "machine"
            }
            if (machineDetail != null) {
                learnIndicatorText = "MT/MO" // O un icono de disco
            } else {
                val tutorDetail = moveSlot.versionGroupDetails.find {
                    it.moveLearnMethod.name == "tutor"
                }
                if (tutorDetail != null) {
                    learnIndicatorText = "Tutor" // O un icono de libro/profesor
                } else {
                    val eggDetail = moveSlot.versionGroupDetails.find {
                        it.moveLearnMethod.name == "egg"
                    }
                    if (eggDetail != null) {
                        learnIndicatorText = "Huevo" // O un icono de huevo
                    } else {
                        learnIndicatorText = null // No se muestra nada o un "-" si lo prefieres
                    }
                }
            }
        }
    }

    val backgroundColor = moveTypeName?.let { type ->
        getPokemonTypeColorClear(type)
    } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    // --- Determinar el Nivel de Aprendizaje ---
    val levelLearnedDetail = remember(moveSlot.versionGroupDetails) {
        moveSlot.versionGroupDetails.find { detail ->
            detail.moveLearnMethod.name == "level-up" && detail.levelLearnedAt > 0
        }
    }
    val levelToShow = levelLearnedDetail?.levelLearnedAt

    // --- UI de la Fila de la Tabla ---
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 4.dp, horizontal = 6.dp) // Reducido un poco el padding vertical para más compacidad
    ) {
        if (isLoadingDetails && (moveTypeName == null || movePower == null || moveDamageClassName == null)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            Column {
                // Fila para el Nombre del Movimiento, Icono de Clase y Nivel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // horizontalArrangement = Arrangement.SpaceBetween, // Se ajustará por los pesos
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayedMoveName,
                        style = MaterialTheme.typography.titleSmall, // O bodyMedium si prefieres
                        fontWeight = FontWeight.Bold,
                        color = colorTexto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.6f) // Ajusta el peso según veas
                    )

                    // Icono de Clase de Daño
                    val iconResId: Int? = when (moveDamageClassName?.lowercase()) {
                        "physical" -> R.drawable.fisico
                        "special" -> R.drawable.especial
                        "status" -> R.drawable.estado
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.15f) // Peso para el icono
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterEnd // Alinear el icono a la derecha de su espacio
                    ) {
                        if (iconResId != null) {
                            Image( // Cambiado Image por Icon para mejor control de tint y tamaño con fuentes de iconos
                                painter = painterResource(id = iconResId),
                                contentDescription = moveDamageClassName,
                                modifier = Modifier.size(20.dp), // Tamaño del icono de clase
                            )
                        } else {
                            Spacer(Modifier.size(16.dp)) // Ocupa espacio si no hay icono para mantener alineación
                        }
                    }

                    if (learnIndicatorText != null) {
                        Text(
                            text = learnIndicatorText!!, // No será null aquí debido al if
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            color = colorTexto.copy(alpha = 0.85f),
                            modifier = Modifier.weight(0.25f), // Ajusta el peso
                            textAlign = TextAlign.End
                        )
                    } else {
                        Spacer(Modifier.weight(0.25f)) // Mantiene la estructura si no hay nada que mostrar
                    }
                }

                Spacer(modifier = Modifier.height(5.dp)) // Espacio entre nombre y detalles

                // Fila para los detalles (Potencia, PP, Precisión, Tipo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MoveDetailItem(label = "Pot.", value = movePower?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = colorTexto)
                    MoveDetailItem(label = "PP", value = movePp?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = colorTexto)
                    MoveDetailItem(label = "Prec.", value = moveAccuracy?.let { "$it%" } ?: "-", modifier = Modifier.weight(1f), colorTexto = colorTexto)
                    // Tipo (con chip)
                    Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                        moveTypeName?.let { typeName ->
                            PokemonTypeChip(
                                typeName = typeName,
                                modifier = Modifier.height(28.dp),
                            )
                        } ?: Text("-", style = MaterialTheme.typography.bodySmall, color = colorTexto, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
@Composable
fun MoveDetailItem(label: String, value: String, modifier: Modifier = Modifier, colorTexto: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium, // Etiqueta más pequeña
            color = colorTexto.copy(alpha = 0.7f) // Un poco más tenue
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium, // Valor
            fontWeight = FontWeight.SemiBold,
            color = colorTexto
        )
    }
}
@Composable
fun MovesTableHeaders(
    textColor: Color = CardBorder, // Color por defecto si no se especifica
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // El padding horizontal debería coincidir con el del contenido de MoveRow
            // y el padding horizontal del LazyColumn en PokemonMovesList.
            // Aquí no usamos el mismo padding vertical que en MoveRow, ya que este es solo un encabezado.
            .padding(horizontal = 14.dp + 12.dp, vertical = 8.dp), // Suma del padding de LazyColumn y el padding interno de MoveRow
        verticalAlignment = Alignment.CenterVertically,
        // Usamos SpaceBetween si los pesos distribuyen bien, o puedes usar pesos fijos
        // y Arrangement.Start si algunos encabezados necesitan más espacio que otros.
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Encabezado para Nombre (este texto no es parte de la fila de datos,
        // así que lo ponemos aquí o lo integramos de otra manera en la UI general de PokemonMovesList)
        // Por simplicidad, no lo incluiré aquí directamente, ya que MoveRow ahora tiene el nombre
        // del movimiento grande arriba y los detalles abajo.
        // Si quisieras un encabezado de "Movimiento" encima de los detalles,
        // este sería el lugar para el texto "Movimiento" que se alinea con la columna de nombres.

        // Encabezados para Pot, PP, Prec., Tipo
        // Los pesos deben ser consistentes con los pesos de MoveDetailItem y el Box del PokemonTypeChip en MoveRow
        Text(
            text = "Pot.",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.weight(1f), // Coincide con el peso de MoveDetailItem
            textAlign = TextAlign.Center
        )
        Text(
            text = "PP",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.weight(1f), // Coincide con el peso de MoveDetailItem
            textAlign = TextAlign.Center
        )
        Text(
            text = "Prec.",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.weight(1f), // Coincide con el peso de MoveDetailItem
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tipo",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.weight(1.5f), // Coincide con el peso del Box para PokemonTypeChip
            textAlign = TextAlign.Center
        )
    }
}