package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.david.pokedex_api.api.model.PokemonDetailResponse

private enum class SpriteOption(val label: String, val hasShiny: Boolean, val isVideo: Boolean, val isAnimatedImage: Boolean) {
    OFFICIAL_ART("Official Art", true, false, false),
    HOME("Home", true, false, false),
    GIF("GIF", true, false, true),
    CLASSIC("Sprites clásicos", true, false, false),
    GIGAMAX("Gigamax", false, false, true)
}

data class LabeledSprite(val label: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSprites(
    pokemon: PokemonDetailResponse,
    colorTexto: Color,
    nombreSpanish: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedOption by rememberSaveable { mutableStateOf(SpriteOption.OFFICIAL_ART.name) }
    var isShiny by rememberSaveable { mutableStateOf(false) }
    val option = SpriteOption.valueOf(selectedOption)

    // Gigamax
    val gigamaxSprite = remember(pokemon.id) {
        dinamaxLiveSprites.find { it.pokeId == pokemon.id }
    }

    // Opciones disponibles
    val availableOptions = remember(pokemon, gigamaxSprite) {
        SpriteOption.entries.filter { opt ->
            when (opt) {
                SpriteOption.OFFICIAL_ART -> pokemon.sprites.other?.officialArtwork?.frontDefault != null
                SpriteOption.HOME -> pokemon.sprites.other?.home?.frontDefault != null
                SpriteOption.GIF -> pokemon.sprites.other?.showdown?.frontDefault != null
                SpriteOption.CLASSIC -> pokemon.sprites.frontDefault != null
                SpriteOption.GIGAMAX -> gigamaxSprite != null
            }
        }
    }

    val sprites = remember(option, isShiny, pokemon) {
        buildSpriteList(pokemon, option, isShiny, gigamaxSprite)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila: Dropdown + botón shiny
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dropdown
            var dropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = option.label,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedContainerColor = colorTexto.copy(alpha = 0.05f),
                        unfocusedContainerColor = colorTexto.copy(alpha = 0.03f),
                        focusedBorderColor = colorTexto.copy(alpha = 0.3f),
                        unfocusedBorderColor = colorTexto.copy(alpha = 0.15f),
                        focusedTextColor = colorTexto,
                        unfocusedTextColor = colorTexto,
                        focusedTrailingIconColor = colorTexto,
                        unfocusedTrailingIconColor = colorTexto.copy(alpha = 0.6f)
                    )
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    availableOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = opt.label,
                                    fontWeight = if (opt == option) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedOption = opt.name
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Botón shiny (solo si la opción tiene shiny)
            if (option.hasShiny) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isShiny) Color(0xFFFFD700).copy(alpha = 0.4f)
                            else colorTexto.copy(alpha = 0.08f)
                        )
                        .clickable { isShiny = !isShiny },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✦",
                        fontSize = 18.sp,
                        color = if (isShiny) Color(0xFFB8860B) else colorTexto.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Contenido
        if (sprites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isShiny) "No hay sprites shiny disponibles" else "No hay sprites disponibles",
                    color = colorTexto.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(items = sprites, key = { it.url }) { sprite ->
                    SpriteCard(sprite = sprite, colorTexto = colorTexto, isAnimated = option.isAnimatedImage)
                }
            }
        }
    }
}

@Composable
private fun SpriteCard(
    sprite: LabeledSprite,
    colorTexto: Color,
    isAnimated: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colorTexto.copy(alpha = 0.05f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = sprite.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = colorTexto.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (isAnimated) {
            val imageLoader = remember {
                ImageLoader.Builder(context)
                    .components { add(ImageDecoderDecoder.Factory()) }
                    .build()
            }
            AsyncImage(
                model = ImageRequest.Builder(context).data(sprite.url).crossfade(true).allowHardware(false).build(),
                imageLoader = imageLoader,
                contentDescription = sprite.label,
                modifier = Modifier.size(160.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context).data(sprite.url).crossfade(true).build(),
                contentDescription = sprite.label,
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun buildSpriteList(
    pokemon: PokemonDetailResponse,
    option: SpriteOption,
    isShiny: Boolean,
    gigamaxSprite: DinamaxLiveSprite?,
): List<LabeledSprite> {
    val list = mutableListOf<LabeledSprite>()
    val sprites = pokemon.sprites
    val other = sprites.other

    when (option) {
        SpriteOption.OFFICIAL_ART -> {
            val art = other?.officialArtwork
            if (isShiny) {
                art?.frontShiny?.let { list.add(LabeledSprite("Shiny", it)) }
            } else {
                art?.frontDefault?.let { list.add(LabeledSprite("Normal", it)) }
            }
        }

        SpriteOption.HOME -> {
            val home = other?.home
            if (isShiny) {
                home?.frontShiny?.let { list.add(LabeledSprite("Frontal shiny", it)) }
                home?.frontShinyFemale?.let { list.add(LabeledSprite("Frontal shiny ♀", it)) }
            } else {
                home?.frontDefault?.let { list.add(LabeledSprite("Frontal", it)) }
                home?.frontFemale?.let { list.add(LabeledSprite("Frontal ♀", it)) }
            }
        }

        SpriteOption.GIF -> {
            val sd = other?.showdown
            if (isShiny) {
                sd?.frontShiny?.let { list.add(LabeledSprite("Frontal shiny", it)) }
                sd?.backShiny?.let { list.add(LabeledSprite("Trasero shiny", it)) }
            } else {
                sd?.frontDefault?.let { list.add(LabeledSprite("Frontal", it)) }
                sd?.backDefault?.let { list.add(LabeledSprite("Trasero", it)) }
                sd?.frontFemale?.let { list.add(LabeledSprite("Frontal ♀", it)) }
                sd?.backFemale?.let { list.add(LabeledSprite("Trasero ♀", it)) }
            }
        }

        SpriteOption.CLASSIC -> {
            if (isShiny) {
                sprites.frontShiny?.let { list.add(LabeledSprite("Frontal shiny", it)) }
                sprites.backShiny?.let { list.add(LabeledSprite("Trasero shiny", it)) }
                sprites.frontShinyFemale?.let { list.add(LabeledSprite("Frontal shiny ♀", it)) }
                sprites.backShinyFemale?.let { list.add(LabeledSprite("Trasero shiny ♀", it)) }
            } else {
                sprites.frontDefault?.let { list.add(LabeledSprite("Frontal", it)) }
                sprites.backDefault?.let { list.add(LabeledSprite("Trasero", it)) }
                sprites.frontFemale?.let { list.add(LabeledSprite("Frontal ♀", it)) }
                sprites.backFemale?.let { list.add(LabeledSprite("Trasero ♀", it)) }
            }
        }

        SpriteOption.GIGAMAX -> {
            gigamaxSprite?.let { list.add(LabeledSprite("Gigamax", it.spriteUrl)) }
        }
    }

    return list
}

// --- Datos Gigamax ---

data class DinamaxLiveSprite(
    val pokeId: Int,
    val spriteUrl: String,
)

val dinamaxLiveSprites = listOf(
    DinamaxLiveSprite(10196, "https://images.wikidexcdn.net/mwuploads/wikidex/9/98/latest/20191206054551/Charizard_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10198, "https://images.wikidexcdn.net/mwuploads/wikidex/f/f2/latest/20191208035316/Butterfree_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10199, "https://images.wikidexcdn.net/mwuploads/wikidex/7/74/latest/20191207032343/Pikachu_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10200, "https://images.wikidexcdn.net/mwuploads/wikidex/5/5c/latest/20191204065122/Meowth_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10201, "https://images.wikidexcdn.net/mwuploads/wikidex/9/9e/latest/20191206055329/Machamp_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10202, "https://images.wikidexcdn.net/mwuploads/wikidex/b/ba/latest/20191205025526/Gengar_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10203, "https://images.wikidexcdn.net/mwuploads/wikidex/6/6b/latest/20191204064551/Kingler_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10204, "https://images.wikidexcdn.net/mwuploads/wikidex/3/30/latest/20191206055216/Lapras_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10205, "https://images.wikidexcdn.net/mwuploads/wikidex/8/81/latest/20191208180045/Eevee_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10206, "https://images.wikidexcdn.net/mwuploads/wikidex/7/7d/latest/20191205030221/Snorlax_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10207, "https://images.wikidexcdn.net/mwuploads/wikidex/3/3a/latest/20191206055024/Garbodor_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10208, "https://images.wikidexcdn.net/mwuploads/wikidex/5/50/latest/20191208030656/Melmetal_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10212, "https://images.wikidexcdn.net/mwuploads/wikidex/8/86/latest/20191207034018/Corviknight_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10213, "https://images.wikidexcdn.net/mwuploads/wikidex/4/4a/latest/20191208043316/Orbeetle_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10214, "https://images.wikidexcdn.net/mwuploads/wikidex/a/a7/latest/20191202213011/Drednaw_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10215, "https://images.wikidexcdn.net/mwuploads/wikidex/c/c9/latest/20191205024332/Coalossal_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10216, "https://images.wikidexcdn.net/mwuploads/wikidex/b/b0/latest/20191205025109/Flapple_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10218, "https://images.wikidexcdn.net/mwuploads/wikidex/5/5f/latest/20191208043317/Sandaconda_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10219, "https://images.wikidexcdn.net/mwuploads/wikidex/1/14/latest/20191206055500/Toxtricity_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10228, "https://images.wikidexcdn.net/mwuploads/wikidex/1/14/latest/20191206055500/Toxtricity_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10220, "https://images.wikidexcdn.net/mwuploads/wikidex/b/be/latest/20191205024034/Centiskorch_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10221, "https://images.wikidexcdn.net/mwuploads/wikidex/5/51/latest/20191205030031/Hatterene_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10222, "https://images.wikidexcdn.net/mwuploads/wikidex/b/bf/latest/20191205025806/Grimmsnarl_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10223, "https://images.wikidexcdn.net/mwuploads/wikidex/0/09/latest/20191205023619/Alcremie_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10224, "https://images.wikidexcdn.net/mwuploads/wikidex/e/ea/latest/20191205024732/Copperajah_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10225, "https://images.wikidexcdn.net/mwuploads/wikidex/2/28/latest/20191206054822/Duraludon_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10195, "https://images.wikidexcdn.net/mwuploads/wikidex/5/56/latest/20200621041242/Venusaur_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10197, "https://images.wikidexcdn.net/mwuploads/wikidex/1/10/latest/20200621041607/Blastoise_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10209, "https://images.wikidexcdn.net/mwuploads/wikidex/4/46/latest/20200621042120/Rillaboom_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10210, "https://images.wikidexcdn.net/mwuploads/wikidex/c/c2/latest/20200621042347/Cinderace_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10211, "https://images.wikidexcdn.net/mwuploads/wikidex/8/89/latest/20200928210111/Inteleon_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10226, "https://images.wikidexcdn.net/mwuploads/wikidex/3/31/latest/20200715053735/Urshifu_brusco_Gigamax_EpEc.gif"),
    DinamaxLiveSprite(10227, "https://images.wikidexcdn.net/mwuploads/wikidex/8/85/latest/20200715054208/Urshifu_fluido_Gigamax_EpEc.gif"),
)
