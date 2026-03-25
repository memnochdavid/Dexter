package com.david.pokedex_api.api.model

import com.google.gson.annotations.SerializedName

data class NameEntry( // Objeto común para nombres localizados
    val language: NamedApiResource,
    val name: String
)




data class PokemonType(
    val name: String,
    val url: String
)

data class FlavorTextEntry(
    @SerializedName("flavor_text") // Buena práctica añadirlo aunque coincida el nombre
    val flavorText: String?, // <--- CAMBIO IMPORTANTE: String anulable
    val language: NamedApiResource,
    val version: NamedApiResource
)

data class PokemonSpeciesResponse(
    val id: Int,
    val name: String,
    @SerializedName("names")
    val localizedNames: List<NameEntry>,
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntry>,
    val generation: NamedApiResource,
    @SerializedName("base_happiness")
    val baseHappiness: Int?,
    @SerializedName("capture_rate")
    val captureRate: Int?,
    @SerializedName("evolution_chain")
    val evolutionChain: EvolutionChainUrl?,
    val color: NamedApiResource?,
    @SerializedName("genera")
    val genera: List<GenusEntry>?,
    val varieties: List<PokemonVariety>,
    @SerializedName("gender_rate")
    val genderRate: Int?,          // -1=sin género, 0=siempre macho, 8=siempre hembra, n/8=prob. hembra
    @SerializedName("egg_groups")
    val eggGroups: List<NamedApiResource>?,
    @SerializedName("hatch_counter")
    val hatchCounter: Int?,        // Ciclos de eclosión (×255 + 1 = pasos aprox.)
    @SerializedName("growth_rate")
    val growthRate: NamedApiResource?,
    val habitat: NamedApiResource?,
    @SerializedName("is_legendary")
    val isLegendary: Boolean?,
    @SerializedName("is_mythical")
    val isMythical: Boolean?
)
data class PokemonVariety(
    @SerializedName("is_default") // Para Gson, si usas snake_case en JSON y camelCase en Kotlin
    val isDefault: Boolean,
    val pokemon: NamedApiResource // NamedApiResource típicamente tiene 'name' y 'url'
)
data class NamedApiResource(
    val name: String,
    val url: String
)


data class EvolutionChainUrl( // <--- NUEVA DATA CLASS
    val url: String // Contiene la URL para obtener los detalles de la cadena de evolución
)

data class EvolutionChainDetailResponse(
    val id: Int,
    @SerializedName("baby_trigger_item")
    val babyTriggerItem: NamedApiResource?,
    val chain: ChainLink
)

data class ChainLink(
    @SerializedName("is_baby")
    val isBaby: Boolean,
    val species: NamedApiResource, // El Pokémon en esta etapa de la cadena
    @SerializedName("evolution_details")
    val evolutionDetails: List<EvolutionDetail>, // Detalles de CÓMO evoluciona a esta forma (si no es la base)
    @SerializedName("evolves_to")
    val evolvesTo: List<ChainLink> // Pokémon a los que evoluciona (puede estar vacío)
)

data class EvolutionDetail( // Detalles de cómo ocurre la evolución
    val item: NamedApiResource?,
    val trigger: NamedApiResource, // ej: "level-up", "use-item", "trade"
    @SerializedName("gender")
    val gender: Int?, // 1 para hembra, 2 para macho, null si no aplica
    @SerializedName("held_item")
    val heldItem: NamedApiResource?,
    @SerializedName("known_move")
    val knownMove: NamedApiResource?,
    @SerializedName("known_move_type")
    val knownMoveType: NamedApiResource?,
    @SerializedName("location")
    val location: NamedApiResource?,
    @SerializedName("min_affection")
    val minAffection: Int?,
    @SerializedName("min_beauty")
    val minBeauty: Int?,
    @SerializedName("min_happiness")
    val minHappiness: Int?,
    @SerializedName("min_level")
    val minLevel: Int?,
    @SerializedName("needs_overworld_rain")
    val needsOverworldRain: Boolean,
    @SerializedName("party_species")
    val partySpecies: NamedApiResource?,
    @SerializedName("party_type")
    val partyType: NamedApiResource?,
    @SerializedName("relative_physical_stats")
    val relativePhysicalStats: Int?, // -1, 0, 1
    @SerializedName("time_of_day")
    val timeOfDay: String, // "day", "night"
    @SerializedName("trade_species")
    val tradeSpecies: NamedApiResource?,
    @SerializedName("turn_upside_down")
    val turnUpsideDown: Boolean
    // ... otros campos posibles ...
)

data class PokemonListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<PokemonListItem>
)

data class PokemonListItem(
    val name: String,
    val url: String // URL para obtener los detalles de este Pokémon
) {
    // Función de utilidad para extraer el ID del Pokémon de la URL
    // La URL es como "https://pokeapi.co/api/v2/pokemon/1/"
    fun getPokemonId(): Int? {
        return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
    }

    // Función de utilidad para obtener la URL del sprite usando el ID
    // Esta es una convención común para los sprites de la PokeAPI,
    // pero no siempre es la imagen de mejor calidad.
    // Para sprites de alta calidad, necesitarías llamar al endpoint de detalles del Pokémon.
    fun getSpriteUrl(): String? {
        val id = getPokemonId()
        return if (id != null) {
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
        } else {
            null
        }
    }
}


// Un nuevo modelo para representar un Pokémon en la lista con los detalles que queremos mostrar
// @Immutable para que Compose lo trate como estable y evite recomposiciones innecesarias
@androidx.compose.runtime.Immutable
data class PokemonSummary(
    val id: Int,
    val name: String,
    val spriteUrl: String?,
    val types: List<String>, // Lista de nombres de tipos
    val colorName: String?
)

data class GenerationListResponse(
    val count: Int,
    val results: List<NamedApiResource> // Cada 'NamedApiResource' es una generación (nombre y URL)
)

// Para /api/v2/generation/{id_or_name}
data class GenerationDetailResponse(
    val id: Int,
    @SerializedName("name") // Nombre original (ej: "generation-i")
    val apiName: String,
    @SerializedName("names")
    val localizedNames: List<NameEntry>, // Nombres de generación localizados
    @SerializedName("main_region")
    val mainRegion: NamedApiResource,
    @SerializedName("pokemon_species")
    val pokemonSpecies: List<NamedApiResource>
    // ...
)

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: PokemonSprites,
    val types: List<TypeResponseSlot>,
    val abilities: List<AbilitySlot>,
    val stats: List<StatSlot>,
    val species: NamedApiResource,
    val moves: List<PokemonMoveSlot>,
    val forms: List<NamedApiResource>,
    @SerializedName("base_experience")
    val baseExperience: Int?,
    val cries: PokemonCries?
)

data class PokemonCries(
    val latest: String?,
    val legacy: String?
)

data class PokemonSprites(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?,
    // Puedes añadir 'back_default', 'back_shiny', etc.
    val other: OtherSprites?
)

data class OtherSprites(
    @SerializedName("official-artwork")
    val officialArtwork: OfficialArtwork?
    // Puedes añadir 'dream_world', 'home', etc.
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?
)

data class TypeResponseSlot(
    val slot: Int,
    val type: NamedApiResource // Contiene el nombre y URL del tipo
)

data class AbilitySlot(
    val ability: NamedApiResource,
    @SerializedName("is_hidden")
    val isHidden: Boolean,
    val slot: Int
)

data class StatSlot(
    @SerializedName("base_stat")
    val baseStat: Int,
    val effort: Int,
    val stat: NamedApiResource // Nombre de la estadística (ej: "hp", "attack")
)

data class VersionGroupDetail(
    @SerializedName("level_learned_at")
    val levelLearnedAt: Int,
    @SerializedName("move_learn_method")
    val moveLearnMethod: NamedApiResource, // Cómo se aprende el movimiento (ej: "level-up", "machine", "tutor")
    @SerializedName("version_group")
    val versionGroup: NamedApiResource // El grupo de versiones del juego al que aplica este detalle
    // (ej: "red-blue", "gold-silver", "sword-shield")
)

data class MoveDetailResponse(
    val id: Int,
    val name: String,
    val names: List<MoveNameEntry>,
    val power: Int?,
    val pp: Int?,
    val accuracy: Int?,
    @SerializedName("type")
    val moveType: NamedApiResource?,
    @SerializedName("damage_class")
    val damageClass: NamedApiResource?,
    // ***** AÑADIR ESTO SI NO LO TIENES *****
    @SerializedName("effect_entries")
    val effectEntries: List<VerboseEffect>, // Para la descripción principal
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<MoveFlavorTextEntry> // Para descripciones de juegos específicos (opcional)
    // ... otros campos
)
data class MoveFlavorTextEntry(
    @SerializedName("flavor_text")
    val flavorText: String,
    val language: NamedApiResource,
    @SerializedName("version_group")
    val versionGroup: NamedApiResource
)
data class MoveNameEntry(
    val name: String, // El nombre traducido
    val language: NamedApiResource // Contiene el nombre del idioma (ej: "es") y su URL
)

data class PokemonMoveSlot(
    val move: NamedApiResource,
    @SerializedName("version_group_details")
    val versionGroupDetails: List<VersionGroupDetail>,
    var translatedName: String? = null // Para almacenar el nombre en español
)

data class VerboseEffect(
    val effect: String,
    @SerializedName("short_effect")
    val shortEffect: String,
    val language: NamedApiResource
)



data class AbilityFlavorText(
    @SerializedName("flavor_text")
    val flavorText: String,
    val language: NamedApiResource,
    @SerializedName("version_group")
    val versionGroup: NamedApiResource
)

data class AbilityDetailResponse(
    val id: Int,
    val name: String, // Nombre original
    @SerializedName("names")
    val localizedNames: List<NameEntry>, // Nombres localizados
    @SerializedName("effect_entries")
    val effectEntries: List<VerboseEffect>, // Efectos principales
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<AbilityFlavorText> // Descripciones "flavor"
    // ... otros campos que puedas necesitar
)

data class TypeDetailResponse(
    val id: Int,
    val name: String,
    val names: List<NameEntry>,
    @SerializedName("damage_relations")
    val damageRelations: TypeDamageRelations
    // ... otros campos si los necesitas
)

data class TypeDamageRelations(
    @SerializedName("no_damage_to")
    val noDamageTo: List<NamedApiResource>, // Tipos a los que este tipo NO hace daño (x0 ofensivo)
    @SerializedName("half_damage_to")
    val halfDamageTo: List<NamedApiResource>, // Tipos a los que este tipo hace la MITAD de daño (x0.5 ofensivo)
    @SerializedName("double_damage_to")
    val doubleDamageTo: List<NamedApiResource>, // Tipos a los que este tipo hace DOBLE daño (x2 ofensivo)
    @SerializedName("no_damage_from")
    val noDamageFrom: List<NamedApiResource>, // Tipos de los que este tipo NO recibe daño (x0 defensivo)
    @SerializedName("half_damage_from")
    val halfDamageFrom: List<NamedApiResource>, // Tipos de los que este tipo recibe la MITAD de daño (x0.5 defensivo)
    @SerializedName("double_damage_from")
    val doubleDamageFrom: List<NamedApiResource> // Tipos de los que este tipo recibe DOBLE daño (x2 defensivo)
)



data class PokemonSpeciesVariety(
    @SerializedName("is_default")
    val isDefault: Boolean,
    val pokemon: NamedApiResource // Contiene el nombre y URL de la forma específica (ej. "charizard-mega-x")
)

data class SpecialForm(
    val formName: String, // ej: "charizard-mega-x"
    val displayName: String, // ej: "Mega Charizard X"
    val spriteUrl: String?
)

data class TypeListResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("next") val next: String?, // URL for the next page of results, if any (types usually fit in one page)
    @SerializedName("previous") val previous: String?, // URL for the previous page of results, if any
    @SerializedName("results") val results: List<NamedApiResource> // The list of Pokémon types
)
data class ItemDetailResponse(
    val id: Int,
    val name: String,
    val names: List<NameEntry>,
    val cost: Int?,
    val category: NamedApiResource?,
    @SerializedName("effect_entries")
    val effectEntries: List<VerboseEffect>?,
    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<ItemFlavorTextEntry>?,
    val sprites: ItemSprites?
)

data class ItemFlavorTextEntry(
    val text: String,
    val language: NamedApiResource,
    @SerializedName("version_group")
    val versionGroup: NamedApiResource
)

data class ItemSprites(
    val default: String?
)

data class ItemListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResource>
)

@androidx.compose.runtime.Immutable
data class ItemSummary(
    val id: Int,
    val name: String,
    val localizedName: String,
    val category: String?,
    val cost: Int?,
    val effect: String?,
    val spriteUrl: String?
)

// --- Berries ---
data class BerryListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResource>
)

data class BerryDetailResponse(
    val id: Int,
    val name: String,
    @SerializedName("growth_time")
    val growthTime: Int,
    @SerializedName("max_harvest")
    val maxHarvest: Int,
    @SerializedName("natural_gift_power")
    val naturalGiftPower: Int,
    @SerializedName("natural_gift_type")
    val naturalGiftType: NamedApiResource?,
    val size: Int,
    val smoothness: Int,
    @SerializedName("soil_dryness")
    val soilDryness: Int,
    val firmness: NamedApiResource?,
    val flavors: List<BerryFlavorMap>?,
    val item: NamedApiResource
)

data class BerryFlavorMap(
    val potency: Int,
    val flavor: NamedApiResource
)

@androidx.compose.runtime.Immutable
data class BerrySummary(
    val id: Int,
    val name: String,
    val localizedName: String,
    val naturalGiftType: String?,
    val naturalGiftPower: Int,
    val growthTime: Int,
    val size: Int,
    val smoothness: Int,
    val maxHarvest: Int,
    val spriteUrl: String?,
    val flavors: Map<String, Int>
)
data class DisplayableEvolutionStage(
    val pokemonName: String,         // Nombre del Pokémon ya formateado/traducido
    val pokemonSpriteUrl: String,
    val evolutionCondition: String?  // Condición de evolución ya construida, formateada y traducida
    // (ej: "Nivel 36", "Piedra Trueno", "Intercambio equipando Roca del Rey")
)



data class GenericNamedResourceDetail(
    val id: Int,
    val name: String, // Nombre API original
    val names: List<NameEntry> // Lista de nombres localizados
    // Puedes añadir otros campos comunes si existen y son útiles
)
data class GenusEntry(
    val genus: String,
    val language: NamedApiResource
)
data class DisplayablePokemonVariety(
    val id: Int, // ID del Pokémon de la variedad
    val name: String, // Nombre (potencialmente localizado)
    val spriteUrl: String?,
    val isDefault: Boolean // Aunque filtremos, puede ser útil mantenerla
)
data class PokemonFormDetailResponse(
    val id: Int,
    val name: String, // e.g., "arceus-bug"
    @SerializedName("form_name")
    val formName: String, // Often empty, but can be "alola", "mega-x", etc. for specific forms
    @SerializedName("form_names")
    val localizedFormNames: List<LocalizedName>, // Localized names for the form variation
    @SerializedName("names")
    val localizedPokemonNames: List<LocalizedName>, // Localized names for the Pokémon itself in this form
    val sprites: PokemonFormSprites,
    val pokemon: NamedApiResource, // Reference to the base Pokemon this form belongs to
    @SerializedName("is_default")
    val isDefault: Boolean,
    @SerializedName("is_battle_only")
    val isBattleOnly: Boolean,
    @SerializedName("is_mega")
    val isMega: Boolean
    // Add other fields like 'types', 'version_group' if needed
)
data class PokemonFormSprites(
    @SerializedName("front_default")
    val frontDefault: String?,
    @SerializedName("front_shiny")
    val frontShiny: String?,
    @SerializedName("back_default")
    val backDefault: String?,
    @SerializedName("back_shiny")
    val backShiny: String?
    // No 'other' or 'official-artwork' directly in form sprites,
    // you might need to fall back to the main Pokemon's sprites if these are null.
)
data class LocalizedName(
    val language: NamedApiResource,
    val name: String
)

data class DisplayableEvolutionChain(
    val stages: List<DisplayableEvolutionStage>
)
data class PokemonSpeciesDetailResponse(
    val id: Int,
    val name: String, // Nombre de la especie
    @SerializedName("evolution_chain")
    val evolutionChain: NamedApiResource?, // URL a la cadena de evolución
    @SerializedName("evolves_from_species")
    val evolvesFromSpecies: NamedApiResource?,
    val varieties: List<PokemonSpeciesVariety>,
    // ... otros campos como flavor_text_entries, genera, etc.
)
data class Language(
    @SerializedName("name") val name: String, // ej: "es", "en"
    @SerializedName("url") val url: String
)
data class Version(
    @SerializedName("name") val name: String, // ej: "sword", "red"
    @SerializedName("url") val url: String
)
data class PokemonTypeSlot(
    val slot: Int,
    val type: PokemonType
)

// --- Encounters ---
data class PokemonEncounterResponse(
    @SerializedName("location_area")
    val locationArea: NamedApiResource,
    @SerializedName("version_details")
    val versionDetails: List<VersionEncounterDetail>
)

data class VersionEncounterDetail(
    @SerializedName("encounter_details")
    val encounterDetails: List<EncounterDetail>,
    @SerializedName("max_chance")
    val maxChance: Int,
    val version: NamedApiResource
)

data class EncounterDetail(
    val chance: Int,
    @SerializedName("condition_values")
    val conditionValues: List<NamedApiResource>,
    @SerializedName("max_level")
    val maxLevel: Int,
    @SerializedName("method")
    val method: NamedApiResource,
    @SerializedName("min_level")
    val minLevel: Int
)

data class DisplayableEncounter(
    val locationName: String,
    val versions: List<DisplayableVersionEncounter>
)

data class DisplayableVersionEncounter(
    val versionName: String,
    val maxChance: Int,
    val methods: List<DisplayableEncounterMethod>
)

data class DisplayableEncounterMethod(
    val methodName: String,
    val minLevel: Int,
    val maxLevel: Int,
    val chance: Int
)

// Agrupado por juego
data class GameEncounterGroup(
    val versionName: String,
    val locations: List<GameEncounterLocation>
)

data class GameEncounterLocation(
    val locationName: String,
    val maxChance: Int,
    val methods: List<DisplayableEncounterMethod>
)

data class MoveListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<NamedApiResource>
)

@androidx.compose.runtime.Immutable
data class MoveSummary(
    val id: Int,
    val name: String,
    val localizedName: String,
    val typeName: String?,
    val damageClass: String?,
    val power: Int?,
    val pp: Int?,
    val accuracy: Int?,
    val description: String?
)