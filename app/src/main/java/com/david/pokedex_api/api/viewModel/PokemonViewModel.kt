package com.david.pokedex_api.api.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.david.pokedex_api.DexterApplication
import com.david.pokedex_api.api.client.RetrofitClient
import com.david.pokedex_api.api.db.PokemonSummaryEntity
import com.david.pokedex_api.api.model.*
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.screen.comun.ALL_POKEMON_TYPES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.Response
import com.david.pokedex_api.api.wikidex.WikiDexRepository
import java.util.concurrent.ConcurrentHashMap

class PokemonViewModel : ViewModel() {

    val pokemonApiService: PokeApiService = RetrofitClient.instance
    private val pokemonDao = DexterApplication.database.pokemonDao()
    private val wikiDexRepository = WikiDexRepository(pokemonDao)

    // --- Card recall: trackea que Pokemon esta "dentro de la pokeball" ---
    val recalledPokemonId = MutableStateFlow<Int?>(null)

    // --- Estados de búsqueda/filtro (compartidos con el BottomSheet de MainActivity) ---
    // Pokemon
    var pokemonSearchQuery = MutableStateFlow("")
    var pokemonSelectedType1 = MutableStateFlow("Sin tipo")
    var pokemonSelectedType2 = MutableStateFlow("Sin tipo")
    var pokemonShowMegas = MutableStateFlow(false)
    var pokemonShowGigamax = MutableStateFlow(false)
    var pokemonShowRegionals = MutableStateFlow(false)
    var pokemonShowLegendaries = MutableStateFlow(false)
    var pokemonShowMythicals = MutableStateFlow(false)
    var pokemonIsGridView = MutableStateFlow(false)

    // Cache de formas especiales (megas/gigas) — se cargan bajo demanda
    private val _specialFormsSummaries = MutableLiveData<List<PokemonSummary>>(emptyList())
    val specialFormsSummaries: LiveData<List<PokemonSummary>> = _specialFormsSummaries
    private var specialFormsLoaded = false

    // IDs de legendarios y singulares (datos estáticos de PokeAPI)
    val legendaryIds = setOf(
        144, 145, 146, 150, // Articuno, Zapdos, Moltres, Mewtwo
        243, 244, 245, 249, 250, // Raikou, Entei, Suicune, Lugia, Ho-Oh
        377, 378, 379, 380, 381, 382, 383, 384, // Regis, Lati@s, Weather trio
        480, 481, 482, 483, 484, 485, 486, 487, 488, // Lake trio, Creation trio, Heatran, Regigigas, Giratina, Cresselia
        638, 639, 640, 641, 642, 643, 644, 645, 646, // Swords, Forces, Tao trio
        716, 717, 718, // Xerneas, Yveltal, Zygarde
        772, 773, // Type: Null, Silvally
        785, 786, 787, 788, 789, 790, 791, 792, 800, // Tapus, Cosmog line, Necrozma
        888, 889, 890, 891, 892, 895, 896, 897, 898, // Zacian, Zamazenta, Eternatus, Kubfu, Urshifu, Regidrago, Regieleki, Spectrier, Glastrier, Calyrex
        905, // Enamorus
        1001, 1002, 1003, 1004, 1007, 1008, 1014, 1015, 1016, 1017, 1024, // Paldea legendaries
    )
    val mythicalIds = setOf(
        151, // Mew
        251, // Celebi
        385, 386, // Jirachi, Deoxys
        489, 490, 491, 492, 493, // Phione, Manaphy, Darkrai, Shaymin, Arceus
        494, 647, 648, 649, // Victini, Keldeo, Meloetta, Genesect
        719, 720, 721, // Diancie, Hoopa, Volcanion
        801, 802, 807, 808, 809, // Magearna, Marshadow, Zeraora, Meltan, Melmetal
        893, // Zarude
        1025, // Pecharunt
    )

    fun fetchSpecialForms() {
        if (specialFormsLoaded) return
        specialFormsLoaded = true
        viewModelScope.launch {
            try {
                // 1. Listar todas las formas alternativas de PokeAPI
                val megaGmaxResources = mutableListOf<PokemonListItem>()
                var offset = 1025
                var hasMore = true
                while (hasMore) {
                    val response = withContext(Dispatchers.IO) {
                        pokemonApiService.getPokemonList(limit = 200, offset = offset)
                    }
                    if (response.isSuccessful) {
                        val results = response.body()?.results ?: emptyList()
                        if (results.isEmpty()) { hasMore = false }
                        else {
                            megaGmaxResources.addAll(results.filter { r ->
                                r.name.contains("-mega") || r.name.contains("-gmax") ||
                                r.name.contains("-alola") || r.name.contains("-galar") ||
                                r.name.contains("-hisui") || r.name.contains("-paldea")
                            })
                            offset += 200
                        }
                    } else { hasMore = false }
                }

                // 2. Obtener detalles de cada forma (tipos, species URL)
                val semaphore = kotlinx.coroutines.sync.Semaphore(30)
                data class FormDetail(val id: Int, val name: String, val types: List<String>,
                    val speciesUrl: String, val fallbackSprite: String?)
                val formDetails = megaGmaxResources.map { resource ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val resp = pokemonApiService.getPokemonDetails(resource.name)
                                if (resp.isSuccessful) {
                                    val d = resp.body()!!
                                    FormDetail(d.id, d.name, d.types.map { it.type.name },
                                        d.species.url,
                                        d.sprites.other?.officialArtwork?.frontDefault ?: d.sprites.frontDefault)
                                } else null
                            } catch (_: Exception) { null }
                        }
                    }
                }.awaitAll().filterNotNull()

                // 3. Agrupar por species y obtener varieties para calcular form index
                val bySpeciesUrl = formDetails.groupBy { it.speciesUrl }
                // Cache de species → varieties (nombre → index)
                val varietiesCache = mutableMapOf<String, Map<String, Int>>()
                bySpeciesUrl.keys.map { speciesUrl ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val resp = pokemonApiService.getPokemonSpeciesDetailsByUrl(speciesUrl)
                                if (resp.isSuccessful) {
                                    val varieties = resp.body()?.varieties ?: emptyList()
                                    val indexMap = varieties.mapIndexed { index, v -> v.pokemon.name to index }.toMap()
                                    synchronized(varietiesCache) { varietiesCache[speciesUrl] = indexMap }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }.awaitAll()

                // 4. Construir PokemonSummary con HOME URL
                val allForms = formDetails.map { form ->
                    val speciesId = form.speciesUrl.trimEnd('/').substringAfterLast('/').toIntOrNull()
                    val formIndex = varietiesCache[form.speciesUrl]?.get(form.name)
                    val homeUrl = if (speciesId != null && formIndex != null) {
                        "https://resource.pokemon-home.com/battledata/img/pokei128/icon${speciesId.toString().padStart(4, '0')}_f${formIndex.toString().padStart(2, '0')}_s0.png"
                    } else null
                    PokemonSummary(
                        id = form.id,
                        name = form.name,
                        spriteUrl = homeUrl ?: form.fallbackSprite,
                        types = form.types,
                        colorName = null,
                        fallbackSpriteUrl = if (homeUrl != null) form.fallbackSprite else null
                    )
                }
                _specialFormsSummaries.postValue(allForms.sortedBy { it.id })
            } catch (_: Exception) { }
        }
    }
    // Movimientos
    var moveSearchQuery = MutableStateFlow("")
    var moveSelectedType = MutableStateFlow("Sin tipo")
    var moveSelectedDamageClass = MutableStateFlow("Todos")
    // Items
    var itemSearchQuery = MutableStateFlow("")
    var itemSelectedCategory = MutableStateFlow("Todas")
    var itemCurrentTab = MutableStateFlow(0)
    // Bayas
    var berrySearchQuery = MutableStateFlow("")
    var berrySelectedType = MutableStateFlow("Sin tipo")
    // Regiones
    var regionSearchQuery = MutableStateFlow("")
    // Extras
    var extrasSearchQuery = MutableStateFlow("")
    var natureStatFilter = MutableStateFlow("Todos")
    // Ficha - seccion seleccionada
    var selectedDetailSection = MutableStateFlow("DESC")
    // Ficha - secciones disponibles (calculadas por DetallesDesplegables)
    var availableDetailSections = MutableStateFlow<List<String>>(emptyList())
    // IDs de Pokemon cuya animacion de entrada ya se ejecuto
    val animatedPokemonIds = mutableSetOf<Int>()

    private val _pokemonDetails = MutableLiveData<PokemonDetailResponse?>()
    val pokemonDetails: LiveData<PokemonDetailResponse?> = _pokemonDetails

    private val _pokemonDescription = MutableLiveData<String?>()
    val pokemonDescription: LiveData<String?> = _pokemonDescription

    private val _wikiDexFlavorTexts = MutableLiveData<Map<String, String>>(emptyMap())
    val wikiDexFlavorTexts: LiveData<Map<String, String>> = _wikiDexFlavorTexts

    private val _wikiDexLocations = MutableLiveData<Map<String, String>>(emptyMap())
    val wikiDexLocations: LiveData<Map<String, String>> = _wikiDexLocations

    private val _isLoadingDetails = MutableLiveData<Boolean>(false)
    val isLoadingDetails: LiveData<Boolean> = _isLoadingDetails
    private var detailFetchJob: kotlinx.coroutines.Job? = null

    // --- OPTIMIZACIÓN DE MOVIMIENTOS ---
    private val _moveDetailsMap = MutableStateFlow<Map<String, MoveDetailResponse>>(emptyMap())
    val moveDetailsMap: StateFlow<Map<String, MoveDetailResponse>> = _moveDetailsMap.asStateFlow()

    private val _isLoadingMoves = MutableStateFlow(false)
    val isLoadingMoves: StateFlow<Boolean> = _isLoadingMoves.asStateFlow()
    // ----------------------------------

    private val _generations = MutableLiveData<List<NamedApiResource>>(emptyList())
    val generations: LiveData<List<NamedApiResource>> = _generations

    private val _pokemonByGenerationCache = MutableLiveData<Map<Int, List<PokemonSummary>>>(emptyMap())
    val pokemonByGenerationCache: LiveData<Map<Int, List<PokemonSummary>>> = _pokemonByGenerationCache

    private val _isLoadingPokemonForCurrentGeneration = MutableLiveData<Boolean>(false)
    val isLoadingPokemonForCurrentGeneration: LiveData<Boolean> = _isLoadingPokemonForCurrentGeneration

    private val _isLoadingGenerations = MutableLiveData<Boolean>(false)
    val isLoadingGenerations: LiveData<Boolean> = _isLoadingGenerations

    private val _evolutionChainDetails = MutableLiveData<EvolutionChainDetailResponse?>()
    val evolutionChainDetails: LiveData<EvolutionChainDetailResponse?> = _evolutionChainDetails

    private val _isLoadingEvolutionChain = MutableLiveData<Boolean>(false)
    val isLoadingEvolutionChain: LiveData<Boolean> = _isLoadingEvolutionChain

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private var errorShownThisFetch = false

    private val _pokemonSpeciesDetails = MutableLiveData<PokemonSpeciesResponse?>()
    val pokemonSpeciesDetails: LiveData<PokemonSpeciesResponse?> = _pokemonSpeciesDetails

    private val _pokemonTypes = MutableLiveData<List<String>>(ALL_POKEMON_TYPES)
    val pokemonTypes: LiveData<List<String>> = _pokemonTypes

    private val _areAllPokemonDetailsAttempted = MutableStateFlow(false)
    val areAllPokemonDetailsAttempted: StateFlow<Boolean> = _areAllPokemonDetailsAttempted.asStateFlow()

    private val _totalGenerationsCount = MutableStateFlow(0)
    private val _currentlyFetchingGenerationId = MutableLiveData<Int?>(null)
    private val isLoadingPokemonByGenerationMap = ConcurrentHashMap<Int, Boolean>()

    private val _pokemonFormsAndVarieties = MutableLiveData<List<DisplayablePokemonVariety>>()
    val pokemonFormsAndVarieties: LiveData<List<DisplayablePokemonVariety>> = _pokemonFormsAndVarieties

    // Navegacion por linea evolutiva: lista ordenada de IDs + datos pre-cargados
    private val _navigationList = MutableLiveData<List<Int>>(emptyList())
    val navigationList: LiveData<List<Int>> = _navigationList

    fun setNavigationList(list: List<Int>) {
        _navigationList.value = list
    }

    data class PreloadedPokemonData(
        val detail: PokemonDetailResponse,
        val species: PokemonSpeciesResponse?
    )

    private val _evoChainPokemonMap = MutableStateFlow<Map<Int, PreloadedPokemonData>>(emptyMap())
    val evoChainPokemonMap: StateFlow<Map<Int, PreloadedPokemonData>> = _evoChainPokemonMap.asStateFlow()

    /**
     * Expande un ChainLink tree añadiendo ramas para variantes regionales.
     *
     * Para cada especie con variantes regionales, crea ramas como siblings del padre:
     * - La rama base mantiene los sucesores que corresponden a la forma original
     * - Cada variante regional se añade como sibling con sus sucesores propios
     *
     * Sucesores se asignan por:
     * - Si tiene variante de esa región → rama regional (con species reemplazada)
     * - Si es exclusivo (sin regionales, generación coincide) → rama regional
     * - En otro caso → rama base
     *
     * Retorna el chain expandido + la lista de todos los IDs de pokemon a precargar.
     */
    suspend fun expandChainWithRegionals(
        chain: ChainLink
    ): Pair<ChainLink, List<Int>> = coroutineScope {
        val allRegionSuffixes = listOf("-alola", "-galar", "-hisui", "-paldea")
        val regionToGeneration = mapOf(
            "-alola" to 7, "-galar" to 8, "-hisui" to 8, "-paldea" to 9
        )

        // Paso 1: recopilar varieties y generación de TODAS las especies del chain
        val allSpeciesIds = mutableListOf<Int>()
        fun collectIds(link: ChainLink) {
            link.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()?.let { allSpeciesIds.add(it) }
            link.evolvesTo.forEach { collectIds(it) }
        }
        collectIds(chain)

        data class SpeciesInfo(val varieties: List<PokemonVariety>, val generation: Int?)

        val speciesInfoMap = allSpeciesIds.map { speciesId ->
            async(Dispatchers.IO) {
                try {
                    val resp = pokemonApiService.getPokemonSpeciesDetailsById(speciesId)
                    val body = resp.body()
                    val gen = body?.generation?.url?.trimEnd('/')?.substringAfterLast('/')?.toIntOrNull()
                    speciesId to SpeciesInfo(body?.varieties ?: emptyList(), gen)
                } catch (_: Exception) { speciesId to SpeciesInfo(emptyList(), null) }
            }
        }.awaitAll().toMap()

        val allPokemonIds = mutableListOf<Int>()

        // Paso 2: expandir evolvesTo en cada nivel
        // Procesa una lista de children: si algún child tiene variantes regionales,
        // lo expande en múltiples siblings (base + regionales)
        fun expandChildren(children: List<ChainLink>): List<ChainLink> {
            val result = mutableListOf<ChainLink>()
            for (child in children) {
                val childSpeciesId = child.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
                val childInfo = childSpeciesId?.let { speciesInfoMap[it] }
                val childRegionals = childInfo?.varieties?.filter { v ->
                    !v.isDefault && allRegionSuffixes.any { s -> v.pokemon.name.contains(s) }
                } ?: emptyList()

                if (childSpeciesId != null) allPokemonIds.add(childSpeciesId)

                if (childRegionals.isEmpty()) {
                    // Sin variantes regionales → mantener, expandir nietos
                    result.add(child.copy(evolvesTo = expandChildren(child.evolvesTo)))
                    continue
                }

                // Tiene variantes regionales → crear ramas
                childRegionals.forEach { v ->
                    v.pokemon.url.trimEnd('/').substringAfterLast('/').toIntOrNull()?.let { allPokemonIds.add(it) }
                }

                // Expandir nietos primero
                val expandedGrandchildren = expandChildren(child.evolvesTo)

                // Clasificar cada nieto: ¿a qué rama(s) pertenece?
                data class GrandchildAssignment(val grandchild: ChainLink, val regions: Set<String>)

                val assignments = expandedGrandchildren.map { gc ->
                    val gcSpeciesId = gc.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
                    val gcInfo = gcSpeciesId?.let { speciesInfoMap[it] }
                    val gcVarieties = gcInfo?.varieties ?: emptyList()
                    val gcGen = gcInfo?.generation

                    val gcRegionalSuffixes = gcVarieties
                        .filter { v -> !v.isDefault }
                        .flatMap { v -> allRegionSuffixes.filter { s -> v.pokemon.name.contains(s) } }
                        .toSet()

                    val assigned = mutableSetOf<String>()

                    if (gcRegionalSuffixes.isNotEmpty()) {
                        // El nieto tiene regionales propias → va a ramas coincidentes + base
                        val parentRegions = childRegionals.mapNotNull { v ->
                            allRegionSuffixes.firstOrNull { s -> v.pokemon.name.contains(s) }
                        }.toSet()
                        assigned.addAll(parentRegions.intersect(gcRegionalSuffixes))
                        assigned.add("base")
                    } else if (gcGen != null) {
                        // Sin regionales propias → ¿exclusivo de alguna región?
                        val matching = childRegionals.mapNotNull { v ->
                            val r = allRegionSuffixes.firstOrNull { s -> v.pokemon.name.contains(s) }
                            if (r != null && regionToGeneration[r] == gcGen) r else null
                        }
                        if (matching.isNotEmpty()) assigned.addAll(matching)
                        else assigned.add("base")
                    } else {
                        assigned.add("base")
                    }

                    GrandchildAssignment(gc, assigned)
                }

                // Rama base: nietos asignados a "base"
                val baseGrandchildren = assignments.filter { "base" in it.regions }.map { it.grandchild }
                result.add(child.copy(evolvesTo = baseGrandchildren))

                // Ramas regionales
                for (regionalVar in childRegionals) {
                    val region = allRegionSuffixes.firstOrNull { s -> regionalVar.pokemon.name.contains(s) } ?: continue

                    val regionalGrandchildren = assignments
                        .filter { region in it.regions }
                        .map { assignment ->
                            // Si el nieto tiene variante de esta región, reemplazar su species
                            val gcSpeciesId = assignment.grandchild.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
                            val gcVarieties = gcSpeciesId?.let { speciesInfoMap[it] }?.varieties ?: emptyList()
                            val gcRegionalVar = gcVarieties.firstOrNull { v -> v.pokemon.name.contains(region) }

                            if (gcRegionalVar != null) {
                                val rId = gcRegionalVar.pokemon.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
                                if (rId != null) allPokemonIds.add(rId)
                                assignment.grandchild.copy(
                                    species = NamedApiResource(gcRegionalVar.pokemon.name, gcRegionalVar.pokemon.url)
                                )
                            } else {
                                assignment.grandchild
                            }
                        }

                    result.add(ChainLink(
                        isBaby = child.isBaby,
                        species = NamedApiResource(regionalVar.pokemon.name, regionalVar.pokemon.url),
                        evolutionDetails = child.evolutionDetails,
                        evolvesTo = regionalGrandchildren
                    ))
                }
            }
            return result
        }

        // Expandir desde la raíz: la raíz se mantiene, se expanden sus hijos
        val rootSpeciesId = chain.species.url.trimEnd('/').substringAfterLast('/').toIntOrNull()
        if (rootSpeciesId != null) allPokemonIds.add(rootSpeciesId)

        val expandedChain = chain.copy(evolvesTo = expandChildren(chain.evolvesTo))
        Pair(expandedChain, allPokemonIds.distinct())
    }

    /**
     * Construye la cadena evolutiva adaptada a la región del Pokémon actual.
     * - Si el Pokémon actual NO es regional → devuelve la cadena base tal cual.
     * - Si ES regional → reemplaza cada especie base por su variedad regional.
     *   Especies sin variedad regional en ninguna región se mantienen (ej: Perrserker, Obstagoon).
     *   Especies con variedad regional pero NO de esta región se excluyen (ej: Persian en cadena Galar).
     */
    suspend fun buildChainForCurrentPokemon(
        chainOrderIds: List<Int>,
        currentPokemonName: String
    ): List<Int> = coroutineScope {
        val allRegionSuffixes = listOf("-alola", "-galar", "-hisui", "-paldea")
        val currentRegion = allRegionSuffixes.firstOrNull { currentPokemonName.contains(it) }

        // Si no es regional, devolver la cadena base
        if (currentRegion == null) return@coroutineScope chainOrderIds

        val results = chainOrderIds.map { speciesId ->
            async(Dispatchers.IO) {
                try {
                    val resp = pokemonApiService.getPokemonSpeciesDetailsById(speciesId)
                    val varieties = resp.body()?.varieties ?: return@async speciesId // fallback

                    // Buscar variedad de esta región
                    val regionalVariety = varieties.firstOrNull { v ->
                        v.pokemon.name.contains(currentRegion)
                    }
                    if (regionalVariety != null) {
                        // Tiene variedad de esta región → usar su ID
                        return@async regionalVariety.pokemon.url
                            .trimEnd('/').substringAfterLast('/').toIntOrNull() ?: speciesId
                    }

                    // ¿Tiene variedades regionales de OTRAS regiones?
                    val hasAnyRegional = varieties.any { v ->
                        !v.isDefault && allRegionSuffixes.any { s -> v.pokemon.name.contains(s) }
                    }

                    if (!hasAnyRegional) {
                        // No tiene regionales en ninguna región → es especie exclusiva
                        // (ej: Perrserker, Obstagoon, Runerigus) → incluir
                        speciesId
                    } else {
                        // Tiene regionales de otra región pero no de esta → excluir
                        // (ej: Persian tiene Alola pero no Galar → no va en cadena Galar)
                        null
                    }
                } catch (_: Exception) { speciesId }
            }
        }.awaitAll()

        results.filterNotNull()
    }

    fun preloadEvolutionChain(pokemonIds: List<Int>) {
        viewModelScope.launch {
            pokemonIds.map { id ->
                async(Dispatchers.IO) {
                    try {
                        val detailResp = pokemonApiService.getPokemonDetailsById(id)
                        val detail = detailResp.body() ?: return@async
                        // Species por ID; si falla (formas regionales ID>10000), usar species URL del detail
                        var speciesResp = pokemonApiService.getPokemonSpeciesDetailsById(id)
                        if (!speciesResp.isSuccessful) {
                            speciesResp = pokemonApiService.getPokemonSpeciesDetailsByUrl(detail.species.url)
                        }
                        _evoChainPokemonMap.update { it + (id to PreloadedPokemonData(detail, speciesResp.body())) }
                    } catch (_: Exception) { }
                }
            }.awaitAll()
        }
    }

    fun switchToPreloadedPokemon(pokemonId: Int) {
        val preloaded = _evoChainPokemonMap.value[pokemonId] ?: return
        _pokemonDetails.value = preloaded.detail
        _pokemonSpeciesDetails.value = preloaded.species
        _isLoadingDetails.value = false

        val species = preloaded.species
        val desc = species?.let {
            val preferred = listOf("sword", "shield", "scarlet", "violet", "legends-arceus")
            it.flavorTextEntries.filter { f -> f.language.name == "es" }
                .firstOrNull { f -> preferred.any { p -> f.version.name.contains(p, true) } }?.flavorText
                ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == "es" }?.flavorText
                ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == "en" }?.flavorText
        }
        _pokemonDescription.value = desc?.replace("\n", " ")?.replace("\u000c", " ")?.replace("POKéMON", "Pokémon")

        // Cargar movimientos y encuentros async
        fetchMovesDetailsParallel(preloaded.detail.moves)
        preloaded.detail.id.let { fetchPokemonEncounters(it) }

        // WikiDex async

        val baseSpanishName = species?.localizedNames?.firstOrNull { it.language.name == "es" }?.name
        if (baseSpanishName != null) {
            // Para formas regionales, buscar con nombre regional en WikiDex
            val pokemonApiName = preloaded.detail.name
            val regionalSuffix = mapOf(
                "-alola" to " de Alola", "-galar" to " de Galar",
                "-hisui" to " de Hisui", "-paldea" to " de Paldea"
            ).entries.firstOrNull { pokemonApiName.contains(it.key) }?.value

            val wikiSearchName = if (regionalSuffix != null) "$baseSpanishName$regionalSuffix" else baseSpanishName

            viewModelScope.launch {
                // Intentar con nombre regional primero, fallback al nombre base
                var texts = wikiDexRepository.getFlavorTexts(wikiSearchName)
                if (texts.isEmpty() && regionalSuffix != null) {
                    texts = wikiDexRepository.getFlavorTexts(baseSpanishName)
                }
                _wikiDexFlavorTexts.value = texts
            }
            viewModelScope.launch {
                var locations = wikiDexRepository.getLocations(wikiSearchName)
                if (locations.isEmpty() && regionalSuffix != null) {
                    locations = wikiDexRepository.getLocations(baseSpanishName)
                }
                _wikiDexLocations.value = locations.mapKeys { (apiName, _) ->
                    translateVersionName(apiName)
                }
            }
        }
    }

    fun clearEvoChainPreload() {
        _evoChainPokemonMap.value = emptyMap()
        _navigationList.value = emptyList()
    }

    private val _isLoadingForms = MutableLiveData<Boolean>(false)
    val isLoadingForms: LiveData<Boolean> = _isLoadingForms

    // Caché de memoria para evitar llamadas redundantes a la API (especialmente nombres localizados)
    private val localizedNamesCache = ConcurrentHashMap<String, String>()

    // MEJORA 3: Caché de cadenas evolutivas para deduplicar peticiones
    private val evolutionChainCache = ConcurrentHashMap<String, EvolutionChainDetailResponse>()

    init {
        combine(
            generations.asFlow(),
            pokemonByGenerationCache.asFlow(),
            _isLoadingPokemonForCurrentGeneration.asFlow()
        ) { generationsList, cache, isLoadingPokemon ->
            if (generationsList.isEmpty()) {
                _areAllPokemonDetailsAttempted.value = false
                _totalGenerationsCount.value = 0
                return@combine false
            }
            _totalGenerationsCount.value = generationsList.size
            val allAttempted = generationsList.all { gen ->
                cache.containsKey(gen.getGenerationIdFromUrl())
            }
            allAttempted && !isLoadingPokemon
        }.onEach { allAttemptedValue ->
            _areAllPokemonDetailsAttempted.value = allAttemptedValue
        }.launchIn(viewModelScope)
    }

    fun fetchPokemonForGeneration(generationId: Int?, forceRefresh: Boolean = false) {
        if (generationId == null) return
        val currentCache = _pokemonByGenerationCache.value ?: emptyMap()
        if (!forceRefresh && currentCache.containsKey(generationId)) return
        if (isLoadingPokemonByGenerationMap[generationId] == true) return

        isLoadingPokemonByGenerationMap[generationId] = true
        _isLoadingPokemonForCurrentGeneration.value = true

        viewModelScope.launch {
            try {
                // MEJORA 2: Intentar cargar desde Room primero
                val cachedSummaries = withContext(Dispatchers.IO) {
                    pokemonDao.getSummariesByGeneration(generationId)
                }

                if (!forceRefresh && cachedSummaries.isNotEmpty()) {
                    val summaries = cachedSummaries.map { it.toPokemonSummary() }
                    withContext(Dispatchers.Main) {
                        val newCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        newCache[generationId] = summaries
                        _pokemonByGenerationCache.value = newCache
                    }
                    return@launch
                }

                // Si no hay datos en Room, descargar de la API
                val response = withContext(Dispatchers.IO) { pokemonApiService.getGenerationDetails(generationId) }
                if (response.isSuccessful) {
                    val speciesList = response.body()?.pokemonSpecies ?: emptyList()

                    // MEJORA 4: Semáforo subido a 50 para mayor paralelismo
                    val allSummaries = withContext(Dispatchers.IO) {
                        val semaphore = Semaphore(50)
                        speciesList.map { resource ->
                            async {
                                semaphore.withPermit { fetchSinglePokemonSummary(resource) }
                            }
                        }.awaitAll().filterNotNull().sortedBy { it.id }
                    }

                    // Guardar en Room para futuras cargas
                    withContext(Dispatchers.IO) {
                        val entities = allSummaries.map { PokemonSummaryEntity.fromPokemonSummary(it, generationId) }
                        pokemonDao.insertSummaries(entities)
                    }

                    withContext(Dispatchers.Main) {
                        val newCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        newCache[generationId] = allSummaries
                        _pokemonByGenerationCache.value = newCache
                    }
                }
            } catch (e: Exception) {
                handleError("Error: ${e.message}")
            } finally {
                isLoadingPokemonByGenerationMap[generationId] = false
                _isLoadingPokemonForCurrentGeneration.value = false
            }
        }
    }

    private suspend fun fetchSinglePokemonSummary(speciesResource: NamedApiResource): PokemonSummary? = coroutineScope {
        val id = speciesResource.url.split("/").dropLast(1).lastOrNull() ?: return@coroutineScope null
        try {
            // Paralelizamos las dos llamadas básicas para obtener el resumen (nombre localizado y datos base)
            val speciesDef = async(Dispatchers.IO) { pokemonApiService.getPokemonSpeciesDetails(id) }
            val detailsDef = async(Dispatchers.IO) { pokemonApiService.getPokemonDetails(id) }
            val sRes = speciesDef.await()
            val dRes = detailsDef.await()

            if (dRes.isSuccessful) {
                val detail = dRes.body()!!
                val species = sRes.body()
                PokemonSummary(
                    id = detail.id,
                    name = species?.localizedNames?.find { it.language.name == "es" }?.name ?: detail.name,
                    spriteUrl = detail.sprites.other?.officialArtwork?.frontDefault ?: detail.sprites.frontDefault,
                    types = detail.types.map { it.type.name.replaceFirstChar(Char::titlecase) },
                    colorName = species?.color?.name
                )
            } else null
        } catch (e: Exception) { null }
    }

    fun fetchPokemonDetailsByName(name: String, lang: String) {
        if (_isLoadingDetails.value == true && _pokemonDetails.value?.name?.equals(name, true) == true) return
        detailFetchJob?.cancel()
        _isLoadingDetails.value = true
        _pokemonDetails.value = null
        _pokemonDescription.value = null


        detailFetchJob = viewModelScope.launch {
            try {
                // Paralelismo en las llamadas de detalle
                val dDef = async(Dispatchers.IO) { pokemonApiService.getPokemonDetails(name.lowercase().trim()) }
                val sDef = async(Dispatchers.IO) { pokemonApiService.getPokemonSpeciesDetails(name.lowercase().trim()) }
                val dRes = dDef.await()
                var sRes = sDef.await()

                // Si la species no se encuentra (formas como mega, gmax, regionales),
                // obtener la species URL del pokemon detail y reintentar
                if (!sRes.isSuccessful && dRes.isSuccessful) {
                    val speciesUrl = dRes.body()?.species?.url
                    if (speciesUrl != null) {
                        sRes = pokemonApiService.getPokemonSpeciesDetailsByUrl(speciesUrl)
                    }
                }

                if (sRes.isSuccessful) {
                    val species = sRes.body()
                    _pokemonSpeciesDetails.value = species
                    species?.let {
                        val preferred = listOf("sword", "shield", "scarlet", "violet", "legends-arceus")
                        val desc = it.flavorTextEntries.filter { f -> f.language.name == lang }
                            .firstOrNull { f -> preferred.any { p -> f.version.name.contains(p, true) } }?.flavorText
                            ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == lang }?.flavorText
                            ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == "en" }?.flavorText

                        _pokemonDescription.value = desc?.replace("\n", " ")?.replace("\u000c", " ")?.replace("POKéMON", "Pokémon")
                        it.evolutionChain?.url?.let { url -> fetchEvolutionChainDetails(url) }

                        // WikiDex: fetch en paralelo para datos en español que faltan
                        val spanishName = it.localizedNames
                            .firstOrNull { n -> n.language.name == "es" }?.name
                        if (spanishName != null) {
                            viewModelScope.launch {
                                val wikiTexts = wikiDexRepository.getFlavorTexts(spanishName)
                                _wikiDexFlavorTexts.value = wikiTexts
                            }
                            viewModelScope.launch {
                                val wikiLocations = wikiDexRepository.getLocations(spanishName)
                                // Traducir claves API a nombres en español para consistencia con encounters de PokeAPI
                                _wikiDexLocations.value = wikiLocations.mapKeys { (apiName, _) ->
                                    translateVersionName(apiName)
                                }
                            }
                        }
                    }
                }

                if (dRes.isSuccessful) {
                    val details = dRes.body()
                    _pokemonDetails.value = details
                    // Carga masiva y reactiva de movimientos
                    details?.let { fetchMovesDetailsParallel(it.moves) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) { handleError(e.message ?: "Error") }
            finally { _isLoadingDetails.value = false }
        }
    }

    private fun fetchMovesDetailsParallel(moves: List<PokemonMoveSlot>) {
        viewModelScope.launch {
            _isLoadingMoves.value = true
            val currentMap = _moveDetailsMap.value.toMutableMap()
            val movesToFetch = moves.filter { !currentMap.containsKey(it.move.url) }

            if (movesToFetch.isNotEmpty()) {
                // Descarga paralela por bloques para mantener fluidez sin bloquear
                movesToFetch.chunked(15).forEach { chunk ->
                    val deferred = chunk.map { slot ->
                        async(Dispatchers.IO) {
                            try {
                                val response = pokemonApiService.getMoveDetailsByUrl(slot.move.url)
                                if (response.isSuccessful) slot.move.url to response.body()
                                else null
                            } catch (e: Exception) { null }
                        }
                    }
                    deferred.awaitAll().filterNotNull().forEach { (url, detail) ->
                        if (detail != null) currentMap[url] = detail
                    }
                    // Actualización reactiva: emitimos el nuevo mapa a la UI
                    _moveDetailsMap.value = currentMap.toMap()
                }
            }
            _isLoadingMoves.value = false
        }
    }

    internal suspend fun fetchLocalizedName(resourceUrl: String, fallbackApiName: String, resourceTypeHint: String, languageCode: String = "es"): String {
        if (resourceUrl.isBlank()) return formatApiName(fallbackApiName)

        // OPTIMIZACIÓN: Cache de nombres localizados para evitar cientos de peticiones repetidas
        val cacheKey = "$resourceUrl-$languageCode"
        localizedNamesCache[cacheKey]?.let { return it }

        try {
            val response: Response<out Any> = withContext(Dispatchers.IO) {
                when (resourceTypeHint.lowercase()) {
                    "item" -> pokemonApiService.getItemDetailsByUrl(resourceUrl)
                    "move" -> pokemonApiService.getMoveDetailsByUrl(resourceUrl)
                    "pokemon-species" -> pokemonApiService.getPokemonSpeciesDetailsByUrl(resourceUrl)
                    "type" -> {
                        val id = resourceUrl.split("/").dropLast(1).lastOrNull()
                        if (id != null) pokemonApiService.getTypeDetailsByName(id) else throw Exception()
                    }
                    else -> pokemonApiService.getGenericNamedResourceDetailsByUrl(resourceUrl)
                }
            }
            if (response.isSuccessful) {
                val body = response.body()
                val names: List<NameEntry>? = when (body) {
                    is ItemDetailResponse -> body.names
                    is MoveDetailResponse -> body.names.map { NameEntry(it.language, it.name) }
                    is PokemonSpeciesResponse -> body.localizedNames
                    is TypeDetailResponse -> body.names
                    is GenericNamedResourceDetail -> body.names
                    else -> null
                }
                val result = names?.find { it.language.name == languageCode }?.name
                    ?: names?.find { it.language.name == "en" }?.name
                    ?: formatApiName(fallbackApiName)

                localizedNamesCache[cacheKey] = result
                return result
            }
        } catch (e: Exception) { }
        return formatApiName(fallbackApiName)
    }

    suspend fun buildEvolutionConditionString(detail: EvolutionDetail): String = coroutineScope {
        val trigger = translateEvolutionTrigger(detail.trigger.name)

        // OPTIMIZACIÓN: Lanzamos todas las peticiones de localización necesarias en PARALELO
        val itemDef = detail.item?.let { async { fetchLocalizedName(it.url, it.name, "item") } }
        val heldItemDef = detail.heldItem?.let { async { fetchLocalizedName(it.url, it.name, "item") } }
        val moveDef = detail.knownMove?.let { async { fetchLocalizedName(it.url, it.name, "move") } }
        val moveTypeDef = detail.knownMoveType?.let { async { fetchLocalizedName(it.url, it.name, "type") } }
        val locationDef = detail.location?.let { async { fetchLocalizedName(it.url, it.name, "location") } }

        var cond = trigger
        detail.minLevel?.let { cond += " $it" }

        itemDef?.await()?.let { cond += "\nUsando $it" }
        heldItemDef?.await()?.let { cond += "\nCon $it equipado" }

        detail.minHappiness?.let { cond += "\nFelicidad mín.: $it" }
        detail.timeOfDay?.takeIf { it.isNotEmpty() }?.let {
            cond += "\nDurante el ${if(it.lowercase() == "day") "día" else "noche"}"
        }

        moveDef?.await()?.let { cond += "\nConociendo $it" }
        moveTypeDef?.await()?.let { cond += "\nConociendo mov. tipo $it" }
        locationDef?.await()?.let { cond += "\nEn $it" }

        detail.gender?.let { cond += "\nSiendo ${if(it == 1) "Hembra" else "Macho"}" }
        if (detail.needsOverworldRain) cond += "\nCon lluvia"
        if (detail.turnUpsideDown) cond += "\nGirando la consola"

        detail.relativePhysicalStats?.let {
            val comp = when {
                it > 0 -> "Ataque > Defensa"
                it < 0 -> "Ataque < Defensa"
                else -> "Ataque = Defensa"
            }
            cond += "\nCon $comp"
        }
        cond.trim()
    }

    private fun translateEvolutionTrigger(trigger: String): String = when (trigger.lowercase()) {
        "level-up" -> "Nivel"; "trade" -> "Intercambio"; "use-item" -> "Objeto"; "shed" -> "Muda"; else -> formatApiName(trigger)
    }

    fun formatApiName(name: String): String = name.split('-').joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

    fun fetchPokemonFormsAndVarieties(pokemonId: Int, speciesName: String) {
        _isLoadingForms.value = true
        viewModelScope.launch {
            try {
                val res = pokemonApiService.getPokemonSpeciesDetailsById(pokemonId)
                if (res.isSuccessful) {
                    val jobs = res.body()?.varieties?.map { v ->
                        async(Dispatchers.IO) {
                            val id = v.pokemon.url.split("/").dropLast(1).last()
                            val dRes = pokemonApiService.getPokemonDetails(id)
                            if (dRes.isSuccessful) {
                                val d = dRes.body()!!
                                DisplayablePokemonVariety(d.id, v.pokemon.name.replace("-", " ").replaceFirstChar(Char::titlecase), d.sprites.other?.officialArtwork?.frontDefault ?: d.sprites.frontDefault, v.isDefault)
                            } else null
                        }
                    }
                    _pokemonFormsAndVarieties.value = jobs?.awaitAll()?.filterNotNull()?.sortedByDescending { it.isDefault }
                }
            } finally { _isLoadingForms.value = false }
        }
    }

    // MEJORA 3: fetchEvolutionChainDetails con deduplicación por URL
    private fun fetchEvolutionChainDetails(url: String) {
        // Si ya tenemos esta cadena en caché, usarla directamente
        evolutionChainCache[url]?.let {
            _evolutionChainDetails.value = it
            return
        }

        _isLoadingEvolutionChain.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getEvolutionChainDetailsByUrl(url) }
                if (res.isSuccessful) {
                    val body = res.body()
                    body?.let { evolutionChainCache[url] = it }
                    _evolutionChainDetails.value = body
                }
            } finally { _isLoadingEvolutionChain.value = false }
        }
    }

    fun fetchGenerations() {
        if (_isLoadingGenerations.value == true || _generations.value?.isNotEmpty() == true) return
        _isLoadingGenerations.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getGenerationList() }
                if (res.isSuccessful) _generations.value = res.body()?.results ?: emptyList()
            } finally { _isLoadingGenerations.value = false }
        }
    }

    private fun NamedApiResource.getGenerationIdFromUrl(): Int? = url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
    private fun handleError(msg: String) { if (!errorShownThisFetch) { _error.postValue(msg); errorShownThisFetch = true } }
    fun clearError() { _error.value = null }
    fun clearPokemonDetails() {
        _pokemonDetails.value = null
        _pokemonDescription.value = null
        _pokemonSpeciesDetails.value = null
        animatedPokemonIds.clear()
    }

    /** Resetea TODO el estado de la ficha al estado inicial (como si la app acabara de abrir) */
    fun resetDetailState() {
        detailFetchJob?.cancel()
        _pokemonDetails.value = null
        _pokemonDescription.value = null
        _pokemonSpeciesDetails.value = null
        _evolutionChainDetails.value = null
        _isLoadingDetails.value = false
        _isLoadingEvolutionChain.value = false
        _wikiDexFlavorTexts.value = emptyMap()
        _wikiDexLocations.value = emptyMap()
        _pokemonEncounters.value = emptyList()
        _navigationList.value = emptyList()
        _evoChainPokemonMap.value = emptyMap()
        animatedPokemonIds.clear()
        selectedDetailSection.value = "DESC"
    }
    fun isFetchingForGenerationId(id: Int?): Boolean = _isLoadingPokemonForCurrentGeneration.value == true && _currentlyFetchingGenerationId.value == id

    // ====================== ENCUENTROS ======================

    private val _pokemonEncounters = MutableStateFlow<List<GameEncounterGroup>>(emptyList())
    val pokemonEncounters: StateFlow<List<GameEncounterGroup>> = _pokemonEncounters.asStateFlow()

    private val _isLoadingEncounters = MutableStateFlow(false)
    val isLoadingEncounters: StateFlow<Boolean> = _isLoadingEncounters.asStateFlow()

    fun fetchPokemonEncounters(pokemonId: Int) {
        _isLoadingEncounters.value = true
        _pokemonEncounters.value = emptyList()
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getPokemonEncounters(pokemonId) }
                if (res.isSuccessful) {
                    val encounters = res.body() ?: emptyList()

                    // Paso 1: recopilar todos los pares (version, location, methods)
                    data class FlatEncounter(
                        val versionName: String,
                        val locationName: String,
                        val maxChance: Int,
                        val methods: List<DisplayableEncounterMethod>
                    )

                    val flat = mutableListOf<FlatEncounter>()
                    for (encounter in encounters) {
                        val locationName = withContext(Dispatchers.IO) {
                            fetchLocalizedName(encounter.locationArea.url, encounter.locationArea.name, "location-area")
                        }
                        for (vd in encounter.versionDetails) {
                            val methods = vd.encounterDetails.map { ed ->
                                DisplayableEncounterMethod(
                                    methodName = translateEncounterMethod(ed.method.name),
                                    minLevel = ed.minLevel,
                                    maxLevel = ed.maxLevel,
                                    chance = ed.chance
                                )
                            }
                            flat.add(FlatEncounter(
                                versionName = translateVersionName(vd.version.name),
                                locationName = locationName,
                                maxChance = vd.maxChance,
                                methods = methods
                            ))
                        }
                    }

                    // Paso 2: agrupar por version
                    val grouped = flat.groupBy { it.versionName }.map { (version, entries) ->
                        GameEncounterGroup(
                            versionName = version,
                            locations = entries.map { e ->
                                GameEncounterLocation(
                                    locationName = e.locationName,
                                    maxChance = e.maxChance,
                                    methods = e.methods
                                )
                            }.sortedByDescending { it.maxChance }
                        )
                    }.sortedBy { it.versionName }

                    _pokemonEncounters.value = grouped
                }
            } catch (e: Exception) {
                handleError("Error cargando encuentros: ${e.message}")
            } finally {
                _isLoadingEncounters.value = false
            }
        }
    }

    private fun translateEncounterMethod(method: String): String = when (method.lowercase()) {
        "walk" -> "Caminando"
        "old-rod" -> "Caña Vieja"
        "good-rod" -> "Caña Buena"
        "super-rod" -> "Supercaña"
        "surf" -> "Surf"
        "rock-smash" -> "Golpe Roca"
        "headbutt" -> "Cabezazo"
        "dark-grass" -> "Hierba oscura"
        "grass-spots" -> "Hierba agitada"
        "cave-spots" -> "Polvo cueva"
        "bridge-spots" -> "Sombras puente"
        "super-rod-spots" -> "Pesca agitada"
        "surf-spots" -> "Agua agitada"
        "yellow-flowers" -> "Flores amarillas"
        "purple-flowers" -> "Flores moradas"
        "red-flowers" -> "Flores rojas"
        "rough-terrain" -> "Terreno escarpado"
        "gift" -> "Regalo"
        "gift-egg" -> "Huevo regalo"
        "only-one" -> "Unico"
        "pokeflute" -> "Pokéflauta"
        "headbutt-low" -> "Cabezazo (baja)"
        "headbutt-normal" -> "Cabezazo (normal)"
        "headbutt-high" -> "Cabezazo (alta)"
        "squirt-bottle" -> "Regadera"
        "berry-piles" -> "Montones de bayas"
        "roaming-grass" -> "Hierba errante"
        "roaming-water" -> "Agua errante"
        else -> formatApiName(method)
    }

    private fun translateVersionName(version: String): String = when (version.lowercase()) {
        "red" -> "Rojo"
        "blue" -> "Azul"
        "yellow" -> "Amarillo"
        "gold" -> "Oro"
        "silver" -> "Plata"
        "crystal" -> "Cristal"
        "ruby" -> "Rubí"
        "sapphire" -> "Zafiro"
        "emerald" -> "Esmeralda"
        "firered" -> "Rojo Fuego"
        "leafgreen" -> "Verde Hoja"
        "diamond" -> "Diamante"
        "pearl" -> "Perla"
        "platinum" -> "Platino"
        "heartgold" -> "Oro HeartGold"
        "soulsilver" -> "Plata SoulSilver"
        "black" -> "Negro"
        "white" -> "Blanco"
        "black-2" -> "Negro 2"
        "white-2" -> "Blanco 2"
        "x" -> "X"
        "y" -> "Y"
        "omega-ruby" -> "Rubí Omega"
        "alpha-sapphire" -> "Zafiro Alfa"
        "sun" -> "Sol"
        "moon" -> "Luna"
        "ultra-sun" -> "Ultra Sol"
        "ultra-moon" -> "Ultra Luna"
        "lets-go-pikachu" -> "Let's Go Pikachu"
        "lets-go-eevee" -> "Let's Go Eevee"
        "sword" -> "Espada"
        "shield" -> "Escudo"
        "brilliant-diamond" -> "Diamante Brillante"
        "shining-pearl" -> "Perla Reluciente"
        "legends-arceus" -> "Leyendas Arceus"
        "scarlet" -> "Escarlata"
        "violet" -> "Púrpura"
        "legends-za" -> "Leyendas Z-A"
        else -> formatApiName(version)
    }

    // ====================== NAVEGADOR DE MOVIMIENTOS ======================

    private val _moveList = MutableStateFlow<List<NamedApiResource>>(emptyList())
    val moveList: StateFlow<List<NamedApiResource>> = _moveList.asStateFlow()

    private val _moveSummaries = MutableStateFlow<Map<Int, MoveSummary>>(emptyMap())
    val moveSummaries: StateFlow<Map<Int, MoveSummary>> = _moveSummaries.asStateFlow()

    private val _isLoadingMoveList = MutableStateFlow(false)
    val isLoadingMoveList: StateFlow<Boolean> = _isLoadingMoveList.asStateFlow()

    private val _isLoadingMoveSummaries = MutableStateFlow(false)
    val isLoadingMoveSummaries: StateFlow<Boolean> = _isLoadingMoveSummaries.asStateFlow()

    fun fetchMoveList() {
        if (_moveSummaries.value.isNotEmpty() || _isLoadingMoveList.value) return
        _isLoadingMoveList.value = true
        viewModelScope.launch {
            try {
                // 1. Intentar cargar desde Room
                val cachedMoves = withContext(Dispatchers.IO) { pokemonDao.getAllMoveSummaries() }
                if (cachedMoves.isNotEmpty()) {
                    val map = cachedMoves.associate { it.id to it.toMoveSummary() }
                    _moveSummaries.value = map
                    _isLoadingMoveList.value = false
                    return@launch
                }

                // 2. Si no hay cache, descargar de la API
                val res = withContext(Dispatchers.IO) { pokemonApiService.getMoveList(limit = 2000) }
                if (res.isSuccessful) {
                    val moves = res.body()?.results ?: emptyList()
                    _moveList.value = moves
                    fetchMoveSummariesBatch(moves)
                }
            } catch (e: Exception) {
                handleError("Error cargando movimientos: ${e.message}")
            } finally {
                _isLoadingMoveList.value = false
            }
        }
    }

    private fun fetchMoveSummariesBatch(moves: List<NamedApiResource>) {
        viewModelScope.launch {
            _isLoadingMoveSummaries.value = true
            val currentMap = _moveSummaries.value.toMutableMap()
            val semaphore = Semaphore(30)

            moves.chunked(50).forEach { chunk ->
                val results = chunk.map { resource ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val id = resource.url.split("/").dropLast(1).lastOrNull()?.toIntOrNull() ?: return@withPermit null
                                if (currentMap.containsKey(id)) return@withPermit null
                                val response = pokemonApiService.getMoveDetailsByUrl(resource.url)
                                if (response.isSuccessful) {
                                    val detail = response.body() ?: return@withPermit null
                                    val localName = detail.names.find { it.language.name == "es" }?.name
                                        ?: detail.names.find { it.language.name == "en" }?.name
                                        ?: formatApiName(detail.name)
                                    val desc = detail.flavorTextEntries
                                        .filter { it.language.name == "es" }
                                        .firstOrNull { it.flavorText.isNotBlank() }?.flavorText
                                        ?: detail.effectEntries.find { it.language.name == "es" }?.shortEffect
                                        ?: detail.effectEntries.find { it.language.name == "en" }?.shortEffect
                                    MoveSummary(
                                        id = detail.id,
                                        name = detail.name,
                                        localizedName = localName,
                                        typeName = detail.moveType?.name,
                                        damageClass = detail.damageClass?.name,
                                        power = detail.power,
                                        pp = detail.pp,
                                        accuracy = detail.accuracy,
                                        description = desc?.replace("\n", " ")?.replace("\u000c", " ")
                                    )
                                } else null
                            } catch (_: Exception) { null }
                        }
                    }
                }.awaitAll().filterNotNull()

                results.forEach { currentMap[it.id] = it }
                _moveSummaries.value = currentMap.toMap()
            }

            // 3. Guardar todo en Room para futuras cargas
            withContext(Dispatchers.IO) {
                val entities = currentMap.values.map {
                    com.david.pokedex_api.api.db.MoveSummaryEntity.fromMoveSummary(it)
                }
                pokemonDao.insertMoveSummaries(entities)
            }

            _isLoadingMoveSummaries.value = false
        }
    }

    // ====================== NAVEGADOR DE ITEMS ======================

    private val _itemSummaries = MutableStateFlow<Map<Int, ItemSummary>>(emptyMap())
    val itemSummaries: StateFlow<Map<Int, ItemSummary>> = _itemSummaries.asStateFlow()

    private val _isLoadingItems = MutableStateFlow(false)
    val isLoadingItems: StateFlow<Boolean> = _isLoadingItems.asStateFlow()

    fun fetchItemList() {
        if (_itemSummaries.value.isNotEmpty() || _isLoadingItems.value) return
        _isLoadingItems.value = true
        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) { pokemonDao.getAllItemSummaries() }
                if (cached.isNotEmpty()) {
                    _itemSummaries.value = cached.associate { it.id to it.toItemSummary() }
                    _isLoadingItems.value = false
                    return@launch
                }

                val res = withContext(Dispatchers.IO) { pokemonApiService.getItemList(limit = 2000) }
                if (res.isSuccessful) {
                    val items = res.body()?.results ?: emptyList()
                    val currentMap = mutableMapOf<Int, ItemSummary>()
                    val semaphore = Semaphore(30)

                    items.chunked(50).forEach { chunk ->
                        val results = chunk.map { resource ->
                            async(Dispatchers.IO) {
                                semaphore.withPermit {
                                    try {
                                        val response = pokemonApiService.getItemDetailsByUrl(resource.url)
                                        if (response.isSuccessful) {
                                            val d = response.body() ?: return@withPermit null
                                            val localName = d.names.find { it.language.name == "es" }?.name
                                                ?: d.names.find { it.language.name == "en" }?.name
                                                ?: formatApiName(d.name)
                                            val effect = d.flavorTextEntries
                                                ?.filter { it.language.name == "es" }
                                                ?.firstOrNull { it.text.isNotBlank() }?.text
                                                ?: d.effectEntries?.find { it.language.name == "es" }?.shortEffect
                                                ?: d.effectEntries?.find { it.language.name == "en" }?.shortEffect
                                            val catName = d.category?.name?.let { formatApiName(it) }
                                            ItemSummary(
                                                id = d.id, name = d.name, localizedName = localName,
                                                category = catName, cost = d.cost,
                                                effect = effect?.replace("\n", " ")?.replace("\u000c", " "),
                                                spriteUrl = d.sprites?.default
                                            )
                                        } else null
                                    } catch (_: Exception) { null }
                                }
                            }
                        }.awaitAll().filterNotNull()
                        results.forEach { currentMap[it.id] = it }
                        _itemSummaries.value = currentMap.toMap()
                    }

                    withContext(Dispatchers.IO) {
                        pokemonDao.insertItemSummaries(currentMap.values.map {
                            com.david.pokedex_api.api.db.ItemSummaryEntity.from(it)
                        })
                    }
                }
            } catch (e: Exception) { handleError("Error cargando items: ${e.message}") }
            finally { _isLoadingItems.value = false }
        }
    }

    // ====================== NAVEGADOR DE BAYAS ======================

    private val _berrySummaries = MutableStateFlow<Map<Int, BerrySummary>>(emptyMap())
    val berrySummaries: StateFlow<Map<Int, BerrySummary>> = _berrySummaries.asStateFlow()

    private val _isLoadingBerries = MutableStateFlow(false)
    val isLoadingBerries: StateFlow<Boolean> = _isLoadingBerries.asStateFlow()

    fun fetchBerryList() {
        if (_berrySummaries.value.isNotEmpty() || _isLoadingBerries.value) return
        _isLoadingBerries.value = true
        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) { pokemonDao.getAllBerrySummaries() }
                if (cached.isNotEmpty()) {
                    _berrySummaries.value = cached.associate { it.id to it.toBerrySummary() }
                    _isLoadingBerries.value = false
                    return@launch
                }

                val res = withContext(Dispatchers.IO) { pokemonApiService.getBerryList(limit = 100) }
                if (res.isSuccessful) {
                    val berries = res.body()?.results ?: emptyList()
                    val currentMap = mutableMapOf<Int, BerrySummary>()
                    val semaphore = Semaphore(20)

                    berries.map { resource ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    val bRes = pokemonApiService.getBerryDetailsByUrl(resource.url)
                                    if (bRes.isSuccessful) {
                                        val b = bRes.body() ?: return@withPermit null
                                        // Get item details for localized name and sprite
                                        val iRes = pokemonApiService.getItemDetailsByUrl(b.item.url)
                                        val item = iRes.body()
                                        val localName = item?.names?.find { it.language.name == "es" }?.name
                                            ?: item?.names?.find { it.language.name == "en" }?.name
                                            ?: formatApiName(b.name)
                                        val flavors = b.flavors?.associate {
                                            translateBerryFlavor(it.flavor.name) to it.potency
                                        } ?: emptyMap()
                                        BerrySummary(
                                            id = b.id, name = b.name, localizedName = localName,
                                            naturalGiftType = b.naturalGiftType?.name,
                                            naturalGiftPower = b.naturalGiftPower,
                                            growthTime = b.growthTime, size = b.size,
                                            smoothness = b.smoothness, maxHarvest = b.maxHarvest,
                                            spriteUrl = item?.sprites?.default,
                                            flavors = flavors
                                        )
                                    } else null
                                } catch (_: Exception) { null }
                            }
                        }
                    }.awaitAll().filterNotNull().forEach { currentMap[it.id] = it }

                    _berrySummaries.value = currentMap.toMap()
                    withContext(Dispatchers.IO) {
                        pokemonDao.insertBerrySummaries(currentMap.values.map {
                            com.david.pokedex_api.api.db.BerrySummaryEntity.from(it)
                        })
                    }
                }
            } catch (e: Exception) { handleError("Error cargando bayas: ${e.message}") }
            finally { _isLoadingBerries.value = false }
        }
    }

    private fun translateBerryFlavor(name: String): String = when (name.lowercase()) {
        "spicy" -> "Picante"
        "dry" -> "Seco"
        "sweet" -> "Dulce"
        "bitter" -> "Amargo"
        "sour" -> "Acido"
        else -> formatApiName(name)
    }

    // ====================== NAVEGADOR DE REGIONES ======================

    private val _regions = MutableStateFlow<List<DisplayableRegion>>(emptyList())
    val regions: StateFlow<List<DisplayableRegion>> = _regions.asStateFlow()

    private val _isLoadingRegions = MutableStateFlow(false)
    val isLoadingRegions: StateFlow<Boolean> = _isLoadingRegions.asStateFlow()

    private val _regionLocations = MutableStateFlow<List<DisplayableLocation>>(emptyList())
    val regionLocations: StateFlow<List<DisplayableLocation>> = _regionLocations.asStateFlow()

    private val _isLoadingLocations = MutableStateFlow(false)
    val isLoadingLocations: StateFlow<Boolean> = _isLoadingLocations.asStateFlow()

    private val _locationAreas = MutableStateFlow<List<DisplayableLocationArea>>(emptyList())
    val locationAreas: StateFlow<List<DisplayableLocationArea>> = _locationAreas.asStateFlow()

    private val _isLoadingAreas = MutableStateFlow(false)
    val isLoadingAreas: StateFlow<Boolean> = _isLoadingAreas.asStateFlow()

    fun fetchRegions() {
        if (_regions.value.isNotEmpty() || _isLoadingRegions.value) return
        _isLoadingRegions.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getRegionList() }
                if (res.isSuccessful) {
                    val regionResources = res.body()?.results ?: emptyList()
                    val semaphore = Semaphore(10)
                    val displayable = regionResources.map { resource ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    val detail = pokemonApiService.getRegionDetailsByUrl(resource.url)
                                    if (detail.isSuccessful) {
                                        val d = detail.body() ?: return@withPermit null
                                        val localName = d.names.find { it.language.name == "es" }?.name
                                            ?: d.names.find { it.language.name == "en" }?.name
                                            ?: formatApiName(d.name)
                                        val gen = d.mainGeneration?.name?.let { formatApiName(it) }
                                        DisplayableRegion(d.id, d.name, localName, gen, d.locations.size)
                                    } else null
                                } catch (_: Exception) { null }
                            }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.id }
                    _regions.value = displayable
                }
            } catch (e: Exception) { handleError("Error cargando regiones: ${e.message}") }
            finally { _isLoadingRegions.value = false }
        }
    }

    fun fetchLocationsForRegion(regionName: String) {
        _isLoadingLocations.value = true
        _regionLocations.value = emptyList()
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    pokemonApiService.getRegionDetailsByUrl("https://pokeapi.co/api/v2/region/$regionName/")
                }
                if (res.isSuccessful) {
                    val locations = res.body()?.locations ?: emptyList()
                    val semaphore = Semaphore(20)
                    val displayable = locations.map { resource ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    val detail = pokemonApiService.getLocationDetailsByUrl(resource.url)
                                    if (detail.isSuccessful) {
                                        val d = detail.body() ?: return@withPermit null
                                        val localName = d.names.find { it.language.name == "es" }?.name
                                            ?: d.names.find { it.language.name == "en" }?.name
                                            ?: formatApiName(d.name)
                                        val id = resource.url.split("/").dropLast(1).lastOrNull()?.toIntOrNull() ?: 0
                                        DisplayableLocation(id, d.name, localName, d.areas?.size ?: 0)
                                    } else null
                                } catch (_: Exception) { null }
                            }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.localizedName }
                    _regionLocations.value = displayable
                }
            } catch (e: Exception) { handleError("Error cargando ubicaciones: ${e.message}") }
            finally { _isLoadingLocations.value = false }
        }
    }

    fun fetchLocationAreas(locationName: String) {
        _isLoadingAreas.value = true
        _locationAreas.value = emptyList()
        viewModelScope.launch {
            try {
                val locRes = withContext(Dispatchers.IO) {
                    pokemonApiService.getLocationDetailsByUrl("https://pokeapi.co/api/v2/location/$locationName/")
                }
                if (locRes.isSuccessful) {
                    val areas = locRes.body()?.areas ?: emptyList()
                    val semaphore = Semaphore(10)
                    val displayable = areas.map { areaResource ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    val areaRes = pokemonApiService.getLocationAreaDetailsByUrl(areaResource.url)
                                    if (areaRes.isSuccessful) {
                                        val a = areaRes.body() ?: return@withPermit null
                                        val localName = a.names.find { it.language.name == "es" }?.name
                                            ?: a.names.find { it.language.name == "en" }?.name
                                            ?: formatApiName(a.name)
                                        val pokemon = a.pokemonEncounters?.flatMap { enc ->
                                            enc.versionDetails.flatMap { vd ->
                                                vd.encounterDetails.map { ed ->
                                                    val pokId = enc.pokemon.url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
                                                    DisplayableAreaPokemon(
                                                        pokemonName = formatApiName(enc.pokemon.name),
                                                        spriteUrl = if (pokId != null) "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$pokId.png" else "",
                                                        maxChance = vd.maxChance,
                                                        minLevel = ed.minLevel,
                                                        maxLevel = ed.maxLevel,
                                                        method = translateEncounterMethod(ed.method.name)
                                                    )
                                                }
                                            }
                                        }?.distinctBy { "${it.pokemonName}_${it.method}" }?.sortedByDescending { it.maxChance } ?: emptyList()
                                        DisplayableLocationArea(a.name, localName, pokemon)
                                    } else null
                                } catch (_: Exception) { null }
                            }
                        }
                    }.awaitAll().filterNotNull()
                    _locationAreas.value = displayable
                }
            } catch (e: Exception) { handleError("Error cargando areas: ${e.message}") }
            finally { _isLoadingAreas.value = false }
        }
    }

    // ====================== NATURALEZAS ======================

    private val _natures = MutableStateFlow<List<DisplayableNature>>(emptyList())
    val natures: StateFlow<List<DisplayableNature>> = _natures.asStateFlow()

    private val _isLoadingNatures = MutableStateFlow(false)
    val isLoadingNatures: StateFlow<Boolean> = _isLoadingNatures.asStateFlow()

    fun fetchNatures() {
        if (_natures.value.isNotEmpty() || _isLoadingNatures.value) return
        _isLoadingNatures.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getNatureList() }
                if (res.isSuccessful) {
                    val resources = res.body()?.results ?: emptyList()
                    val semaphore = Semaphore(10)
                    val displayable = resources.map { resource ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                try {
                                    val detail = pokemonApiService.getNatureDetailsByUrl(resource.url)
                                    if (detail.isSuccessful) {
                                        val d = detail.body() ?: return@withPermit null
                                        val localName = d.names.find { it.language.name == "es" }?.name
                                            ?: d.names.find { it.language.name == "en" }?.name
                                            ?: formatApiName(d.name)
                                        DisplayableNature(
                                            id = d.id, name = d.name, localizedName = localName,
                                            increasedStat = d.increasedStat?.name?.let { formatStatName(it) },
                                            decreasedStat = d.decreasedStat?.name?.let { formatStatName(it) }
                                        )
                                    } else null
                                } catch (_: Exception) { null }
                            }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.id }
                    _natures.value = displayable
                }
            } catch (e: Exception) { handleError("Error cargando naturalezas: ${e.message}") }
            finally { _isLoadingNatures.value = false }
        }
    }

    private fun formatStatName(statName: String): String = when (statName.lowercase()) {
        "hp" -> "PS"
        "attack" -> "Ataque"
        "defense" -> "Defensa"
        "special-attack" -> "At. Esp."
        "special-defense" -> "Def. Esp."
        "speed" -> "Velocidad"
        else -> formatApiName(statName)
    }

    // ====================== CONCURSOS ======================

    private val _contestTypes = MutableStateFlow<List<DisplayableContestType>>(emptyList())
    val contestTypes: StateFlow<List<DisplayableContestType>> = _contestTypes.asStateFlow()

    private val _isLoadingContests = MutableStateFlow(false)
    val isLoadingContests: StateFlow<Boolean> = _isLoadingContests.asStateFlow()

    fun fetchContestTypes() {
        if (_contestTypes.value.isNotEmpty() || _isLoadingContests.value) return
        _isLoadingContests.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getContestTypeList() }
                if (res.isSuccessful) {
                    val resources = res.body()?.results ?: emptyList()
                    val displayable = resources.map { resource ->
                        async(Dispatchers.IO) {
                            try {
                                val detail = pokemonApiService.getContestTypeDetailsByUrl(resource.url)
                                if (detail.isSuccessful) {
                                    val d = detail.body() ?: return@async null
                                    val esName = d.names.find { it.language.name == "es" }
                                    val enName = d.names.find { it.language.name == "en" }
                                    DisplayableContestType(
                                        id = d.id, name = d.name,
                                        localizedName = esName?.name ?: enName?.name ?: formatApiName(d.name),
                                        color = esName?.color ?: enName?.color ?: "",
                                        berryFlavor = d.berryFlavor?.name?.let { translateBerryFlavor(it) }
                                    )
                                } else null
                            } catch (_: Exception) { null }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.id }
                    _contestTypes.value = displayable
                }
            } catch (e: Exception) { handleError("Error cargando concursos: ${e.message}") }
            finally { _isLoadingContests.value = false }
        }
    }
}
