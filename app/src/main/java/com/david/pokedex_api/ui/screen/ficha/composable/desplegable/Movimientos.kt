package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.david.pokedex_api.api.model.PokemonMoveSlot
import com.david.pokedex_api.api.model.VersionGroupDetail // Asegúrate que esta ruta sea correcta
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder // Asumo que tienes este color definido
import com.david.pokedex_api.R
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.theme.color_progress_bar
import kotlin.collections.putAll
import kotlin.collections.toMap

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

@Composable
fun PokemonMovesList(
    moves: List<PokemonMoveSlot>,
    cardBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface, // Ajusta CardBorder si es un color específico
    pokemonApiService: PokeApiService,
    modifier: Modifier = Modifier,
    initiallyExpandedCard: Boolean = true,
    initiallyExpandedGroups: Boolean = true // Estado inicial para cada grupo de movimientos
) {
    // Estado para el plegado/desplegado GENERAL de la tarjeta "Movimientos"
    var isCardExpanded by rememberSaveable { mutableStateOf(initiallyExpandedCard) }

    // Procesamiento de movimientos (agrupación y ordenación)
    val groupedMoves = remember(moves) { // Recalcular solo si 'moves' cambia
        moves.map { moveSlot ->
            // Primero, determina el indicador para cada movimiento
            determineLearnIndicatorText(moveSlot.versionGroupDetails) to moveSlot
        }
            .filter { (indicator, _) -> indicator != null } // Filtra los que no tienen indicador
            .groupBy(
                keySelector = { (indicator, _) -> indicator!! }, // Agrupa por el indicador
                valueTransform = { (_, moveSlot) -> moveSlot }    // Solo el moveSlot en la lista de valores
            )
            .map { (indicator, moveSlotsInGroup) ->
                // Ordena los movimientos dentro de cada grupo si es necesario (ej. por nivel)
                val sortedMoveSlots = if (indicator == "Nivel") {
                    moveSlotsInGroup.sortedWith(compareBy { moveSlot ->
                        moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "level-up" }?.levelLearnedAt ?: Int.MAX_VALUE
                    })
                } else {
                    moveSlotsInGroup // Sin ordenación específica para otros grupos
                }
                indicator to sortedMoveSlots
            }
            .sortedWith(compareBy { (indicator, _) -> // Ordena los grupos
                when (indicator) {
                    "Nivel" -> 0
                    "MT/MO" -> 1
                    "Tutor" -> 2
                    "Huevo" -> 3
                    else -> 4
                }
            })
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- Cabecera General "Movimientos" (plegable) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .clickable { isCardExpanded = !isCardExpanded }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Movimientos",
                    style = MaterialTheme.typography.titleLarge, // O el estilo que prefieras
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
//                val cardRotationAngle by animateFloatAsState(
//                    targetValue = if (isCardExpanded) 180f else 0f, // Rota para que la flecha apunte hacia arriba cuando está expandido
//                    label = "cardArrowRotation"
//                )
//                Icon(
//                    imageVector = Icons.Filled.ArrowDropDown, // Puedes cambiarlo por KeyboardArrowUp/Down si prefieres
//                    contentDescription = if (isCardExpanded) "Plegar Movimientos" else "Expandir Movimientos",
//                    tint = textColor,
//                    modifier = Modifier.graphicsLayer(rotationZ = cardRotationAngle)
//                )
            }

            // --- Contenido Plegable General de la Tarjeta ---
            AnimatedVisibility(
                visible = isCardExpanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                // Columna para el contenido que se mostrará/ocultará
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    if (moves.isEmpty()) { // Comprobación dentro de AnimatedVisibility si solo se muestra cuando está expandido
//                        Text(
//                            text = "No hay movimientos disponibles.",
//                            textAlign = TextAlign.Center,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            color = textColor
//                        )
                    } else if (groupedMoves.isEmpty()) { // Si hay movimientos pero no se pudieron agrupar (ej. todos los indicadores fueron null)
//                        Text(
//                            text = "No se pudo determinar el método de aprendizaje para los movimientos.",
//                            textAlign = TextAlign.Center,
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(16.dp),
//                            color = textColor
//                        )
                    } else {
                        // Usamos LazyColumn para la lista de secciones plegables (grupos)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp), // Padding lateral para la lista de grupos
                            contentPadding = PaddingValues(top = 4.dp),
                            // No es necesario verticalArrangement.spacedBy aquí si CollapsibleSection maneja su propio padding/margen
                        ) {
                            items(
                                items = groupedMoves,
                                key = { (indicator, _) -> "group_section_$indicator" } // Clave única para cada sección de grupo
                            ) { (learnIndicatorText, movesInGroup) ->
                                CollapsibleSection(
                                    title = learnIndicatorText,
                                    initiallyExpanded = initiallyExpandedGroups, // Usa el parámetro
                                    headerTextColor = textColor,
                                    modifier = Modifier.padding(bottom = 8.dp) // Espacio entre secciones colapsables
                                ) { contentModifier -> // El lambda 'content' de CollapsibleSection
                                    // Este Column es el contenido de la sección plegable
                                    Column(
                                        modifier = contentModifier, // Aplica el modifier pasado por CollapsibleSection
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        movesInGroup.forEach { moveSlot ->
                                            MoveRow(
                                                moveSlot = moveSlot,
                                                pokemonApiService = pokemonApiService
                                                // backgroundColor y textColor se manejan dentro de MoveRow
                                                // o se pueden pasar si es necesario
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CollapsibleSection( // Eliminado el <T> de aquí
    title: String,
    initiallyExpanded: Boolean = true,
    headerTextColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier,
    content: @Composable (modifier: Modifier) -> Unit // El lambda content ahora no depende de T
) {
    // El 'key' en rememberSaveable ayuda a que cada instancia de CollapsibleSection
    // (incluso si se reutiliza el mismo título en contextos muy diferentes, aunque aquí
    // los títulos de grupo deberían ser únicos) mantenga su propio estado.
    var isExpanded by rememberSaveable(key = title) { mutableStateOf(initiallyExpanded) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Cabecera de la sección
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = headerTextColor,
                modifier = Modifier.weight(1f)
            )
//            val rotationAngle by animateFloatAsState(
//                targetValue = if (isExpanded) 180f else 0f,
//                label = "arrowRotation_$title" // Etiqueta única para la animación
//            )
//            Icon(
//                imageVector = Icons.Filled.ArrowDropDown,
//                contentDescription = if (isExpanded) "Colapsar $title" else "Expandir $title",
//                tint = headerTextColor,
//                modifier = Modifier.graphicsLayer(rotationZ = rotationAngle)
//            )
        }

        // Contenido animado
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            // Se pasa un Modifier al lambda de content para que el desarrollador
            // pueda aplicarlo al contenedor raíz del contenido.
            // Esto permite, por ejemplo, añadir padding consistente al contenido desde dentro.
            content(
                Modifier // Este es el Modifier que recibe el lambda
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
            )
        }
    }
}
/*
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
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = color_progress_bar)
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
                            Image(
                                // Cambiado Image por Icon para mejor control de tint y tamaño con fuentes de iconos
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
*/
@Composable
fun MoveRow(
    moveSlot: PokemonMoveSlot,
    pokemonApiService: PokeApiService
) {
    var displayedMoveName by remember(moveSlot.translatedName, moveSlot.move.name) {
        mutableStateOf(
            moveSlot.translatedName ?: moveSlot.move.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString()
            }.replace("-", " ")
        )
    }
    var moveTypeName by remember { mutableStateOf<String?>(null) }
    var movePower by remember { mutableStateOf<Int?>(null) }
    var movePp by remember { mutableStateOf<Int?>(null) }
    var moveAccuracy by remember { mutableStateOf<Int?>(null) }
    var moveDamageClassName by remember { mutableStateOf<String?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var learnIndicatorText by remember { mutableStateOf<String?>(null) }

    val actualColorTexto = moveTypeName?.let { type ->
        if (esTipoColorOscuro(type)) { // Asegúrate que esTipoColorOscuro está definida
            Color.White
        } else {
            Color.Black // O tu Color CardBorder si lo prefieres para texto oscuro
        }
    } ?: MaterialTheme.colorScheme.onSurface // Un color por defecto si el tipo es nulo

    LaunchedEffect(key1 = moveSlot.move.url) {
        // Carga detalles solo si no los tenemos ya (ej. el nombre traducido Y el tipo)
        // O si algún dato crucial como el tipo de movimiento falta.
        if (moveSlot.translatedName == null || moveTypeName == null) {
            isLoadingDetails = true
            try {
                // CAMBIO AQUÍ: Usar el nombre correcto del método del servicio
                val response = pokemonApiService.getMoveDetailsByUrl(moveSlot.move.url)
                //                                 ^^^^^^^^^^^^^^^^^^^
                if (response.isSuccessful) {
                    val moveDetails = response.body()
                    moveDetails?.names?.find { it.language.name == "es" }?.name?.let { spanishNameEntry ->
                        displayedMoveName = spanishNameEntry
                    }
                    // Si no hay nombre en español, nos quedamos con el nombre por defecto ya seteado
                    moveTypeName = moveDetails?.moveType?.name
                    movePower = moveDetails?.power
                    movePp = moveDetails?.pp
                    moveAccuracy = moveDetails?.accuracy
                    moveDamageClassName = moveDetails?.damageClass?.name
                } else {
                    println("Error fetching move details: ${response.code()} for ${moveSlot.move.url}")
                    // No limpiamos todos los datos si la llamada falla,
                    // podríamos tener el nombre base o traducido de antes.
                    // Si la API falla, es mejor mostrar "-" para los datos no cargados.
                }
            } catch (e: Exception) {
                println("Exception fetching move details: ${e.message} for ${moveSlot.move.url}")
            }
            isLoadingDetails = false
        } else {
            // Si ya tenemos el nombre traducido y el tipo, los otros datos también deberían estar.
            // Esto es por si `moveSlot` ya viene con todos los datos precargados.
            // Pero como la condición del if es `translatedName == null || moveTypeName == null`
            // este `else` implica que ya tenemos esos datos, así que no es necesario resetearlos.
            // El `displayedMoveName` ya se inicializó correctamente arriba.
            // Para `moveTypeName`, `movePower`, etc., si `moveSlot` pudiera traerlos
            // directamente, los inicializarías arriba también. Como no es el caso (según el código actual),
            // el `LaunchedEffect` se encarga de llenarlos.
        }
    }

    LaunchedEffect(moveSlot.versionGroupDetails) {
        val levelUpDetail = moveSlot.versionGroupDetails.find {
            it.moveLearnMethod.name == "level-up" && it.levelLearnedAt > 0
        }

        learnIndicatorText = if (levelUpDetail != null) {
            "Nv. ${levelUpDetail.levelLearnedAt}"
        } else {
            val machineDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "machine" }
            if (machineDetail != null) {
                "MT/MO"
            } else {
                val tutorDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "tutor" }
                if (tutorDetail != null) {
                    "Tutor"
                } else {
                    val eggDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "egg" }
                    if (eggDetail != null) "Huevo" else null
                }
            }
        }
    }

    val backgroundColor = moveTypeName?.let { type ->
        getPokemonTypeColorClear(type) // Asegúrate que getPokemonTypeColorClear está definida
    } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(vertical = 6.dp, horizontal = 8.dp) // Ajuste de padding
    ) {
        // Muestra el indicador de carga solo si isLoadingDetails es true
        // Y algunos datos clave (como moveTypeName) aún no se han cargado.
        if (isLoadingDetails && moveTypeName == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp), // Darle algo de espacio
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFFD32F2F) // color_progress_bar - Reemplaza con tu color
                )
            }
        } else {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayedMoveName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = actualColorTexto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.55f) // Ajustado
                    )
                    val iconResId: Int? = when (moveDamageClassName?.lowercase(java.util.Locale.getDefault())) {
                        "physical" -> R.drawable.fisico // Asegúrate que tus drawables existen
                        "special" -> R.drawable.especial
                        "status" -> R.drawable.estado
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.15f) // Peso para el icono de clase
                            .padding(horizontal = 4.dp), // Espacio alrededor del icono
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (iconResId != null) {
                            Image(
                                painter = painterResource(id = iconResId),
                                contentDescription = moveDamageClassName ?: "Clase de daño",
                                modifier = Modifier.size(20.dp), // Tamaño del icono de clase
                            )
                        } else {
                            // Ocupa espacio si no hay icono para mantener la alineación
                            // O puedes omitir el Box si prefieres que el texto del nivel se mueva
                            Spacer(Modifier.size(20.dp))
                        }
                    }

                    // Indicador de aprendizaje (Nivel, MT/MO, Tutor, Huevo)
                    if (learnIndicatorText != null) {
                        Text(
                            text = learnIndicatorText!!, // No será null aquí debido al if
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal, // O Bold si prefieres
                            color = actualColorTexto.copy(alpha = 0.85f), // Ligeramente más tenue
                            modifier = Modifier.weight(0.30f), // Ajusta el peso según veas necesario
                            textAlign = TextAlign.End // Alinear a la derecha de su espacio
                        )
                    } else {
                        // Si no hay texto de aprendizaje, ocupa el espacio para mantener la estructura
                        Spacer(Modifier.weight(0.30f))
                    }
                }

                Spacer(modifier = Modifier.height(5.dp)) // Espacio entre la primera fila y los detalles

                // Fila para los detalles (Potencia, PP, Precisión, Tipo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Distribuye el espacio entre los items
                ) {
                    MoveDetailItem(label = "Pot.", value = movePower?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)
                    MoveDetailItem(label = "PP", value = movePp?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)
                    MoveDetailItem(label = "Prec.", value = moveAccuracy?.let { "$it%" } ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)

                    // Tipo del movimiento (con chip)
                    Box(
                        modifier = Modifier.weight(1.5f), // Darle un poco más de peso si es necesario
                        contentAlignment = Alignment.Center // Centrar el chip en su espacio
                    ) {
                        if (moveTypeName != null) {
                            PokemonTypeChip( // Asegúrate de que este Composable está definido y disponible
                                typeName = moveTypeName!!,
                                modifier = Modifier.height(28.dp), // Altura del chip
                                // Puedes pasarle el color del texto si tu PokemonTypeChip lo soporta
                                // textStyle = MaterialTheme.typography.labelSmall.copy(color = ...)
                            )
                        } else {
                            // Muestra un guion si el tipo no está cargado
                            Text(
                                "-",
                                style = MaterialTheme.typography.bodySmall,
                                color = actualColorTexto,
                                textAlign = TextAlign.Center
                            )
                        }
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


private fun determineLearnIndicatorText(versionGroupDetails: List<VersionGroupDetail>): String? {
    // Ejemplo basado en la lógica que tenías en MoveRow:
    val levelUpDetail = versionGroupDetails.find {
        it.moveLearnMethod.name == "level-up" && it.levelLearnedAt > 0
    }
    if (levelUpDetail != null) {
        // Si quieres el texto "Nivel" en lugar de "Nv. X", puedes usar formatMoveLearnMethod
        // return formatMoveLearnMethod("level-up")
        return "Nivel" // O "Nv. ${levelUpDetail.levelLearnedAt}" si prefieres incluir el nivel aquí
    }

    val machineDetail = versionGroupDetails.find {
        it.moveLearnMethod.name == "machine"
    }
    if (machineDetail != null) {
        return formatMoveLearnMethod("machine") // "MT/MO"
    }

    val tutorDetail = versionGroupDetails.find {
        it.moveLearnMethod.name == "tutor"
    }
    if (tutorDetail != null) {
        return formatMoveLearnMethod("tutor") // "Tutor"
    }

    val eggDetail = versionGroupDetails.find {
        it.moveLearnMethod.name == "egg"
    }
    if (eggDetail != null) {
        return formatMoveLearnMethod("egg") // "Huevo"
    }

    // Considera un valor por defecto si quieres agrupar movimientos
    // cuyo método no coincide con los anteriores
    // return "Otros"
    return null // Si devuelves null, estos movimientos se filtrarán antes de agrupar
}

