package com.david.pokedex_api.api.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.david.pokedex_api.api.client.RetrofitClient
import com.david.pokedex_api.api.model.DisplayablePokemonVariety
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.EvolutionDetail
import com.david.pokedex_api.api.model.GenericNamedResourceDetail
import com.david.pokedex_api.api.model.ItemDetailResponse
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.NameEntry
import com.david.pokedex_api.api.model.NamedApiResource
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonFormDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
import com.david.pokedex_api.api.model.PokemonSummary
import com.david.pokedex_api.api.model.TypeDetailResponse
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
import kotlinx.coroutines.withContext
import retrofit2.Response

class PokemonViewModel : ViewModel() {

    val pokemonApiService: PokeApiService = RetrofitClient.instance

    private val _pokemonDetails = MutableLiveData<PokemonDetailResponse?>()
    val pokemonDetails: LiveData<PokemonDetailResponse?> = _pokemonDetails

    private val _pokemonDescription = MutableLiveData<String?>()
    val pokemonDescription: LiveData<String?> = _pokemonDescription

    private val _isLoadingDetails = MutableLiveData<Boolean>(false)
    val isLoadingDetails: LiveData<Boolean> = _isLoadingDetails

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
    private val isLoadingPokemonByGenerationMap = mutableMapOf<Int, Boolean>()

    private val _pokemonFormsAndVarieties = MutableLiveData<List<DisplayablePokemonVariety>>()
    val pokemonFormsAndVarieties: LiveData<List<DisplayablePokemonVariety>> = _pokemonFormsAndVarieties

    private val _isLoadingForms = MutableLiveData<Boolean>(false)
    val isLoadingForms: LiveData<Boolean> = _isLoadingForms

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

    // --- CARGA OPTIMIZADA POR BLOQUES ---
    fun fetchPokemonForGeneration(generationId: Int?, forceRefresh: Boolean = false) {
        if (generationId == null) return
        if (!forceRefresh && _pokemonByGenerationCache.value?.containsKey(generationId) == true &&
            _pokemonByGenerationCache.value?.get(generationId)?.isNotEmpty() == true
        ) return

        if (isLoadingPokemonByGenerationMap[generationId] == true && !forceRefresh) return

        isLoadingPokemonByGenerationMap[generationId] = true
        _isLoadingPokemonForCurrentGeneration.value = true
        errorShownThisFetch = false

        viewModelScope.launch {
            val loadedSummaries = mutableListOf<PokemonSummary>()
            try {
                val response = withContext(Dispatchers.IO) { pokemonApiService.getGenerationDetails(generationId) }
                if (response.isSuccessful) {
                    val speciesList = response.body()?.pokemonSpecies ?: emptyList()
                    
                    // Paralelismo por grupos de 20 para máxima velocidad
                    speciesList.chunked(20).forEach { chunk ->
                        val deferredSummaries = chunk.map { speciesResource ->
                            async(Dispatchers.IO) { fetchSinglePokemonSummary(speciesResource) }
                        }
                        loadedSummaries.addAll(deferredSummaries.awaitAll().filterNotNull())
                        
                        val currentCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        currentCache[generationId] = loadedSummaries.toList().sortedBy { it.id }
                        _pokemonByGenerationCache.postValue(currentCache)
                    }
                }
            } catch (e: Exception) {
                handleError("Error: ${e.message}")
            } finally {
                isLoadingPokemonByGenerationMap[generationId] = false
                if (isLoadingPokemonByGenerationMap.none { it.value }) {
                    _isLoadingPokemonForCurrentGeneration.postValue(false)
                }
            }
        }
    }

    private suspend fun fetchSinglePokemonSummary(speciesResource: NamedApiResource): PokemonSummary? = coroutineScope {
        val id = speciesResource.url.split("/").dropLast(1).lastOrNull() ?: return@coroutineScope null
        try {
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

    // --- MÉTODOS DE FICHA Y EVOLUCIÓN (RESTAURADOS) ---

    fun fetchPokemonDetailsByName(name: String, lang: String) {
        if (_isLoadingDetails.value == true && _pokemonDetails.value?.name?.equals(name, true) == true) return
        _isLoadingDetails.value = true
        _pokemonDetails.value = null
        _pokemonDescription.value = null
        viewModelScope.launch {
            try {
                val dDef = async(Dispatchers.IO) { pokemonApiService.getPokemonDetails(name.lowercase().trim()) }
                val sDef = async(Dispatchers.IO) { pokemonApiService.getPokemonSpeciesDetails(name.lowercase().trim()) }
                val dRes = dDef.await()
                val sRes = sDef.await()

                if (sRes.isSuccessful) {
                    val species = sRes.body()
                    _pokemonSpeciesDetails.value = species
                    species?.let {
                        val preferred = listOf("sword", "shield", "scarlet", "violet", "legends-arceus")
                        var desc = it.flavorTextEntries.filter { f -> f.language.name == lang }
                            .firstOrNull { f -> preferred.any { p -> f.version.name.contains(p, true) } }?.flavorText
                            ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == lang }?.flavorText
                            ?: it.flavorTextEntries.firstOrNull { f -> f.language.name == "en" }?.flavorText

                        _pokemonDescription.value = desc?.replace("\n", " ")?.replace("\u000c", " ")?.replace("POKéMON", "Pokémon")
                        it.evolutionChain?.url?.let { url -> fetchEvolutionChainDetails(url) }
                    }
                }
                if (dRes.isSuccessful) _pokemonDetails.value = dRes.body()
            } catch (e: Exception) { handleError(e.message ?: "Error") }
            finally { _isLoadingDetails.value = false }
        }
    }

    internal suspend fun fetchLocalizedName(resourceUrl: String, fallbackApiName: String, resourceTypeHint: String, languageCode: String = "es"): String {
        if (resourceUrl.isBlank()) return formatApiName(fallbackApiName)
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
                return names?.find { it.language.name == languageCode }?.name ?: names?.find { it.language.name == "en" }?.name ?: formatApiName(fallbackApiName)
            }
        } catch (e: Exception) { }
        return formatApiName(fallbackApiName)
    }

    suspend fun buildEvolutionConditionString(detail: EvolutionDetail): String {
        var cond = translateEvolutionTrigger(detail.trigger.name)
        detail.minLevel?.let { cond += " $it" }
        detail.item?.let { cond += "\nUsando ${fetchLocalizedName(it.url, it.name, "item")}" }
        detail.heldItem?.let { cond += "\nCon ${fetchLocalizedName(it.url, it.name, "item")} equipado" }
        detail.minHappiness?.let { cond += "\nFelicidad mín.: $it" }
        detail.timeOfDay?.takeIf { it.isNotEmpty() }?.let { cond += "\nDurante el ${if(it.lowercase()=="day") "día" else "noche"}" }
        detail.knownMove?.let { cond += "\nConociendo ${fetchLocalizedName(it.url, it.name, "move")}" }
        detail.knownMoveType?.let { cond += "\nConociendo mov. tipo ${fetchLocalizedName(it.url, it.name, "type")}" }
        detail.location?.let { cond += "\nEn ${fetchLocalizedName(it.url, it.name, "location")}" }
        detail.gender?.let { cond += "\nSiendo ${if(it==1) "Hembra" else "Macho"}" }
        if (detail.needsOverworldRain) cond += "\nCon lluvia"
        if (detail.turnUpsideDown) cond += "\nGirando la consola"
        detail.relativePhysicalStats?.let {
            val comp = when { it > 0 -> "Ataque > Defensa"; it < 0 -> "Ataque < Defensa"; else -> "Ataque = Defensa" }
            cond += "\nCon $comp"
        }
        return cond.trim()
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

    private fun fetchEvolutionChainDetails(url: String) {
        _isLoadingEvolutionChain.value = true
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { pokemonApiService.getEvolutionChainDetailsByUrl(url) }
                if (res.isSuccessful) _evolutionChainDetails.value = res.body()
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
