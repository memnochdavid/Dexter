package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.david.pokedex_api.util.Lottie
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.PokemonMoveSlot
import com.david.pokedex_api.api.model.VersionGroupDetail
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.comun.PokemonTypeChip
import com.david.pokedex_api.ui.screen.comun.esTipoColorOscuro
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorClear
import com.david.pokedex_api.ui.screen.comun.getPokemonTypeColorSurface

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
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    pokemonApiService: PokeApiService,
    moveDetailsMap: Map<String, MoveDetailResponse> = emptyMap(),
    modifier: Modifier = Modifier,
    initiallyExpandedCard: Boolean = true,
    initiallyExpandedGroups: Boolean = true
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
            .fillMaxSize(),
        shape = RoundedCornerShape(0.dp),
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
                                                pokemonApiService = pokemonApiService,
                                                cachedMoveDetail = moveDetailsMap[moveSlot.move.url]
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

@Composable
fun MoveRow(
    moveSlot: PokemonMoveSlot,
    pokemonApiService: PokeApiService,
    cachedMoveDetail: MoveDetailResponse? = null
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
    var moveDescription by remember { mutableStateOf<String?>(null) }

    var isExpanded by rememberSaveable(key = moveSlot.move.name) { mutableStateOf(false) }

//    val actualColorTexto = moveTypeName?.let { type ->
//        if (esTipoColorOscuro(type)) Color.White else Color.Black
//    } ?: MaterialTheme.colorScheme.onSurface

    val actualColorTexto = Color.Black

    // Función para aplicar los datos de un MoveDetailResponse a los estados locales
    fun applyMoveDetails(moveDetails: MoveDetailResponse?) {
        if (moveDetails == null) return
        if (moveSlot.translatedName == null) {
            val spanishNameEntry = moveDetails.names.find { it.language.name == "es" }?.name
            if (!spanishNameEntry.isNullOrBlank()) displayedMoveName = spanishNameEntry
        }
        moveTypeName = moveDetails.moveType?.name
        movePower = moveDetails.power
        movePp = moveDetails.pp
        moveAccuracy = moveDetails.accuracy
        moveDamageClassName = moveDetails.damageClass?.name

        val spanishEffectEntry = moveDetails.effectEntries.find { it.language.name == "es" }
        var foundDescription = spanishEffectEntry?.effect?.takeIf { it.isNotBlank() }
            ?: spanishEffectEntry?.shortEffect?.takeIf { it.isNotBlank() }

        if (foundDescription != null) {
            val effectChanceValue = moveDetails.accuracy
            if (effectChanceValue != null) {
                foundDescription = foundDescription.replace("\$effect_chance", effectChanceValue.toString())
            }
            moveDescription = foundDescription.replace("\n", " ").replace("\u000c", " ")
        } else {
            val spanishFlavorText = moveDetails.flavorTextEntries
                .filter { it.language.name == "es" }
                .firstOrNull { it.flavorText.isNotBlank() }
                ?.flavorText
            moveDescription = spanishFlavorText?.replace("\n", " ")?.replace("\u000c", " ")
                ?: "Descripción en español no disponible."
        }
    }

    // Si el ViewModel ya pre-cargó este movimiento, usarlo directamente sin llamada API
    LaunchedEffect(key1 = moveSlot.move.url, key2 = cachedMoveDetail) {
        if (moveTypeName != null) return@LaunchedEffect // Ya procesado

        if (cachedMoveDetail != null) {
            applyMoveDetails(cachedMoveDetail)
            return@LaunchedEffect
        }

        // Fallback: si el caché no lo tiene, llamar a la API (servida desde caché HTTP de OkHttp)
        isLoadingDetails = true
        try {
            val response = pokemonApiService.getMoveDetailsByUrl(moveSlot.move.url)
            if (response.isSuccessful) {
                applyMoveDetails(response.body())
            } else {
                if (moveSlot.translatedName == null) displayedMoveName = moveSlot.move.name.replace("-", " ") + " (Error)"
                moveDescription = "No se pudo cargar la descripción."
            }
        } catch (e: Exception) {
            if (moveSlot.translatedName == null) displayedMoveName = moveSlot.move.name.replace("-", " ") + " (Excepción)"
            moveDescription = "Error al cargar la descripción."
        }
        isLoadingDetails = false
    }

    // LaunchedEffect para learnIndicatorText (sin cambios)
    LaunchedEffect(moveSlot.versionGroupDetails) {
        val levelUpDetail = moveSlot.versionGroupDetails.find {
            it.moveLearnMethod.name == "level-up" && it.levelLearnedAt > 0
        }
        learnIndicatorText = if (levelUpDetail != null) {
            "Nv. ${levelUpDetail.levelLearnedAt}"
        } else {
            val machineDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "machine" }
            if (machineDetail != null) "MT/MO"
            else {
                val tutorDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "tutor" }
                if (tutorDetail != null) "Tutor"
                else {
                    val eggDetail = moveSlot.versionGroupDetails.find { it.moveLearnMethod.name == "egg" }
                    if (eggDetail != null) "Huevo" else null
                }
            }
        }
    }

    val backgroundColor = moveTypeName?.let { type ->
        getPokemonTypeColorSurface(type)
    } ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    // ***** ENVOLVER EL CONTENIDO EN UN COLUMN Y HACER CLICKEABLE EL BOX PRINCIPAL *****
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                // Opcional: añade una indicación de ripple si quieres
                // interactionSource = remember { MutableInteractionSource() },
                // indication = rememberRipple(bounded = true),
                onClick = { isExpanded = !isExpanded }
            )
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        // Contenido principal de la fila (siempre visible)
        if (isLoadingDetails && moveTypeName == null) { // Muestra shimmer o loading solo para la parte principal si es necesario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(24.dp))
            }
        } else {
            // Fila para el nombre, clase de daño e indicador de aprendizaje
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
                    modifier = Modifier.weight(0.50f) // Ajusta pesos según necesites
                )
                val iconResId: Int? = when (moveDamageClassName?.lowercase(java.util.Locale.getDefault())) {
                    "physical" -> R.drawable.fisico
                    "special" -> R.drawable.especial
                    "status" -> R.drawable.estado
                    else -> null
                }
                Box(
                    modifier = Modifier
                        .weight(0.15f)
                        .padding(horizontal = 4.dp),
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
                        Spacer(Modifier.size(20.dp))
                    }
                }

                // Indicador de aprendizaje (Nivel, MT/MO, Tutor, Huevo)
                if (learnIndicatorText != null) {
                    Text(
                        text = learnIndicatorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = actualColorTexto.copy(alpha = 0.85f),
                        modifier = Modifier.weight(0.35f), // Ajusta el peso
                        textAlign = TextAlign.End
                    )
                } else {
                    Spacer(Modifier.weight(0.35f))
                }
            }

            Spacer(modifier = Modifier.height(5.dp)) // Espacio entre la primera fila y los detalles

            // Fila para los detalles (Potencia, PP, Precisión, Tipo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MoveDetailItem(label = "Pot.", value = movePower?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)
                MoveDetailItem(label = "PP", value = movePp?.toString() ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)
                MoveDetailItem(label = "Prec.", value = moveAccuracy?.let { "$it%" } ?: "-", modifier = Modifier.weight(1f), colorTexto = actualColorTexto)

                Box(
                    modifier = Modifier.weight(1.5f),
                    contentAlignment = Alignment.Center
                ) {
                    if (moveTypeName != null) {
                        PokemonTypeChip(
                            typeName = moveTypeName!!,
                            modifier = Modifier.height(28.dp),
                        )
                    } else {
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

        // ***** SECCIÓN DE DESCRIPCIÓN EXPANDIBLE *****
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            // Solo muestra la descripción si no está cargando y si la descripción existe
            if (!isLoadingDetails && !moveDescription.isNullOrBlank()) {
                Column { // Envuelve en Column para poder añadir padding superior
                    Spacer(modifier = Modifier.height(8.dp)) // Espacio entre detalles y descripción
                    Text(
                        text = moveDescription!!,
                        style = MaterialTheme.typography.bodyMedium, // Un poco más grande para la descripción
                        color = actualColorTexto.copy(alpha = 0.9f), // Ligeramente más opaco
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp), // Padding interno para la descripción
                        textAlign = TextAlign.Justify // Opcional: justificar el texto
                    )
                }
            } else if (isExpanded && isLoadingDetails) {
                // Opcional: Mostrar un pequeño indicador de carga para la descripción
                // si la fila está expandida pero la descripción aún se está cargando.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(20.dp))
                }
            }
        }
    } // Fin del Column principal clickeable
}



/*
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
    var moveDescription by remember { mutableStateOf<String?>(null) }

    val actualColorTexto = moveTypeName?.let { type ->
        if (esTipoColorOscuro(type)) { // Asegúrate que esTipoColorOscuro está definida
            Color.White
        } else {
            Color.Black // O tu Color CardBorder si lo prefieres para texto oscuro
        }
    } ?: MaterialTheme.colorScheme.onSurface // Un color por defecto si el tipo es nulo

    LaunchedEffect(key1 = moveSlot.move.url) {
        if (moveSlot.translatedName == null || moveTypeName == null || moveDescription == null) {
            isLoadingDetails = true
            try {
                val response = pokemonApiService.getMoveDetailsByUrl(moveSlot.move.url)

                if (response.isSuccessful) {
                    val moveDetails = response.body()

                    // Establecer nombre (si no está traducido ya en moveSlot)
                    if (moveSlot.translatedName == null) {
                        val spanishNameEntry = moveDetails?.names?.find { it.language.name == "es" }?.name
                        if (!spanishNameEntry.isNullOrBlank()) {
                            displayedMoveName = spanishNameEntry
                        }
                        // Si no, se queda el nombre por defecto (generalmente en inglés de la API o formateado)
                    }

                    moveTypeName = moveDetails?.moveType?.name
                    movePower = moveDetails?.power
                    movePp = moveDetails?.pp
                    moveAccuracy = moveDetails?.accuracy
                    moveDamageClassName = moveDetails?.damageClass?.name

                    // Extraer y establecer la descripción del movimiento EN ESPAÑOL
                    val spanishEffectEntry = moveDetails?.effectEntries?.find { it.language.name == "es" }

                    var foundDescription = spanishEffectEntry?.effect?.takeIf { it.isNotBlank() }
                        ?: spanishEffectEntry?.shortEffect?.takeIf { it.isNotBlank() }

                    if (foundDescription != null) {
                        // Reemplazar placeholders si es necesario (ejemplo con $effect_chance)
                        // Asegúrate de que `moveDetails.effectChance` existe en tu MoveDetailResponse si lo usas.
                        val effectChanceValue = moveDetails?.accuracy
                        if (effectChanceValue != null) {
                            foundDescription = foundDescription.replace("\$effect_chance", effectChanceValue.toString())
                        }

                        // Limpiar saltos de línea y otros caracteres
                        moveDescription = foundDescription.replace("\n", " ").replace("\u000c", " ")
                    } else {
                        // Si no se encontró descripción en español (ni 'effect' ni 'short_effect')
                        // podrías intentar con flavor_text_entries en español como último recurso
                        // o establecer un mensaje específico.
                        val spanishFlavorText = moveDetails?.flavorTextEntries
                            ?.filter { it.language.name == "es" }
                            // Quizás quieras una lógica para elegir la "mejor" flavor text si hay varias
                            // Por ejemplo, la de la versión de juego más reciente o una específica.
                            // Por ahora, tomaremos la primera que no esté vacía.
                            ?.firstOrNull { it.flavorText.isNotBlank() }
                            ?.flavorText

                        if (!spanishFlavorText.isNullOrBlank()) {
                            moveDescription = spanishFlavorText.replace("\n", " ").replace("\u000c", " ")
                        } else {
                            moveDescription = "Descripción en español no disponible." // Mensaje específico
                        }
                    }

                } else {
                    println("Error fetching move details: ${response.code()} for ${moveSlot.move.url}")
                    if (moveSlot.translatedName == null) displayedMoveName = moveSlot.move.name.replace("-", " ") + " (Error)"
                    moveDescription = "No se pudo cargar la descripción."
                }
            } catch (e: Exception) {
                println("Exception fetching move details: ${e.message} for ${moveSlot.move.url}")
                if (moveSlot.translatedName == null) displayedMoveName = moveSlot.move.name.replace("-", " ") + " (Excepción)"
                moveDescription = "Error al cargar la descripción."
            }
            isLoadingDetails = false
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
        getPokemonTypeColorSurface(type)
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
                Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(24.dp))
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
                    if (moveDescription != null && !isLoadingDetails) { // Mostrar solo si hay descripción y no está cargando
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = moveDescription!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = actualColorTexto.copy(alpha = 0.85f), // Un poco más tenue que el nombre
                            modifier = Modifier.padding(horizontal = 4.dp) // Algo de padding lateral
                        )
                    }
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
*/