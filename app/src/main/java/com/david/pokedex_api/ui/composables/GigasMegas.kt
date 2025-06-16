package com.david.pokedex_api.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.david.pokedex_api.api.model.SpecialForm
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.theme.CardBorder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonSpecialFormsView(
    pokemonSpeciesUrl: String?, // URL de la especie del Pokémon actual
    pokemonName: String, // Nombre del Pokémon base para el título
    pokemonApiService: PokeApiService,
    cardColor: Color = MaterialTheme.colorScheme.surfaceVariant, // Color de fondo de la tarjeta principal
    colorTexto: Color = MaterialTheme.colorScheme.onSurface, // Color del texto
    itemCardColor: Color = MaterialTheme.colorScheme.surface, // Color de fondo para cada item de forma
    onFormClick: (pokemonName: String) -> Unit, // Callback si quieres hacer algo al clickear una forma
    modifier: Modifier = Modifier,
) {
    var specialForms by remember { mutableStateOf<List<SpecialForm>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pokemonSpeciesUrl) {
        if (pokemonSpeciesUrl == null) {
            isLoading = false
            error = "No species data available."
            return@LaunchedEffect
        }

        isLoading = true
        error = null
        coroutineScope.launch {
            try {
                val speciesResponse = pokemonApiService.getSpeciesDetailsByUrl(pokemonSpeciesUrl)
                if (speciesResponse.isSuccessful && speciesResponse.body() != null) {
                    val speciesDetail = speciesResponse.body()!!
                    val forms = mutableListOf<SpecialForm>()

                    speciesDetail.varieties.forEach { variety ->
                        // Filtramos para obtener solo las variedades que no son la por defecto
                        // y que parezcan ser Mega o Gigantamax
                        if (!variety.isDefault) {
                            val formApiName = variety.pokemon.name
                            var displayName = ""
                            var isSpecialForm = false

                            if (formApiName.contains("-mega")) {
                                displayName = "Mega " + formApiName.substringBefore("-mega")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                if (formApiName.contains("-mega-x")) displayName += " X"
                                if (formApiName.contains("-mega-y")) displayName += " Y"
                                isSpecialForm = true
                            } else if (formApiName.contains("-gmax")) {
                                displayName = "Giga " + formApiName.substringBefore("-gmax")
                                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                isSpecialForm = true
                            }
                            // Podrías añadir más condiciones para otras formas (Alola, etc.)
                            // else if (formApiName.contains("-alola")) { ... }

                            if (isSpecialForm) {
                                // Obtener el sprite para esta forma
                                val formDetailsResponse = pokemonApiService.getPokemonDetailsByNameForSprite(formApiName)
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
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(modifier = modifier
            .fillMaxWidth()
            .padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        }
        return
    }
    if (specialForms.isEmpty()) {
        // Puedes mostrar un mensaje o simplemente no renderizar nada si no hay megaevoluciones
        // Box(modifier = modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
        //     Text("No special forms found for $pokemonName.", textAlign = TextAlign.Center)
        // }
        return // No renderizar nada si no hay formas especiales
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent), // El color de fondo lo da el Column interior
        shape = RoundedCornerShape(12.dp), // Esquinas redondeadas para la tarjeta principal
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor) // Color de fondo principal de la tarjeta de "Formas Especiales"
        ) {
            Row( // Encabezado
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 16.dp), // Ajusta el padding según necesites
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Evoluciones Especiales", // Título de la sección
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorTexto
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top
            ) {
                items(specialForms) { form ->
                    SpecialFormItemView(
                        specialForm = form,
                        backgroundColor = itemCardColor, // Color para las tarjetas individuales de cada forma
                        colorTexto = colorTexto,
                        onClick = { onFormClick(form.formName) }
                    )
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
        modifier = modifier
            .wrapContentSize(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backgroundColor
        ),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp)
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(specialForm.spriteUrl ?: "") // Usa una URL vacía o un placeholder si es null
                    .crossfade(true)
//                    .error(R.drawable.ic_pokeball_placeholder) // Reemplaza con tu placeholder
//                    .placeholder(R.drawable.ic_pokeball_placeholder) // Reemplaza con tu placeholder
                    .build(),
                contentDescription = specialForm.displayName,
                modifier = Modifier
                    .size(72.dp) // Tamaño del sprite
                    .padding(bottom = 4.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = specialForm.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = colorTexto,
                maxLines = 2, // Para nombres largos
                lineHeight = 13.sp
            )
        }
    }
}

data class GigasMegasDescription(
    val pokeId: Int,
    val desc: String,
)

val descripcionesMegas = listOf<GigasMegasDescription>(
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
    GigasMegasDescription(10043, ""),
    GigasMegasDescription(10044, ""),
    GigasMegasDescription(10045, ""),
    GigasMegasDescription(10046, ""),
    GigasMegasDescription(10047, ""),
    GigasMegasDescription(10048, ""),
    GigasMegasDescription(10049, ""),
    GigasMegasDescription(10050, ""),
)
//https://www.wikidex.net/wiki/Megaevoluci%C3%B3n