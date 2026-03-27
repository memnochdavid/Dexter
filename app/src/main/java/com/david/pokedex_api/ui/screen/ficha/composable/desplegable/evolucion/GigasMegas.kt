package com.david.pokedex_api.ui.screen.ficha.composable.desplegable.evolucion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.david.pokedex_api.R
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.util.Lottie
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonSpecialFormsView(
    pokemonSpeciesUrl: String?, // URL de la especie del Pokémon actual
    pokemonApiService: PokeApiService, // Asegúrate que es tu interfaz de servicio actualizada
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    colorTexto: Color = MaterialTheme.colorScheme.onSurface,
    itemCardColor: Color = MaterialTheme.colorScheme.surface,
    onFormClick: (pokemonName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var specialForms by remember { mutableStateOf<List<SpecialForm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(pokemonSpeciesUrl) {
        if (pokemonSpeciesUrl == null) {
            isLoading = false
            error = "No species data available."
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        specialForms = emptyList() // Limpiar formas anteriores al recargar

        coroutineScope.launch {
            try {
                // CAMBIO 1: Usa el nuevo nombre de la función del servicio
                val speciesResponse = pokemonApiService.getPokemonSpeciesDetailsByUrl(pokemonSpeciesUrl)
                //                                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^

                if (speciesResponse.isSuccessful && speciesResponse.body() != null) {
                    val speciesDetail = speciesResponse.body()!!
                    val forms = mutableListOf<SpecialForm>()

                    // Asegúrate de que tu modelo PokemonSpeciesResponse tenga 'varieties'
                    speciesDetail.varieties.forEach { variety ->
                        if (!variety.isDefault) {
                            val formApiName = variety.pokemon.name
                            var displayName = ""
                            var isSpecialForm = false

                            if (formApiName.contains("-mega")) {
                                displayName = "Mega " + formApiName.substringBefore("-mega")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                if (formApiName.contains("-mega-x")) displayName += " X"
                                if (formApiName.contains("-mega-y")) displayName += " Y"
                                isSpecialForm = true
                            } else if (formApiName.contains("-gmax")) {
                                displayName = "Giga " + formApiName.substringBefore("-gmax")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                isSpecialForm = true
                            }

                            if (isSpecialForm) {
                                // CAMBIO 2: Usa la función estándar para obtener detalles del Pokémon por nombre
                                val formDetailsResponse = pokemonApiService.getPokemonDetails(formApiName)
                                //                                             ^^^^^^^^^^^^^^^^^

                                val sprite = if (formDetailsResponse.isSuccessful) {
                                    formDetailsResponse.body()?.sprites?.other?.officialArtwork?.frontDefault
                                        ?: formDetailsResponse.body()?.sprites?.frontDefault
                                } else {
                                    null
                                }
                                forms.add(SpecialForm(formApiName, displayName, sprite))
                            }
                        }
                    }
                    specialForms = forms
                } else {
                    error = "Failed to load species details: ${speciesResponse.message()}"
                }
            } catch (e: Exception) {
                error = "Error fetching special forms: ${e.localizedMessage}"
                e.printStackTrace() // Es bueno tener esto para depuración
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Lottie(rawResId = R.raw.pokeball, modifier = Modifier.size(30.dp))
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }
        return
    }
    if (specialForms.isEmpty()) {
        // No renderizar nada si no hay formas especiales, ni siquiera la Card.
        // Si quisieras mostrar un mensaje dentro de una tarjeta plegable aunque esté vacía,
        // moverías esta condición dentro de la Card y AnimatedVisibility.
        return
    }

    Card(
        modifier = modifier
            .fillMaxSize(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            // No es necesario .background(cardColor) aquí ya que se define en CardDefaults
        ) {
            Row( // Encabezado clickeable
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded } // Hace el título clickeable
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evoluciones Especiales",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorTexto
                )
//                Icon(
//                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
//                    contentDescription = if (isExpanded) "Plegar" else "Expandir",
//                    tint = colorTexto,
//                    modifier = Modifier.padding(start = 8.dp)
//                )
            }

            // Contenido plegable animado
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    specialForms.forEach { form ->
                        SpecialFormItemView(
                            specialForm = form,
                            backgroundColor = itemCardColor,
                            colorTexto = colorTexto,
                            onClick = { onFormClick(form.formName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialFormItemView(
    specialForm: SpecialForm,
    backgroundColor: Color,
    colorTexto: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(specialForm.spriteUrl ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = specialForm.displayName,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = specialForm.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = colorTexto,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }
    }
}

data class GigasMegasDescription(
    val pokeId: Int,
    val desc: String,
)

val descripcionesMegasGigas = listOf<GigasMegasDescription>(
    //megaevos
    GigasMegasDescription(10033, "Al megaevolucionar, le crece una flor rosa de seis pétalos en la frente y otra en la parte posterior. Para poder soportar el peso de esa inmensa flor, sus piernas y su cuerpo se vuelven más robustos. También le sale un rombo verde en la cara y dos triángulos del mismo color a los lados. En la espalda, sus hojas se hacen más oscuras, le crecen lianas y dos troncos, con tres hojas cada uno y la flor pasa a tener un único punto blanco en cada pétalo."),
    GigasMegasDescription(10034, "Al equipar a Charizard con una Charizardita X, podrá megaevolucionar a Mega-Charizard X. Este, a diferencia de Mega-Charizard Y, cambia de tipo al megaevolucionar. De tipo fuego/volador pasa a ser de tipo fuego/dragón. Su cuerpo pasa a ser de colores negros y azules, y sus llamas de la cola y la boca, más abrasadoras que nunca, arden con un tono azulado."),
    GigasMegasDescription(10035, "Mega-Charizard Y es el resultado de la megaevolución de Charizard al equiparlo con una Charizardita Y. Cuando se convierte en Mega-Charizard Y, los puntiagudos cuernos y larga cola de este Pokémon le confieren un aspecto imponente, y sus alas se hacen más grandes que nunca. Mega-Charizard Y no tiene rival cuando está en juego demostrar la destreza volando, pues puede alcanzar unas alturas increíbles."),
    GigasMegasDescription(10036, "Los dos reactores que lleva Blastoise en su caparazón se convierten en un único cañón descomunal, que cuenta con un alcance increíble, pudiendo acertar a objetivos que se encuentren a 10 km de distancia. Además se le añaden dos cañones más pequeños en los brazos. Su peso aumenta y sus características de ataque, defensa, ataque especial y defensa especial se fortalecen."),
    GigasMegasDescription(10037, "En este estado, le crece una prominente barba blanca. Sus muñequeras se vuelven más grandes, tomando la forma de grandes mangas. Su cabeza crece adquiriendo forma de estrella y en su frente aparece un órgano de color rojo (similar a un tercer ojo) con el que se dice que emite un fuerte poder psíquico. Ahora tiene 5 cucharas en vez de 2, que levitan y amplifican sus poderes psíquicos."),
    GigasMegasDescription(10038, "Sus brazos ahora son más grandes y están apoyados en el suelo, al igual que su cola, mientras que sus patas permanecen ocultas bajo el suelo. Le aparece un tercer ojo en la frente de color amarillo, que no parpadea y le permite ver otras dimensiones. Su altura es ligeramente menor y sus características de velocidad, defensa, ataque especial y defensa especial se fortalecen. "),
    GigasMegasDescription(10039, "La fuerza de Mega-Kangaskhan radica en la felicidad que siente una madre al ver crecer a sus hijos. Sin embargo, como la megaevolución hace crecer de golpe a la cría, causa preocupación a la madre. Como la cría adopta una naturaleza muy competitiva, a su madre le preocupa un poco su futuro y comienza a pensar en el inevitable momento de su separación."),
    GigasMegasDescription(10040, "Sus ojos pasan a ser de color amarillo y le salen unos élitros de colores amarillos y naranjas gracias a los cuales puede volar. Los grandes pinchos de sus cuernos crecen y se vuelven más puntiagudos. Además, los dedos de sus pies se vuelven blancos y le crecen unas aletas en los brazos."),
    GigasMegasDescription(10041, "La apariencia de Mega-Gyarados asemeja en alguna forma a su preevolución Magikarp, con la única diferencia que la cabeza sigue poseyendo el tamaño normal, propio de su evolución. Su vientre pasa del color crema al negro, y las escamas de color crema se vuelven rojas. Las pequeñas aletas blancas que hay por todo su cuerpo, así como la cola, aumentan de tamaño y pasan a ser amarillas, estando la mayor de ellas en la parte del lomo más cercana a la cabeza. Sus cuernos azules de la cabeza ahora son más grandes y de color negro, y sus bigotes se hacen más largos y pasan a ser del color del cuerpo. "),
    GigasMegasDescription(10042, "En esta forma su cuerpo pasa a ser de colores un poco más oscuros y aparecen rocas puntiagudas en su cabeza, torso, alas, cola y lomo. La megaevolución hace que el cuerpo de Aerodactyl empiece a endurecerse como la piedra. Sus ojos se vuelven verdes. Aumentan tanto su peso como su altura, y se fortalecen sus características de velocidad, ataque, defensa, ataque especial y defensa especial."),
    GigasMegasDescription(10043, "En este estado sus brazos y piernas se vuelven más musculosos. Su cola se acorta y se vuelve más rígida y aparecen dos estructuras rosadas que cubren sus hombros, similares a la capucha de una sudadera, relacionada con su estilo de lucha callejera. Sus ojos se vuelven azules y sus orejas más puntiagudas. Mega-Mewtwo X podrá atacar con una potencia espectacular, tanto con ataques físicos como especiales."),
    GigasMegasDescription(10044, "Tras su transformación, Mewtwo pierde la cola, pero adquiere un apéndice que se sitúa en su cabeza. Se vuelve más pequeño y sus dedos de las manos y los pies cambian de color blanco a morado. Sus orejas se vuelven más puntiagudas y sus ojos se vuelven rojos. Con su cuerpo aerodinámico parece estar flotando en el aire todo el tiempo. Su peso disminuye considerablemente y su altura se reduce un poco. Aumentan su velocidad, ataque, ataque especial y defensa especial, y disminuye su defensa."),
    GigasMegasDescription(10045, "En este estado su cabeza y su cola se cubren de pelo. En la cabeza le crece una larga melena blanca que remata en la esfera que conserva en su forma original, y en la cola le crece un pelaje blanco que la cubre completamente en el que le crecen nuevas esferas coloradas. Sus cuernos se deforman, como si se retorciesen sobre sí mismos. Las uñas de sus pies se vuelven de color negro. El color de su piel se vuelve un poco más claro. También cambia un poco la forma del pelo blanco de su pecho."),
    GigasMegasDescription(10046, "Al megaevolucionar, Scizor adquiere un aspecto más robótico. Sus tenazas se vuelven más voluminosas todavía, con púas afiladas capaces de atravesar el hormigón como si fuera mantequilla. Si un oponente queda atrapado en las tenazas de Mega-Scizor, ya no podrá liberarse. Sus piernas se vuelven puntiagudas y adquiere tonos negros en algunas partes de su cuerpo. Además, sus ojos se vuelven azules."),
    GigasMegasDescription(10047, "Al megaevolucionar, aumenta su altura, peso, ataque, defensa y defensa especial, pero disminuye su velocidad. En esta forma sí se asemeja a un escarabajo hércules macho., teniendo ahora dos cuernos: uno grande encima de su cabeza y otro entre sus ojos, pareciéndose a una nariz, y sus antenas se vuelven alargadas y estilizadas. Sus brazos se tornan más robustos y adquiere una tercera garra en cada uno"),
    GigasMegasDescription(10048, "En este estado, su habilidad pasa a ser poder solar, su defensa, defensa especial, ataque especial y velocidad se incrementan y su peso y altura aumentan. Le crecen prominentes colmillos y la mayoría de su cuerpo está cubierto por huesos. Su cola pasa a ser de dos puntas en relación al bidente de Hades, dios griego del Inframundo, así como dueño de Cerbero, en el que también está basado."),
    GigasMegasDescription(10049, "Mega-Tyranitar adquiere púas aun más grandes alrededor de su espalda y cola. El color de su estómago pasa a ser de azul grisáceo a rojo como el de Larvitar y su ataque, defensa, defensa especial y velocidad aumentan."),
    GigasMegasDescription(10050, "Lo más destacado de los cambios físicos que experimenta Mega-Blaziken son unas largas llamaradas saliendo de sus muñecas. El pelaje claro de su cabeza que caía hacia sus hombros ahora crece para arriba en punta; de esta manera, despejándole la espalda y ese mismo pelaje que le rodeaba el cuello ahora es más poblado por debajo de los brazos. Los picos que salían de su nariz se han transformado en un cuerno que crece desde su frente. El tono amarillo del plumaje de sus patas pasa a ser rojo, como el de su estómago, y la parte superior de las patas adquiere un color negro al igual que la parte alta de su pecho. Mantiene su pata derecha flexionada y levantada."),
    GigasMegasDescription(10051, "En este estado, su apariencia cambia siendo que parece ser que su cabello cambia en cuanto a su estilo siendo más corto y ondulado y ahora parece tener una especie de vestido de reina o de gala. También cambia la protuberancia rosa o cuerno de su torso, pues donde antes tenía una, ahora tiene dos, convergentes hacia el centro de su pecho. Su ataque, ataque especial, defensa especial y velocidad se incrementan."),
    GigasMegasDescription(10052, "Mega-Mawile tiene ahora 2 bocas en su cabeza en vez de una, que cambian ligeramente de forma. El color de su cuerpo de cintura para abajo cambia a rosa, igual que el extremo de sus brazos. Los mechones de pelo de su cabeza se hacen más largos y la punta termina en dos picos asimilando un listón. En esta forma tiene un carácter muy agresivo. Atrapa a sus víctimas con sus dos juegos de mandíbulas y las despedaza. Sus mandíbulas se agitan con violencia como si tuvieran voluntad propia. Pueden hacer pedazos una roca de un solo mordisco."),
    GigasMegasDescription(10053, "Al megaevolucionar, Aggron pierde el tipo roca, convirtiéndose en un Pokémon de tipo acero puro. Además, aumentan su altura y su peso y adquiere la habilidad filtro. Su aspecto se vuelve más robusto y le crecen púas en ambos brazos. Su ataque y defensa aumentan considerablemente."),
    GigasMegasDescription(10054, "En este estado todas sus características aumentan, sobre todo su ataque y velocidad. En cuanto al físico, de su cintura caen los adornos de telas, que parecen caer de su cinto, característico de los profesionales de ciertas disciplinas de baile. Sus ojos se vuelven azules, aparecen dos muñequeras doradas y el accesorio que cubre su cabeza se torna en su mayoría de un color blanquecino. Rodeando su cabeza destacan cuatro brazos producto de sus poderes psíquicos, simbolizando la fuerza mental de este Pokémon."),
    GigasMegasDescription(10055, "En este estado, su habilidad pasa a ser intimidación y su peso y altura aumentan, también incrementa su defensa, defensa especial, ataque especial y velocidad."),
    GigasMegasDescription(10056, "El color grisáceo de su cuerpo se oscurece ligeramente aportando un toque más sombrío a su figura. De su ojo derecho sale una cremallera que discurre por su cabeza y se extiende por su coleta, que ahora se encuentra alzada. Sus extremidades superiores han agrandado y una cremallera las abre, quedando al descubierto tres garras de tonalidad fucsia en cada mano. En la mitad inferior de su cuerpo sucede algo similar, sus dos patas salen por el hueco de la cremallera."),
    GigasMegasDescription(10057, "A Mega-Absol se le eriza el pelaje de su cuello de tal forma que parece que tuviera un par de alas. Estas emiten una potente aura que arrolla al rival, aunque al no tratarse de verdaderas alas, no puede volar. También se le eriza el pelo de sus patas. El cuerno de su cabeza y su cola cambian de forma, y le crece el pelo de su cabeza, cayendo por su frente tapándole un ojo. Al ser de naturaleza pacífica, odia adoptar esta forma tan apta para los combates. La energía de la megaevolución lo rodea formando un aura abrumadora. Quienes carecen de fortaleza espiritual mueren del susto al verlo."),
    GigasMegasDescription(10058, "El color general de su cuerpo pasa a ser un azul más claro. En su cara, la piel roja asciende hasta la parte de las sienes. Con respecto a sus brazos, los pinchos pasan a situarse en la parte inferior, y las cuchillas antes unidas a sus garras se han transformado en dos guadañas que alternan los colores rojo y azul. En sus rodillas crece un tercer par de pinchos también rojos, y en su abdomen, la parte amarilla adquiere una forma de rombo. La forma circular de su cola es ahora más redondeada."),
    GigasMegasDescription(10059, "Los cambios físicos que experimenta Mega-Lucario son un aumento del pelaje amarillo de su pecho y espalda, que ahora también envuelve su cola. Los sensores de su cabeza se separan y deforman, adquiriendo un color rojizo en las puntas. La diferencia de grosor que había entre la parte superior e inferior de las piernas desaparece, mezclándose el color azul y negro a lo largo de las piernas y brazos, que también adquieren un color rojizo en las extremidades. Le crecen nuevos pinchos en el reverso de las manos y también en los pies, además en los hombros, que también crecen en tamaño."),
    GigasMegasDescription(10060, "Su aspecto cambia ligeramente: sus brazos aparecen anclados al sustrato, los pies están cubiertos por una mayor cantidad de nieve, y la diferencia principal es una multitud de nuevas capas heladas que rodean su cabeza y cuerpo. De su tronco sobresalen dos columnas de hielo protegidas por nieve."),
    GigasMegasDescription(10090, "Los aguijones venenosos de su abdomen y patas delanteras son ahora de mayor tamaño. Asimismo, de sus patas traseras salen ahora dos aguijones. El veneno que segrega por las cuatro extremidades actúa al instante, mientras que el del abdomen es de efecto retardado. Usa el primero para evitar que sus enemigos huyan y el segundo para asestar el golpe de gracia. Ahora tiene seis alas lo que le permite alcanzar unas velocidades mucho mayores."),
    GigasMegasDescription(10073, "Los nutrientes y pigmentos presentes en las plumas de las alas y cola de Mega-Pidgeot cambian, por lo que su coloración también varía. Su cuerpo crece y se hace más fuerte, sobre todo sus alas. La larga cresta de la cabeza hace las veces de una antena muy sensible y le proporciona estabilidad en el vuelo, sus ojos se vuelven color rojo, su cola pasa a ser roja y las puntas terminan en azul. Las ráfagas de viento que puede levantar con sus alas robustas y bien desarrolladas tienen la potencia necesaria para destrozar árboles inmensos."),
    GigasMegasDescription(10072, "La energía liberada por la megaevolución cristaliza las células de su cuerpo, volviéndolas más fuertes que cualquier mineral y capaces de soportar temperaturas de todo tipo. Se mueve con increíble lentitud, pero su cuerpo fortalecido resiste bien el daño y ataca a sus adversarios sin inmutarse. Sus ojos cambian a color azul, sus pinchos son de diamante y aparecen unos aros azules en su cuerpo. También aparecen trozos de metal flotando alrededor de su cabeza."),
    GigasMegasDescription(10064, "Mantiene sus tipos y su habilidad cambia a nado rápido. Su color azul se vuelve más pálido y gana en peso y altura. Sus aletas se vuelven más largas y la parte del final se vuelve recta. El número de baldosas naranjas que posee en sus extremidades aumenta y cambian ligeramente de color y forma. Las prolongaciones de sus mejillas se alargan y ahora son tres en vez de dos. Los dedos de sus extremidades, que son ahora más fuertes, adquieren un color negro. Su físico también aumenta notablemente. Todas sus características aumentan excepto sus puntos de salud."),
    GigasMegasDescription(10065, "Adquiere el tipo dragón y su habilidad cambia a pararrayos. Al megaevolucionar aumenta en peso y tamaño y también sus características de ataque, defensa, ataque especial y velocidad. Su cola ahora acaba en punta, con el final rojo, pudiendo cortar un trozo de esta para dispararlo contra su oponente como si se tratara de un misil y ésta volverá a crecer. Las semillas de su espalda se multiplican, cambiando de color conforme nos acercamos al final. Las hojas en forma de daga de los brazos se vuelven rojas en la punta, y más estilizadas y afiladas. Su cresta se vuelve más aguileña, y parece mordida. Gracias a la potencia de sus ágiles patas, Mega-Sceptile puede lanzarse sobre su oponente en un abrir y cerrar de ojos."),
    GigasMegasDescription(10066, "Cuando Sableye megaevoluciona, aumentan todas sus características a excepción de su velocidad, que disminuye debido al peso de la enorme joya. Su peso aumenta considerablemente debido a esta, aunque su altura se mantiene igual. Su habilidad pasa a ser espejo mágico."),
    GigasMegasDescription(10070, "Los colmillos de Mega-Sharpedo se han transformado y los cuernos que le salen del morro pueden volver a crecer una y otra vez. Las marcas amarillas de su cuerpo son cicatrices causadas durante los combates. Gracias a su propulsión explosiva, puede pasar de 0 a 200 km/h en un instante."),
    GigasMegasDescription(10087, "Las jorobas de su lomo se han convertido en un único volcán de grandes dimensiones y en constante actividad porque el magma en ebullición de su cuerpo emana sin cesar. Este Pokémon, que detesta profundamente el agua o cualquier ápice de humedad, tiene una personalidad explosiva y muy malas pulgas. De hecho, si el volcán de su lomo está en constante erupción, no es para otra cosa que para intimidar a sus rivales. Tiene la letra M en su frente y los mechones que tenía en la parte superior de la cabeza se vuelven más grandes, en especial el central. Sus pezuñas se vuelven más oscuras y su pelaje se vuelve más denso."),
    GigasMegasDescription(10067, "El cuerpo de Mega-Altaria está envuelto en unas plumas únicas que ha desarrollado como medida de protección y que destellan con un brillo iridiscente. Ahora cubren más parte de su cuerpo, que por su parte ahora es de un azul más claro. Al cantar, su voz es aún más hermosa si cabe. Este Pokémon es muy sociable y nada tímido."),
    GigasMegasDescription(10089, "La energía de la megaevolución de Salamence se concentra en sus alas. Las dos se funden en una sola mucho más grande con forma de media luna. Cuando vuela a gran velocidad, el ala hace las veces de hoja afilada rebanando todo lo que se interponga a su paso. Sus patas se hacen más pequeñas y se esconden en su armadura, los cuernos de su cabeza aumentan ligeramente de tamaño, sus ojos se vuelven amarillos, y su altura y peso aumentan un poco. Esta forma más aerodinámica y especializada en el vuelo le permite incrementar sustancialmente su velocidad, y la coraza que parece cubrir su torso le dota de una defensa muy reforzada respecto a su forma original."),
    GigasMegasDescription(10076, "Su tipo no cambia, aumenta casi el doble de peso y el doble de altura y su habilidad pasa a ser garra dura. Al megaevolucionar su cuerpo cambia notablemente, apareciendo en él más patas con grandes garras, la X de su cabeza se estiliza y cambia de color, además le aparece una púa en la parte inferior de la cabeza. Aumentan todas sus características menos los PS."),
    GigasMegasDescription(10062, "Los cambios físicos que experimenta Mega-Latias son similares a los de Mega-Latios. Su aspecto pasa a ser más robusto y adquiere un tono violeta (mezcla del rojo de Latias y el azul de Latios) y blanco en la piel. Además, las orejas pasan a ser más puntiagudas y las alas y los brazos parecen fusionarse en un solo par de extremidades. Mega-Latias junto a su compañero Mega-Latios es uno los Pokémon más veloces que existen. Ambos pueden llegar a adelantar a aviones a reacción y alcanzar una velocidad de Mach 4. Aunque es de menor tamaño que Mega-Latios, es más ágil que él y puede realizar giros más pronunciados. Sus ojos son grandes y de un color amarillo claro."),
    GigasMegasDescription(10063, "Los cambios físicos que experimenta Mega-Latios son idénticos a los de Mega-Latias, con la única diferencia que Mega-Latios tiene los ojos color rojo y Mega-Latias color amarillo. Su aspecto pasa a ser más robusto y adquiere un tono violeta y blanco en la piel. Además, las orejas pasan a ser más puntiagudas y las alas y los brazos parecen fusionarse en un solo par de extremidades. Sus ojos son grandes y de un color rojo. Se dice que Mega-Latios es uno de los Pokémon más rápidos que existen en el mundo, pudiendo alcanzar velocidades de Mach 4."),
    GigasMegasDescription(10079, "Se dice que este Pokémon alcanza este estado de un modo único y distinto a los demás debido a su singular biología. En cuanto a su forma física, las líneas amarillas del tronco sobresalen convirtiéndose en una especie de alas. Su cola se divide en dos, casi tocando la punta; los cuernos inferiores de su cabeza se expanden y retuercen hacia adelante en forma de tenaza, y de ellos sobresalen las marcas amarillas dando impresión de ser unas líneas de fuego que se alargan como cintas resplandecientes. Además, su cráneo adquiere una forma más afilada y fina, y en sus ojos aparece una marca negra de aspecto tribal."),
    GigasMegasDescription(10088, "Adquiere el tipo lucha, convirtiéndose en tipo normal/lucha, y su habilidad cambia a intrépido. Su color se vuelve más oscuro y sus enormes orejas blancas se convierten en unas más finas y marrones debido a la pérdida de pelaje blanco. Sus cejas se hacen más grandes y se juntan en forma de X. Sus patas también se hacen más oscuras. Al megaevolucionar, su altura aumenta ligeramente, y su peso disminuye. Su ataque y velocidad aumentan enormemente."),
    GigasMegasDescription(10069, "El cuerpo de Mega-Audino emite un pulso calmante que reduce la hostilidad en los demás y relaja a cualquiera que se le acerque. Además, cualquier criatura que entre en contacto con su segundo par de antenas caerá en un profundo sueño."),
    GigasMegasDescription(10075, "El diamante de su cabeza ahora tiene forma de corazón y posee dos cintas gigantes saliendo del mismo. Los anteriores bloques de cristal adheridos a la joya, ahora se asemejan a dos gotas cristalizadas. La roca sobre la que se apoyaba está cubierta por su falda, ahora más grande. Presenta un mayor número de diamantes, y de los cinco principales salen (al igual que de las cintas y de los diamantes que cuelgan de su cabeza) cristales que levitan. Al desaparecer las impurezas de su superficie, brilla con tal esplendor que es imposible mirarlo directamente cuando refleja la luz. El diamante de su cabeza pesa más de 2000 quilates. Debido a su noble beldad, se le conoce como La Princesa Real Rosada."),
    GigasMegasDescription(10068, "Al megaevolucionar, la cresta de su casco crece y se vuelve de color celeste, su torso y abdomen se vuelven blancos y las púas rojas que lo atraviesa se reducen en tamaño. A Gallade le crecen unas placas en los brazos, y las protuberancias, que se asemejan a un casco y una capa, le aportan cierto aire caballeresco. Cuando la situación lo requiere, Mega-Gallade puede usar su poder psíquico para utilizar simultáneamente sus dos brazos a modo de espada, con ello sus espadas sueltan un aura roja haciendo el ataque más poderoso."),
    GigasMegasDescription(10071, "Al megaevolucionar, este Pokémon acumula energía en el Shellder que tiene enganchado a la cola creciendo hasta cubrir casi todo el cuerpo de Slowbro a excepción de la cabeza, las patas delanteras y la cola, manteniéndose en pie con la punta de esta última. También la dureza de su caparazón aumenta y se vuelve una coraza increíblemente dura consiguiendo que su defensa y ataque especial aumenten considerablemente."),
    GigasMegasDescription(10074, "Al megaevolucionar, el exceso de energía producido por la megaevolución le sale a borbotones por la boca, destruyéndole la mandíbula con una explosión. Mega-Glalie no puede cerrar la boca y el vaho gélido que desprende se vuelve aún más intenso. Arroja a sus enemigos a su boca abierta, expulsando una ráfaga de aire frío que los congela en el acto. También le sale un tercer cuerno de hielo en la frente."),
    //gigamax
    GigasMegasDescription(10196, "Las llamas del interior de su cuerpo se han vuelto más potentes, y rezuma fuego por la boca y por los cuernos, además de por la cola. La intensidad de las llamas parece aumentar cuando ruge. Sus llamas se han imbuido de energía Gigamax y han formado alas en su lomo. Estas alas son más calientes que el magma, y Charizard puede lanzarlas para calcinar a sus oponentes."),
    GigasMegasDescription(10198, "Su característica más destacable son sus cegadoras alas, que han crecido hasta alcanzar proporciones gigantescas. La luz de sus alas proviene de sus escamas, que pueden causar parálisis, envenenamiento o sueño a sus oponentes y constituyen el arma más efectiva de Butterfree. Butterfree Gigamax es capaz de levantar un camión de 10 toneladas solo con el viento provocado por el aleteo de sus gigantescas alas, que también le permite esparcir escamas venenosas por el terreno de combate. Acercarse a este Pokémon puede resultar muy peligroso."),
    GigasMegasDescription(10199, "Pikachu Gigamax se ha vuelto tan poderoso que puede producir tanta energía como una central eléctrica. Pero usar toda esa energía para abastecer los hogares de Galar no parece viable, pues Pikachu solo puede gigamaxizarse durante un tiempo limitado. Almacena en la cola toda la electricidad que genera en las bolsas de las mejillas. En los combates golpea a sus rivales con la cola y les propicia una descarga eléctrica con la potencia de un rayo."),
    GigasMegasDescription(10200, "El fenómeno Gigamax ha provocado que el cuerpo de Meowth se estire hasta alcanzar un tamaño sorprendente, y la moneda que lleva en la frente también se ha vuelto gigantesca. Esta moneda puede descargar un torrente de energía Gigamax, que llena el área que la rodea de un poderoso brillo dorado. A Meowth le encantan los objetos brillantes y, cuando ve rascacielos de cristal, no es capaz de resistir la tentación de afilar sus garras en ellos. Esto provoca grandes destrozos en los edificios, tanto en las paredes como en las ventanas."),
    GigasMegasDescription(10201, "Mediante el Fenómeno Gigamax, algunos Machamp son capaces de transformarse durante los combates en Machamp Gigamax. Todos los movimientos de tipo lucha usados por Machamp Gigamax se transformarán en el movimiento gigapuñición, que causa daño al oponente y aumenta la probabilidad de golpe crítico del bando aliado."),
    GigasMegasDescription(10202, "Mediante el fenómeno Gigamax, los Gengar con factor Gigamax son capaces de transformarse durante los combates en Gengar Gigamax. Esta forma está inspirada en un túnel del terror. Todos los movimientos de tipo fantasma usados por Gengar Gigamax se transforman en el movimiento gigaaparición, que causa daño al oponente y lo deja atrapado."),
    GigasMegasDescription(10203, "Mediante el fenómeno Gigamax, algunos Kingler especiales son capaces de transformarse durante los combates en Kingler Gigamax. Sus pinzas se hacen más grandes, adquieren dentadura propia y sus brazos y piernas se hacen más largos. Además, sus ojos se vuelven de color rojo, y le sale una barba hecha de espuma."),
    GigasMegasDescription(10204, "Mediante el fenómeno Gigamax, algunos Lapras especiales son capaces de transformarse durante los combates en Lapras Gigamax. Todos los movimientos de tipo hielo usados por Lapras Gigamax se transformarán en el movimiento gigamelodía, que causa daño al oponente y activa velo aurora durante 5 turnos."),
    GigasMegasDescription(10205, "Eevee Gigamax se ha vuelto mucho más revoltoso que de costumbre, por lo que intentará jugar con todo rival con el que se tope. Pero, debido a su gigantesco tamaño, en muchas ocasiones lo único que consigue es aplastarlo. Gracias a la energía Gigamax, el pelaje que le rodea el cuello es ahora mucho más abundante y sedoso. Cualquier enemigo que se vea envuelto en él quedará prendado de Eevee y perderá las ganas de combatir."),
    GigasMegasDescription(10206, "Las semillas de bayas y las piedritas que se habían enredado en el pelo de su tripa han crecido hasta dimensiones inimaginables gracias al poder Gigamax. Esta forma gigantesca de Snorlax presenta la majestuosidad de una gran montaña, con árbol en la tripa incluido."),
    GigasMegasDescription(10207, "Mediante el fenómeno Gigamax, los Garbodor con factor Gigamax son capaces de transformarse durante los combates en Garbodor Gigamax. Debido a la energía del fenómeno Gigamax, sus emisiones tóxicas se han solidificado y han adoptado la forma de juguetes abandonados. Al entrar en contacto con el gas tóxico que emite por la boca y los dedos, el veneno penetra hasta la médula."),
    GigasMegasDescription(10208, "Mediante el fenómeno Gigamax, los Melmetal con factor Gigamax son capaces de transformarse durante los combates en Melmetal Gigamax. Melmetal Gigamax puede usar su elástico cuerpo de metal líquido para asestar puñetazos a sus rivales por muy lejos que estén. Y, si junta las tuercas de sus brazos, forma unos enormes puños con los que es capaz de dar puñetazos todavía más fuertes. También es capaz de generar electricidad gracias al movimiento del metal líquido que forma su cuerpo. Esta electricidad carga las partículas que después salen despedidas por el agujero de la tuerca del abdomen del Pokémon. El rayo eléctrico resultante es tan poderoso que podría desintegrar una colina entera."),
    GigasMegasDescription(10212, "Mediante el fenómeno Gigamax, los Corviknight con factor Gigamax son capaces de transformarse durante los combates en Corviknight Gigamax. Corviknight Gigamax cuenta con una armadura reforzada, por lo que los ataques de los Pokémon que están en tierra apenas le afectan. Esta armadura se separa en sus alas, convirtiéndose en plumas, llamadas ornitofilos, que pueden flotar por sí solas, y Corviknight las lanza para cortar a sus oponentes. Sus alas gigantescas le permiten planear en el aire mientras combate gracias a las corrientes que crea la energía Gigamax."),
    GigasMegasDescription(10213, "Mediante el fenómeno Gigamax, los Orbeetle con factor Gigamax son capaces de transformarse durante los combates en Orbeetle Gigamax. Su forma Gigamax parece estar fuertemente inspirada en un OVNI, también con rasgos mecanizados aún más pronunciados que su forma previa. Su cerebro es mucho más grande y debajo de él se pueden apreciar cinco orbes amarillos y en el centro a un orbe celeste. En esta fase sus alas desaparecen y ahora el Pokémon solo se desplaza levitando, similar a un platillo volante. Si despliega su máximo poder, Orbeetle Gigamax es capaz de controlar la mente de todos cuantos se hallen a su alrededor."),
    GigasMegasDescription(10214, "Mediante el fenómeno Gigamax, los Drednaw con factor Gigamax son capaces de transformarse durante los combates en Drednaw Gigamax. El fenómeno Gigamax le da a Drednaw un aspecto mucho más robusto y le permite erguirse sobre dos patas, pudiendo dejarse caer y aplastar a sus rivales con su cuerpo. Drednaw Gigamax oculta su cabeza dentro del caparazón, pero es capaz de sacarla a toda velocidad cuando ataca. Tiene gran fuerza en sus mandíbulas, que son capaces de destrozar enormes torres metálicas de un mordisco. El cuerno de su cabeza ahora es más prominente y puntiagudo, y con la fuerza de sus mandíbulas y su cuello le permite agujerear una montaña rocosa de un solo golpe."),
    GigasMegasDescription(10215, "Mediante el fenómeno Gigamax, los Coalossal con factor Gigamax son capaces de transformarse durante los combates en Coalossal Gigamax, con lo cual su aspecto se torna mucho más grande y robusto y la montaña de carbón en su lomo se expande. Ahora su cuerpo en esta forma tiene una temperatura de 2000 °C. Se dice que hace tiempo salvó incontables vidas de una cruenta ola de frío usando su cuerpo a modo de estufa gigante."),
    GigasMegasDescription(10216, "El néctar que cubre su cuerpo es tan grueso que le permite absorber más daño de lo normal y puede disparar suficiente de este como para ahogar a sus oponentes en él. Cuando Flapple estira su cuello, de su néctar se desprende un aroma excesivamente dulce capaz de desmayar a otros Pokémon simplemente con olerlo."),
    GigasMegasDescription(10218, "Mediante el fenómeno Gigamax, los Sandaconda con factor Gigamax son capaces de transformarse durante los combates en Sandaconda Gigamax. La arena que gira vertiginosamente en torno a su cuerpo posee suficiente capacidad destructiva para reducir a escombros un rascacielos. Además de su cuerpo, su bolsa de arena también se ha agigantado. El torbellino arenoso que lo rodea supera el millón de toneladas."),
    GigasMegasDescription(10219, "Mediante el fenómeno Gigamax, los Toxtricity con factor Gigamax son capaces de transformarse durante los combates en Toxtricity Gigamax. En su forma Gigamax, tiene lo que parece ser una torre eléctrica en su espalda junto con dos aislantes eléctricos. En esta forma su propio veneno le afecta al cerebro y le hace perder el control. Cuando esto ocurre, transpira un sudor tóxico que contamina el terreno a su alrededor. También usa la electricidad que le sobra como arma, ya que sus reservas energéticas superan incluso la carga eléctrica de un nubarrón de tormenta."),
    GigasMegasDescription(10228, "Mediante el fenómeno Gigamax, los Toxtricity con factor Gigamax son capaces de transformarse durante los combates en Toxtricity Gigamax. En su forma Gigamax, tiene lo que parece ser una torre eléctrica en su espalda junto con dos aislantes eléctricos. En esta forma su propio veneno le afecta al cerebro y le hace perder el control. Cuando esto ocurre, transpira un sudor tóxico que contamina el terreno a su alrededor. También usa la electricidad que le sobra como arma, ya que sus reservas energéticas superan incluso la carga eléctrica de un nubarrón de tormenta."),
    GigasMegasDescription(10220, "Mediante el fenómeno Gigamax, los Centiskorch con factor Gigamax son capaces de transformarse durante los combates en Centiskorch Gigamax. Esta forma parece estar basada en el Ōmukade, un gigantesco ciempiés de origen japonés y con similitudes a un dragón chino. Al estar en esta forma, su cuerpo genera tanto calor que es capaz de subir su temperatura a más de 1.000 ºC, y al expulsar aire caliente es capaz de alterar el clima y crear poderosas tormentas. A su vez, la gran cantidad de patas que obtiene al adoptar esta forma le permite poseer una gran velocidad, a pesar de su tamaño."),
    GigasMegasDescription(10221, "Mediante el fenómeno Gigamax, los Hatterene con factor Gigamax son capaces de transformarse durante los combates en Hatterene Gigamax. En esta forma, además de adquirir un tamaño gigantesco, el tentáculo de su sombrero se divide en tres y el pelo le crece para formar una especie de cámara alrededor de su verdadero cuerpo. Sus ojos se tornan de un color azul y pasan a estar iluminados. Hatterene Gigamax puede percibir los sentimientos de cualquier ser vivo en un radio de hasta 50 km. Si detecta hostilidad en ese radio inicia la ofensiva antes de ser atacado. De sus tentáculos brotan incesantemente rayos láser similares a relámpagos. Debido a su comportamiento tan hostil se lo conoce también como la Divinidad Iracunda."),
    GigasMegasDescription(10222, "Mediante el fenómeno Gigamax, los Grimmsnarl con factor Gigamax son capaces de transformarse durante los combates en Grimmsnarl Gigamax. Debido al fenómeno Gigamax, le crece el pelo de forma descomunal. El pelo de sus piernas se transforma y da aún más fuerza a sus patadas, capaces de abrir grandes agujeros en la tierra. Puede superar de un salto los rascacielos más altos del mundo."),
    GigasMegasDescription(10223, "En su forma Gigamax, la nata que segrega se solidifica al recibir impactos, haciéndose más consistente cuanto más potentes sean los impactos, adquiriendo así una mayor resistencia a ataques físicos. De hecho, los adornos que cubren su cuerpo son duros como el diamante, protegiéndole así de la mayoría de los ataques. Alcremie ataca lanzando proyectiles hipercalóricos. Si un Pokémon toca la nata de esos proyectiles, se volverá eufórico y enérgico, pero acabará presa de un estado de confusión total."),
    GigasMegasDescription(10224, "Mediante el fenómeno Gigamax, los Copperajah con factor Gigamax son capaces de transformarse durante los combates en Copperajah Gigamax. Cuando Copperajah se gigamaxiza, se vuelve bípedo, asemejándose a un tótem de un elefante, y ve su trompa agigantada. Con esta prolongación de su cuerpo puede derrumbar un rascacielos de un solo golpe y, si proyecta la energía que hay acumulada en ella, cambiar la topografía destruyendo montañas."),
    GigasMegasDescription(10225, "Mediante el fenómeno Gigamax, los Duraludon con factor Gigamax son capaces de transformarse durante los combates en Duraludon Gigamax. Al gigamaxizarse, Duraludon se vuelve tan grande como un rascacielos. Desborda energía, por lo que algunas partes de su cuerpo son luminiscentes. Su estructura celular es tan sólida que resiste perfectamente un terremoto."),
    GigasMegasDescription(10195, "Mediante el fenómeno Gigamax, algunos Venusaur especiales (obtenidos en el pase de expansión) son capaces de transformarse durante los combates en Venusaur Gigamax. Al transformarse, la flor en el lomo de Venusaur crece a tal punto que cubre la gran parte de todo su cuerpo. Debido a esto, las hojas de Venusaur son empujadas hacia abajo. También el bulbo que tiene en su espalda adquiere un mayor tamaño que en su forma habitual. La esclerótica de los ojos se vuelve verde y obtiene unas pequeñas manchas verdes en el lomo."),
    GigasMegasDescription(10197, "Al momento de Gigamaxizarse, los cañones en el caparazón de Blastoise desaparecen, y adoptan un sistema de armamento en su espalda, teniendo varias armas con las cuales puede atacar. También, los colores en su piel cambian, ademas de tener pequeños triángulos en la parte de sus pies y ojos color verde."),
    GigasMegasDescription(10209, "El tocón que usaba como tambor ha crecido tanto que ahora parece un pequeño bosque. Rillaboom Gigamax se ha fundido con su batería forestal y se ha vuelto mucho más poderoso. Controla sus dos baquetas como si fueran sus propias extremidades y con ellas demuestra sus increíbles dotes como percusionista. Cuando está tocando la batería, Rillaboom Gigamax se concentra por completo, inspirado por el poder que de ella emana. Su sonido puede cruzar océanos y llegar a aquellos que viven en otras regiones. Se dice que quienes lo escuchan comienzan a bailar descontrolados, siguiendo un ritmo que surge de su interior."),
    GigasMegasDescription(10210, "La energía Gigamax ha hecho que el balón de fuego haya crecido hasta alcanzar un tamaño gigantesco. Poniendo en práctica su pulida técnica, Cinderace Gigamax le da una patada al balón haciendo que su espíritu luchador lo posea y persiga al oponente como si tuviera vida propia. Cinderace Gigamax posee una fuerza inmensa en sus piernas. De hecho, se dice que son las más fuertes de entre todos los Pokémon. Es capaz de propinar movimientos tan poderosos con sus patadas que dejará atónito a cualquier Pokémon Dinamax, incluso sin su balón ígneo gigante."),
    GigasMegasDescription(10211, "Se dice que puede llegar a superar los 40 metros. Cuando alcanza el estado Gigamax, la cola de Inteleon se vuelve mucho más larga y rígida, por lo que le sirve de pilar sobre el que subirse para tomar su posición de combate. Este Pokémon tiene una mente muy avispada y es capaz de calcular los efectos exactos que tendrá la gravedad en la trayectoria de sus proyectiles, lo que lo convierte en un francotirador magnífico. Ahora, las membranas nictitantes de sus ojos no solo son capaces de medir la distancia con el objetivo, también pueden detectar la temperatura, la presión del aire y el grado de humedad. Inteleon posee una puntería extraordinaria. Para este Pokémon, acertar a una baya rodando a 15 km de distancia es pan comido."),
    GigasMegasDescription(10226, "Da la sensación de que esta forma de Urshifu va a comenzar a rugir con toda su furia de un momento a otro. Dicen de él que es la encarnación de la rabia y también un emisario divino que hace desaparecer las fuerzas del mal con su furia. En esta forma su pelaje adquiere un tono rojizo."),
    GigasMegasDescription(10227, "Cuando adopta esta forma, Urshifu permanece tranquilo, con una expresión de calma. Hay quien dice que, al enfrentarse a este Pokémon, se sienten intranquilos, como si lo más profundo de su alma hubiese quedado al descubierto. Otros, sin embargo, aseguran haber sentido calma, como si los malos espíritus que los atormentaban se hubieran desvanecido. En esta forma su pelaje adquiere un tono azulado."),
)
//https://www.wikidex.net/wiki/Megaevoluci%C3%B3n