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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap

class PokemonViewModel : ViewModel() {

    val pokemonApiService: PokeApiService = RetrofitClient.instance
    private val pokemonDao = DexterApplication.database.pokemonDao()

    private val _pokemonDetails = MutableLiveData<PokemonDetailResponse?>()
    val pokemonDetails: LiveData<PokemonDetailResponse?> = _pokemonDetails

    private val _pokemonDescription = MutableLiveData<String?>()
    val pokemonDescription: LiveData<String?> = _pokemonDescription

    private val _isLoadingDetails = MutableLiveData<Boolean>(false)
    val isLoadingDetails: LiveData<Boolean> = _isLoadingDetails

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
        _isLoadingDetails.value = true
        _pokemonDetails.value = null
        _pokemonDescription.value = null

        viewModelScope.launch {
            try {
                // Paralelismo en las llamadas de detalle
                val dDef = async(Dispatchers.IO) { pokemonApiService.getPokemonDetails(name.lowercase().trim()) }
                val sDef = async(Dispatchers.IO) { pokemonApiService.getPokemonSpeciesDetails(name.lowercase().trim()) }
                val dRes = dDef.await()
                val sRes = sDef.await()

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
                    }
                }

                if (dRes.isSuccessful) {
                    val details = dRes.body()
                    _pokemonDetails.value = details
                    // Carga masiva y reactiva de movimientos
                    details?.let { fetchMovesDetailsParallel(it.moves) }
                }
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
    fun isFetchingForGenerationId(id: Int?): Boolean = _isLoadingPokemonForCurrentGeneration.value == true && _currentlyFetchingGenerationId.value == id
}
