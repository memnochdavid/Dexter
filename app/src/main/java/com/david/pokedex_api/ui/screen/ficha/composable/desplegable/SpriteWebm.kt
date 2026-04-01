package com.david.pokedex_api.ui.screen.ficha.composable.desplegable

import android.net.Uri
import android.util.Log
import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun WebmImageDialog(
    pokemonResourceName: String?, // Nombre del recurso en res/raw (ej: "pikachu_home")
    pokemonDisplayName: String,  // Nombre para mostrar en el título (ej: "Pikachu")
    onDismiss: () -> Unit
) {
    if (pokemonResourceName == null) {
        // Decide cómo manejar esto: puedes no mostrar el diálogo,
        // mostrar un mensaje de error, o simplemente loguearlo.
        Log.e("WebmImageDialog", "pokemonResourceName is null for $pokemonDisplayName. Dialog will not be shown.")
        return // No se muestra el diálogo si no hay nombre de recurso
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permite que la Card controle el ancho
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f) // O el ancho que prefieras
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White) // O el color de fondo de la Card
        ) {
            Column( // androidx.compose.foundation.layout.Column
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween

                ){
                    Text(
                        text = pokemonDisplayName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .weight(0.8f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                // Usar ExoPlayerComposable
                ExoPlayerSimple(
                    pokemonInputName = pokemonResourceName, // Pasar el nombre del recurso
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f) // Asumiendo que los sprites son cuadrados
                        .clip(RoundedCornerShape(12.dp)) // Clip opcional
//                        .background(Color.Black) // Fondo negro detrás del video transparente
                )

            }
        }
    }
}




@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSimple(
    pokemonInputName: String,
    modifier: Modifier = Modifier,
    backgroundColor: Int = android.graphics.Color.WHITE
) {
    val context = LocalContext.current

    val resourceName = remember(pokemonInputName) {
        transformPokemonNameToResourceName(pokemonInputName)
    }

    val resourceId = remember(resourceName) {
        context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }

    if (resourceId == 0) {
        Text("Video '$resourceName.webm' no encontrado", color = Color.Red, modifier = modifier)
        return
    }

    val videoUri = remember(resourceId) {
        Uri.parse("android.resource://${context.packageName}/$resourceId")
    }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                setBackgroundColor(backgroundColor)
                setShutterBackgroundColor(backgroundColor)
            }
        },
        modifier = modifier
    )
}

// Mapeo completo nombre API → nombre recurso local.
// Se busca primero por nombre completo (fullNameMap), luego por sufijo (formSuffixMap).
// Los casos ambiguos (colores, tipos) van en fullNameMap con nombre completo.

// Sufijos genéricos sin ambigüedad (usados como fallback para nombres no listados)
private val formSuffixMap = mapOf(
    "f" to "hembra",
    "m" to "macho",
    "z" to "z",
)
private val fullNameMap = mapOf(
    // --- Nombres compuestos (sin forma, el guión es parte del nombre) ---
    // Unown (forma base = A)
    "unown" to "unown_a",
    "unown-exclamation" to "unown_exclamacion",
    "unown-question" to "unown_pregunta",

    // --- Formas base que no tienen webp sin sufijo ---
    "ogerpon" to "ogerpon_mascara_turquesa",
    "terapagos" to "terapagos_normal",
    "gimmighoul" to "gimmighoul_andante",
    "vivillon" to "vivillon_vergel",
    "frillish-male" to "frillish",
    "jellicent-male" to "jellicent",

    // --- Maushold ---
    "maushold-family-of-four" to "maushold_familia_de_cuatro",
    "maushold-family-of-three" to "maushold_familia_de_tres",
    "maushold" to "maushold_familia_de_tres",

    // --- Paradox pasado (Scarlet) ---
    "great-tusk" to "colmilargo",
    "scream-tail" to "colagrito",
    "brute-bonnet" to "furioseta",
    "flutter-mane" to "melenaleteo",
    "slither-wing" to "reptalada",
    "sandy-shocks" to "pelarena",
    "roaring-moon" to "bramaluna",
    "walking-wake" to "ondulagua",
    "gouging-fire" to "flamariete",
    "raging-bolt" to "electrofuria",

    // --- Paradox futuro (Violet) ---
    "iron-treads" to "ferropuas",
    "iron-bundle" to "ferrosaco",
    "iron-hands" to "ferropalmas",
    "iron-jugulis" to "ferrodada",
    "iron-moth" to "ferropolilla",
    "iron-thorns" to "ferrocuello",
    "iron-valiant" to "ferropaladin",
    "iron-leaves" to "ferroverdor",
    "iron-boulder" to "ferromole",
    "iron-crown" to "ferrotesta",

    "ho-oh" to "ho_oh",
    "porygon-z" to "porygon_z",
    "mr-mime" to "mr_mime",
    "mime-jr" to "mime_jr",
    "mr-rime" to "mr_rime",
    "hakamo-o" to "hakamo_o",
    "jangmo-o" to "jangmo_o",
    "kommo-o" to "kommo_o",
    "tapu-koko" to "tapu_koko",
    "tapu-lele" to "tapu_lele",
    "tapu-bulu" to "tapu_bulu",
    "tapu-fini" to "tapu_fini",
    "chi-yu" to "chi_yu",
    "chien-pao" to "chien_pao",
    "ting-lu" to "ting_lu",
    "wo-chien" to "wo_chien",
    "type-null" to "codigo_cero",

    // --- Formas por defecto (el recurso es el nombre base sin sufijo) ---
    "deoxys-normal" to "deoxys",
    "hoopa-confined" to "hoopa",
    "darmanitan-standard" to "darmanitan",
    "keldeo-ordinary" to "keldeo",
    "basculegion-male" to "basculegion",
    "morpeko-full-belly" to "morpeko",
    "mimikyu-disguised" to "mimikyu",
    "eiscue-ice" to "eiscue",
    "zygarde-50" to "zygarde",
    "shaymin-land" to "shaymin_tierra",
    "meloetta-aria" to "meloetta_lirica",
    "wishiwashi-solo" to "wishiwashi_individual",

    // --- Deoxys ---
    "deoxys-attack" to "deoxys_ataque",
    "deoxys-defense" to "deoxys_defensa",
    "deoxys-speed" to "deoxys_velocidad",

    // --- Castform ---
    "castform-sunny" to "castform_sol",
    "castform-rainy" to "castform_lluvia",
    "castform-snowy" to "castform_nieve",

    // --- Cherrim ---
    "cherrim-overcast" to "cherrim_encapotado",
    "cherrim-sunshine" to "cherrim_soleado",

    // --- Burmy / Wormadam ---
    "burmy-plant" to "burmy_planta",
    "burmy-sandy" to "burmy_arena",
    "burmy-trash" to "burmy_basura",
    "wormadam-plant" to "wormadam_planta",
    "wormadam-sandy" to "wormadam_arena",
    "wormadam-trash" to "wormadam_basura",

    // --- Aegislash ---
    "aegislash-blade" to "aegislash_filo",
    "aegislash-shield" to "aegislash_escudo",

    // --- Hoopa ---
    "hoopa-unbound" to "hoopa_desatado",

    // --- Oricorio ---
    "oricorio-baile" to "oricorio_apasionado",
    "oricorio-pom-pom" to "oricorio_animado",
    "oricorio-pau" to "oricorio_placido",
    "oricorio-sensu" to "oricorio_refinado",

    // --- Wishiwashi ---
    "wishiwashi-school" to "wishiwashi_banco",

    // --- Dudunsparce ---
    "dudunsparce-two-segment" to "dudunsparce_binodular",
    "dudunsparce-three-segment" to "dudunsparce_trinodular",

    // --- Giratina / Dialga / Palkia ---
    "giratina-origin" to "giratina_origen",
    "giratina-altered" to "giratina_modificada",
    "dialga-origin" to "dialga_origen",
    "palkia-origin" to "palkia_origen",

    // --- Shaymin ---
    "shaymin-sky" to "shaymin_cielo",

    // --- Keldeo ---
    "keldeo-resolute" to "keldeo_brio",

    // --- Meloetta ---
    "meloetta-pirouette" to "meloetta_danza",

    // --- Fuerzas de la Naturaleza ---
    "landorus-incarnate" to "landorus_avatar",
    "landorus-therian" to "landorus_totem",
    "thundurus-incarnate" to "thundurus_avatar",
    "thundurus-therian" to "thundurus_totem",
    "tornadus-incarnate" to "tornadus_avatar",
    "tornadus-therian" to "tornadus_totem",
    "enamorus-incarnate" to "enamorus_avatar",
    "enamorus-therian" to "enamorus_totem",

    // --- Kyurem ---
    "kyurem-white" to "kyurem_blanco",
    "kyurem-black" to "kyurem_negro",

    // --- Primigenios ---
    "groudon-primal" to "groudon_primigenio",
    "kyogre-primal" to "kyogre_primigenio",

    // --- Lycanroc ---
    "lycanroc-midday" to "lycanroc_diurno",
    "lycanroc-midnight" to "lycanroc_nocturno",
    "lycanroc-dusk" to "lycanroc_crepuscular",

    // --- Rotom ---
    "rotom-heat" to "rotom_calor",
    "rotom-frost" to "rotom_frio",
    "rotom-wash" to "rotom_lavado",
    "rotom-fan" to "rotom_ventilador",
    "rotom-mow" to "rotom_corte",

    // --- Toxtricity ---
    "toxtricity-amped" to "toxtricity_aguda",
    "toxtricity-low-key" to "toxtricity_grave",

    // --- Darmanitan ---
    "darmanitan-zen" to "darmanitan_daruma",
    "darmanitan-galar-standard" to "darmanitan_de_galar",
    "darmanitan-galar-zen" to "darmanitan_de_galar_daruma",

    // --- Cramorant ---
    "cramorant-gulping" to "cramorant_tragatodo",
    "cramorant-gorging" to "cramorant_engulletodo",

    // --- Morpeko ---
    "morpeko-hangry" to "morpeko_voraz",

    // --- Eiscue ---
    "eiscue-noice" to "eiscue_cara_deshielo",

    // --- Mimikyu ---
    "mimikyu-busted" to "mimikyu_descubierto",

    // --- Palafin ---
    "palafin-zero" to "palafin_ingenua",
    "palafin-hero" to "palafin_heroica",

    // --- Zygarde ---
    "zygarde-10" to "zygarde_diez",
    "zygarde-complete" to "zygarde_completo",

    // --- Urshifu ---
    "urshifu-single-strike" to "urshifu_brusco",
    "urshifu-rapid-strike" to "urshifu_fluido",

    // --- Calyrex ---
    "calyrex-ice-rider" to "calyrex_jinete_glacial",
    "calyrex-shadow-rider" to "calyrex_jinete_espectral",

    // --- Necrozma ---
    "necrozma-dawn-wings" to "necrozma_alas_del_alba",
    "necrozma-dusk-mane" to "necrozma_melena_crepuscular",
    "necrozma-ultra" to "ultra_necrozma",

    // --- Zacian / Zamazenta ---
    "zacian-crowned-sword" to "zacian_espada_suprema",
    "zacian-crowned" to "zacian_espada_suprema",
    "zamazenta-crowned-shield" to "zamazenta_escudo_supremo",
    "zamazenta-crowned" to "zamazenta_escudo_supremo",

    // --- Gimmighoul ---
    "gimmighoul-roaming" to "gimmighoul_andante",
    "gimmighoul-chest" to "gimmighoul_cofre",

    // --- Terapagos ---
    "terapagos-normal" to "terapagos_normal",
    "terapagos-terastal" to "terapagos_teracristal",
    "terapagos-stellar" to "terapagos_estelar",

    // --- Zarude ---
    "zarude-dada" to "zarude_papa",

    // --- Magearna ---
    "magearna-original" to "magearna_vetusta",

    // --- Greninja ---
    "greninja-ash" to "greninja_ash",

    // --- Ursaluna ---
    "ursaluna-bloodmoon" to "ursaluna_luna_carmesi",

    // --- Ogerpon ---
    "ogerpon-cornerstone-mask" to "ogerpon_mascara_cimiento",
    "ogerpon-wellspring-mask" to "ogerpon_mascara_fuente",
    "ogerpon-hearthflame-mask" to "ogerpon_mascara_horno",
    "ogerpon-teal-mask" to "ogerpon_mascara_turquesa",

    // --- Marshadow ---
    "marshadow-zenith" to "marshadow_ataque",

    // --- Basculin ---
    "basculin-red-striped" to "basculin_roja",
    "basculin-blue-striped" to "basculin_azul",
    "basculin-white-striped" to "basculin_blanca",

    // --- Floette ---
    "floette-eternal" to "floette_eterna",

    // --- Pikachu gorras ---
    "pikachu-original-cap" to "pikachu_original",
    "pikachu-hoenn-cap" to "pikachu_hoenn",
    "pikachu-sinnoh-cap" to "pikachu_sinnoh",
    "pikachu-unova-cap" to "pikachu_teselia",
    "pikachu-kalos-cap" to "pikachu_kalos",
    "pikachu-alola-cap" to "pikachu_alola",
    "pikachu-partner-cap" to "pikachu_companero",

    // --- Genesect ---
    "genesect-douse" to "genesect_hidrorom",
    "genesect-shock" to "genesect_fulgorom",
    "genesect-burn" to "genesect_pirorom",
    "genesect-chill" to "genesect_criorom",

    // --- Vivillon ---
    "vivillon-meadow" to "vivillon_vergel",
    "vivillon-icy-snow" to "vivillon_polar",
    "vivillon-polar" to "vivillon_polar",
    "vivillon-tundra" to "vivillon_tundra",
    "vivillon-continental" to "vivillon_continental",
    "vivillon-garden" to "vivillon_floral",
    "vivillon-elegant" to "vivillon_estepa",
    "vivillon-modern" to "vivillon_moderno",
    "vivillon-marine" to "vivillon_marino",
    "vivillon-archipelago" to "vivillon_isleno",
    "vivillon-high-plains" to "vivillon_oasis",
    "vivillon-sandstorm" to "vivillon_desierto",
    "vivillon-river" to "vivillon_oceano",
    "vivillon-monsoon" to "vivillon_monzon",
    "vivillon-savanna" to "vivillon_jungla",
    "vivillon-sun" to "vivillon_solar",
    "vivillon-ocean" to "vivillon_oceano",
    "vivillon-jungle" to "vivillon_jungla",
    "vivillon-fancy" to "vivillon_fantasia",
    "vivillon-poke-ball" to "vivillon_poke_ball",

    // --- Furfrou ---
    "furfrou-heart" to "furfrou_corazon",
    "furfrou-star" to "furfrou_estrella",
    "furfrou-diamond" to "furfrou_rombo",
    "furfrou-debutante" to "furfrou_dama",
    "furfrou-matron" to "furfrou_senorita",
    "furfrou-dandy" to "furfrou_caballero",
    "furfrou-la-reine" to "furfrou_aristocratico",
    "furfrou-kabuki" to "furfrou_kabuki",
    "furfrou-pharaoh" to "furfrou_faraonico",

    // --- Deerling / Sawsbuck ---
    "deerling-spring" to "deerling_primavera",
    "deerling-summer" to "deerling_verano",
    "deerling-autumn" to "deerling_otono",
    "deerling-winter" to "deerling_invierno",
    "sawsbuck-spring" to "sawsbuck_primavera",
    "sawsbuck-summer" to "sawsbuck_verano",
    "sawsbuck-autumn" to "sawsbuck_otono",
    "sawsbuck-winter" to "sawsbuck_invierno",

    // --- Tatsugiri ---
    "tatsugiri-curly" to "tatsugiri_curvada",
    "tatsugiri-droopy" to "tatsugiri_languida",
    "tatsugiri-stretchy" to "tatsugiri_recta",

    // --- Shellos / Gastrodon ---
    "shellos-east" to "shellos_este",
    "shellos-west" to "shellos_oeste",
    "gastrodon-east" to "gastrodon_este",
    "gastrodon-west" to "gastrodon_oeste",

    // --- Minior ---
    "minior-red" to "minior_rojo",
    "minior-orange" to "minior_naranja",
    "minior-yellow" to "minior_amarillo",
    "minior-green" to "minior_verde",
    "minior-blue" to "minior_azul",
    "minior-indigo" to "minior_anil",
    "minior-violet" to "minior_violeta",
    "minior-meteor" to "minior_meteorito",

    // --- Flabébé / Floette / Florges (colores en femenino) ---
    "flabebe-red" to "flabebe_roja",
    "flabebe-orange" to "flabebe_naranja",
    "flabebe-yellow" to "flabebe_amarilla",
    "flabebe-blue" to "flabebe_azul",
    "flabebe-white" to "flabebe_blanca",
    "floette-red" to "floette_roja",
    "floette-orange" to "floette_naranja",
    "floette-yellow" to "floette_amarilla",
    "floette-blue" to "floette_azul",
    "floette-white" to "floette_blanca",
    "florges-red" to "florges_roja",
    "florges-orange" to "florges_naranja",
    "florges-yellow" to "florges_amarilla",
    "florges-blue" to "florges_azul",
    "florges-white" to "florges_blanca",

    // --- Squawkabilly ---
    "squawkabilly-green-plumage" to "squawkabilly_verde",
    "squawkabilly-blue-plumage" to "squawkabilly_azul",
    "squawkabilly-yellow-plumage" to "squawkabilly_amarillo",
    "squawkabilly-white-plumage" to "squawkabilly_blanco",

    // --- Arceus (tipos) ---
    "arceus-bug" to "arceus_bicho",
    "arceus-dark" to "arceus_siniestro",
    "arceus-dragon" to "arceus_dragon",
    "arceus-electric" to "arceus_electrico",
    "arceus-fairy" to "arceus_hada",
    "arceus-fighting" to "arceus_lucha",
    "arceus-fire" to "arceus_fuego",
    "arceus-flying" to "arceus_volador",
    "arceus-ghost" to "arceus_fantasma",
    "arceus-grass" to "arceus_planta",
    "arceus-ground" to "arceus_tierra",
    "arceus-ice" to "arceus_hielo",
    "arceus-poison" to "arceus_veneno",
    "arceus-psychic" to "arceus_psiquico",
    "arceus-rock" to "arceus_roca",
    "arceus-steel" to "arceus_acero",
    "arceus-water" to "arceus_agua",

    // --- Silvally (tipos) ---
    "silvally-bug" to "silvally_bicho",
    "silvally-dark" to "silvally_siniestro",
    "silvally-dragon" to "silvally_dragon",
    "silvally-electric" to "silvally_electrico",
    "silvally-fairy" to "silvally_hada",
    "silvally-fighting" to "silvally_lucha",
    "silvally-fire" to "silvally_fuego",
    "silvally-flying" to "silvally_volador",
    "silvally-ghost" to "silvally_fantasma",
    "silvally-grass" to "silvally_planta",
    "silvally-ground" to "silvally_tierra",
    "silvally-ice" to "silvally_hielo",
    "silvally-poison" to "silvally_veneno",
    "silvally-psychic" to "silvally_psiquico",
    "silvally-rock" to "silvally_roca",
    "silvally-steel" to "silvally_acero",
    "silvally-water" to "silvally_agua",

    // --- Alcremie sabores (los más comunes) ---
    "alcremie-vanilla-cream" to "alcremie_crema_rosa",
    "alcremie-ruby-cream" to "alcremie_crema_rosa",
    "alcremie-matcha-cream" to "alcremie_crema_rosa",
    "alcremie-mint-cream" to "alcremie_crema_rosa",
    "alcremie-lemon-cream" to "alcremie_crema_de_limon",
    "alcremie-salted-cream" to "alcremie_crema_salada",
    "alcremie-ruby-swirl" to "alcremie_mezcla_rosa",
    "alcremie-caramel-swirl" to "alcremie_mezcla_caramelo",
    "alcremie-rainbow-swirl" to "alcremie_tres_sabores",
)

fun transformPokemonNameToResourceName(pokemonInputName: String): String {
    val inputLower = pokemonInputName.lowercase()

    // Primero: buscar coincidencia exacta del nombre completo (con o sin guiones)
    fullNameMap[inputLower]?.let { return it }

    // Sin guiones → nombre simple
    if (!inputLower.contains("-")) {
        if (inputLower.contains("código")) return "codigo_cero"
        return inputLower
    }

    val partes = inputLower.split("-")

    // Mega evoluciones
    if (partes.size >= 2 && partes[1] == "mega") {
        return if (partes.size == 3) "mega_${partes[0]}_${partes[2]}"
        else "mega_${partes[0]}"
    }

    // Regionales
    when (partes[1]) {
        "alola" -> return "${partes[0]}_de_alola"
        "galar" -> {
            // Casos como darmanitan-galar-zen → darmanitan_de_galar_daruma
            if (partes.size > 2) {
                val subForm = partes.drop(2).joinToString("-")
                val mappedSub = formSuffixMap[subForm]
                return if (mappedSub != null && mappedSub.isNotEmpty())
                    "${partes[0]}_de_galar_$mappedSub"
                else "${partes[0]}_de_galar"
            }
            return "${partes[0]}_de_galar"
        }
        "hisui" -> return "${partes[0]}_de_hisui"
        "paldea" -> {
            if (partes[0] == "tauros" && partes.size > 2) {
                return when (partes[2]) {
                    "blaze" -> "tauros_de_paldea_ardiente"
                    "aqua" -> "tauros_de_paldea_acuatica"
                    "combat" -> "tauros_de_paldea_combatiente"
                    else -> "tauros_de_paldea"
                }
            }
            return "${partes[0]}_de_paldea"
        }
    }

    // Sufijo de forma (todo después del primer guión)
    val base = partes[0]
    val formSuffix = partes.drop(1).joinToString("-")

    // Buscar en el mapa de sufijos
    val mapped = formSuffixMap[formSuffix]
    if (mapped != null) {
        return if (mapped.isEmpty()) base else "${base}_$mapped"
    }

    // Fallback genérico: reemplazar guiones por guiones bajos
    return inputLower.replace("-", "_")
}

/*
@Composable
fun VlcPlayer(
    url: String,
    modifier: Modifier = Modifier // Este modifier es importante, vendrá de WebmImageDialog
) {
    val context = LocalContext.current
    val TAG = "VlcPlayerComposable"

    var libVLC by remember { mutableStateOf<LibVLC?>(null) }
    var vlcMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 1. Inicializar LibVLC
    LaunchedEffect(context) {
        Log.d(TAG, "Attempting to initialize LibVLC.")
        try {
            libVLC = LibVLC(context, ArrayList<String>().apply {
                // add("--no-audio") // Descomenta si no necesitas audio
                add("--verbose=0") // 0 para menos logs, 1 o 2 para más detalle si depuras
            })
            Log.d(TAG, "LibVLC initialized successfully: $libVLC")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LibVLC", e)
        }
    }

    // 2. Liberar recursos de LibVLC y MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Disposing VlcPlayer: Releasing MediaPlayer and LibVLC.")
            vlcMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                detachViews()
                release()
            }
            vlcMediaPlayer = null
            libVLC?.release()
            libVLC = null
            Log.d(TAG, "MediaPlayer and LibVLC released.")
        }
    }

    // 3. Manejar pausa/reanudación del ciclo de vida
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vlcMediaPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            val currentPlayer = vlcMediaPlayer
            if (currentPlayer == null) {
                return@LifecycleEventObserver
            }
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (currentPlayer.isPlaying) {
                        currentPlayer.pause()
                        Log.d(TAG, "Lifecycle ON_PAUSE: MediaPlayer paused.")
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!currentPlayer.isPlaying && currentPlayer.media != null) {
                        currentPlayer.play()
                        Log.d(TAG, "Lifecycle ON_RESUME: MediaPlayer playing.")
                    }
                }
                else -> { /* No-op */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "Removing lifecycle observer.")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. Crear y actualizar la vista de video
    AndroidView(
        factory = { ctx ->
            Log.d(TAG, "AndroidView factory: Creating VLCVideoLayout.")
            VLCVideoLayout(ctx).apply {
                // INTENTO DE CONFIGURAR TRANSPARENCIA:
                // El objetivo es que el SurfaceView dentro de VLCVideoLayout sea transparente
                // para que el fondo negro aplicado en el Modifier de Compose (en WebmImageDialog)
                // se muestre a través de las partes transparentes del video.

                var surfaceViewSuccessfullyConfigured = false
                if (this.childCount > 0) {
                    val surfaceViewCandidate = this.getChildAt(0) // VLCVideoLayout suele tener el SurfaceView como primer hijo
                    if (surfaceViewCandidate is android.view.SurfaceView) {
                        Log.d(TAG, "SurfaceView found, attempting to configure for transparency.")

                        // setZOrderOnTop(true) es crucial para que setFormat con transparencia funcione en un SurfaceView.
                        // ADVERTENCIA: Esto hace que el SurfaceView se dibuje encima de cualquier otro
                        // elemento de Compose en la misma elevación en la ventana.
                        // Para un diálogo donde el video es el contenido principal, podría estar bien.
                        surfaceViewCandidate.setZOrderOnTop(true)

                        // Establece el formato de píxeles del SurfaceHolder para soportar transparencia.
                        // PixelFormat.TRANSLUCENT es la opción estándar.
                        // PixelFormat.RGBA_8888 también soporta alfa y podría ser una alternativa a probar.
                        surfaceViewCandidate.holder.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

                        // Adicionalmente, podrías intentar establecer el color de fondo del propio SurfaceView a transparente,
                        // aunque setFormat debería ser el principal habilitador de la transparencia.
                        // surfaceViewCandidate.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        surfaceViewSuccessfullyConfigured = true
                        Log.d(TAG, "SurfaceView configured for transparency (setZOrderOnTop(true), holder.setFormat(TRANSLUCENT)).")
                    } else {
                        Log.w(TAG, "VLCVideoLayout's first child is not a SurfaceView. Found: ${surfaceViewCandidate::class.java.name}")
                    }
                } else {
                    Log.w(TAG, "VLCVideoLayout has no children upon creation; cannot configure SurfaceView directly.")
                }

                // El fondo del VLCVideoLayout (que es un FrameLayout) también debe ser transparente.
                // Si el SurfaceView se configuró correctamente como transparente, esto asegura que el
                // FrameLayout contenedor no lo opaque.
                // Si el SurfaceView NO se pudo configurar como transparente, establecer esto a transparente
                // por sí solo NO hará que el contenido del video alfa se mezcle con el fondo de Compose;
                // simplemente hará que el FrameLayout de VLCVideoLayout sea transparente, pero el video
                // renderizado por el motor de VLC (con su fondo rosa) aún se dibujaría.
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                if (surfaceViewSuccessfullyConfigured) {
                    Log.d(TAG, "VLCVideoLayout background set to TRANSPARENT (as SurfaceView is expected to be transparent).")
                } else {
                    Log.d(TAG, "VLCVideoLayout background set to TRANSPARENT (SurfaceView transparency configuration might have failed or was not applicable).")
                }
            }
        },
        modifier = modifier, // Este modifier incluye el .background(Color.Black) de WebmImageDialog
        update = { vlcVideoLayout ->
            val currentLibVLC = libVLC
            if (currentLibVLC != null && vlcMediaPlayer == null) {
                Log.d(TAG, "AndroidView update: LibVLC available, MediaPlayer is null. Creating MediaPlayer for URL: $url")

                vlcMediaPlayer = MediaPlayer(currentLibVLC).apply {
                    attachViews(vlcVideoLayout, null, false, false)
                    Log.d(TAG, "MediaPlayer views attached to VLCVideoLayout.")

                    val media = Media(currentLibVLC, Uri.parse(url)).apply {
                        addOption(":input-repeat=65535")
                        Log.d(TAG, "Media option :input-repeat=65535 added.")

                        // Para WebM (VP8/VP9), la decodificación por software suele ser robusta.
                        // La aceleración por hardware puede ser quisquillosa, especialmente con transparencia.
                        setHWDecoderEnabled(false, false)
                        Log.d(TAG, "Media HW Decoder enabled: false (prefer software for potential alpha handling).")

                        // Opciones adicionales que podrían influir en el manejo del alfa (experimentales):
                        // addOption(":codec=libvpx") // o vp9, vp8 si ayuda a VLC a seleccionar el decodificador correcto
                        // addOption("--no-osd") // Deshabilita On Screen Display, por si interfiere
                        // addOption("--video-chroma=RGBA") // Esto es más para salida, pero podría tener algún efecto (raro)
                    }

                    setMedia(media)
                    media.release()
                    Log.d(TAG, "Media set on MediaPlayer and released.")

                    setEventListener { event ->
                        when (event.type) {
                            MediaPlayer.Event.Playing -> Log.d(TAG, "MediaPlayer Event: Playing")
                            MediaPlayer.Event.Paused -> Log.d(TAG, "MediaPlayer Event: Paused")
                            MediaPlayer.Event.Stopped -> Log.d(TAG, "MediaPlayer Event: Stopped")
                            MediaPlayer.Event.EndReached -> Log.d(TAG, "MediaPlayer Event: EndReached (should be handled by :input-repeat)")
                            MediaPlayer.Event.EncounteredError -> Log.e(TAG, "MediaPlayer Event: EncounteredError")
                            MediaPlayer.Event.Buffering -> {
                                // val buffering = event.buffering
                                // Log.d(TAG, "MediaPlayer Event: Buffering $buffering")
                            }
                            MediaPlayer.Event.Opening -> Log.d(TAG, "MediaPlayer Event: Opening")
                            // Puedes añadir más eventos para depuración si es necesario
                            // MediaPlayer.Event.TimeChanged -> { /* Log.v(TAG, "MediaPlayer Event: TimeChanged ${event.timeChanged}"); */ }
                            // MediaPlayer.Event.PositionChanged -> { /* Log.v(TAG, "MediaPlayer Event: PositionChanged ${event.positionChanged}"); */ }
                            // MediaPlayer.Event.Vout -> Log.d(TAG, "MediaPlayer Event: Vout ${event.voutCount}") // Video output count
                        }
                    }

                    play()
                    Log.d(TAG, "MediaPlayer.play() called.")
                }
            } else if (currentLibVLC == null && vlcMediaPlayer == null) {
                Log.w(TAG, "AndroidView update: LibVLC is null, cannot create MediaPlayer yet.")
            } else if (vlcMediaPlayer != null) {
                // MediaPlayer ya existe.
                Log.v(TAG, "AndroidView update: MediaPlayer already exists. Views should be attached. Current state: isPlaying=${vlcMediaPlayer?.isPlaying}")
                // Si la vlcVideoLayout necesitara ser re-adjuntada por alguna razón (poco común en este setup)
                // if (!vlcMediaPlayer!!.isPlaying && vlcVideoLayout.isAttachedToWindow) {
                //    vlcMediaPlayer?.attachViews(vlcVideoLayout, null, false, false)
                // }
            }
        }
    )
}
*/