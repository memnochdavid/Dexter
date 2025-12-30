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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

//----- FUNCIONA
class PokemonViewModel : ViewModel() {

    val pokemonApiService: PokeApiService = RetrofitClient.instance

    private val _pokemonDetails = MutableLiveData<PokemonDetailResponse?>()
    val pokemonDetails: LiveData<PokemonDetailResponse?> = _pokemonDetails

    private val _pokemonDescription = MutableLiveData<String?>()
    val pokemonDescription: LiveData<String?> = _pokemonDescription

    private val _isLoadingDetails = MutableLiveData<Boolean>(false)
    val isLoadingDetails: LiveData<Boolean> = _isLoadingDetails

    // --- Para las Generaciones y la Lista de Pokémon por Generación ---
    private val _generations = MutableLiveData<List<NamedApiResource>>(emptyList())
    val generations: LiveData<List<NamedApiResource>> = _generations

    private val _pokemonByGenerationCache =
        MutableLiveData<Map<Int, List<PokemonSummary>>>(emptyMap())
    val pokemonByGenerationCache: LiveData<Map<Int, List<PokemonSummary>>> =
        _pokemonByGenerationCache

    private val _isLoadingPokemonForCurrentGeneration = MutableLiveData<Boolean>(false)
    val isLoadingPokemonForCurrentGeneration: LiveData<Boolean> =
        _isLoadingPokemonForCurrentGeneration

    private val _isLoadingGenerations = MutableLiveData<Boolean>(false)
    val isLoadingGenerations: LiveData<Boolean> = _isLoadingGenerations

    private val _evolutionChainDetails = MutableLiveData<EvolutionChainDetailResponse?>()
    val evolutionChainDetails: LiveData<EvolutionChainDetailResponse?> = _evolutionChainDetails

    // ***** AÑADIR ESTAS LÍNEAS *****
    private val _isLoadingEvolutionChain = MutableLiveData<Boolean>(false)
    val isLoadingEvolutionChain: LiveData<Boolean> = _isLoadingEvolutionChain
    // *******************************

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private var errorShownThisFetch = false

    private val _pokemonSpeciesDetails = MutableLiveData<PokemonSpeciesResponse?>()
    val pokemonSpeciesDetails: LiveData<PokemonSpeciesResponse?> = _pokemonSpeciesDetails

    private val _pokemonTypes = MutableLiveData<List<String>>(ALL_POKEMON_TYPES) // Inicializa con tu constante
    val pokemonTypes: LiveData<List<String>> = _pokemonTypes

    private val _areAllPokemonDetailsAttempted = MutableStateFlow(false)
    val areAllPokemonDetailsAttempted: StateFlow<Boolean> = _areAllPokemonDetailsAttempted.asStateFlow()

    private val _totalGenerationsCount =
        MutableStateFlow(0) // Para saber cuántas generaciones esperamos

    private val _currentlyFetchingGenerationId = MutableLiveData<Int?>(null)



    init {
        // Observar cambios en las generaciones y el caché para actualizar _areAllPokemonDetailsAttempted
        combine(
            generations.asFlow(), // Necesitarás convertir LiveData a Flow o usar otra LiveData
            pokemonByGenerationCache.asFlow(),
            _isLoadingPokemonForCurrentGeneration.asFlow() // Para no marcar como "todo cargado" mientras algo individual aún carga
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

            // Consideramos que todo está "intentado" si todas las generaciones tienen una entrada en el caché
            // Y no hay una carga individual de Pokémon de generación en curso.
            // Esto es una simplificación; podrías querer una lógica más robusta
            // para rastrear si cada fetchPokemonForGeneration individual ha terminado.
            allAttempted && !isLoadingPokemon
        }.onEach { allAttemptedValue ->
            _areAllPokemonDetailsAttempted.value = allAttemptedValue
            if (allAttemptedValue) {
                Log.d("PokemonViewModel", "All Pokemon details from known generations have been attempted.")
            }
        }.launchIn(viewModelScope)
    }
    fun fetchPokemonDetailsByName(name: String, lang: String) {
        if (_isLoadingDetails.value == true && _pokemonDetails.value?.name?.equals(
                name,
                ignoreCase = true
            ) == true
        ) {
            return
        }

        _isLoadingDetails.value = true
        _pokemonDetails.value = null
        _pokemonDescription.value = null
        errorShownThisFetch = false

        val pokemonNameLower = name.lowercase().trim()

        viewModelScope.launch {
            try {
                val detailResponseDeferred = async(Dispatchers.IO) {
                    RetrofitClient.instance.getPokemonDetails(pokemonNameLower)
                }
                val speciesResponseDeferred = async(Dispatchers.IO) {
                    RetrofitClient.instance.getPokemonSpeciesDetails(pokemonNameLower)
                }

                val detailResponse = detailResponseDeferred.await()
                val speciesResponse = speciesResponseDeferred.await()

                if (speciesResponse.isSuccessful) {
                    val speciesData = speciesResponse.body()
                    _pokemonSpeciesDetails.value = speciesData

                    if (speciesData != null) {
                        val preferredVersions = listOf(
                            "sword", "shield", "scarlet", "violet", "legends-arceus",
                            "ultra-sun", "alpha-sapphire"
                        )

                        // --- INICIO DE LA LÓGICA MODIFICADA PARA LA DESCRIPCIÓN ---

                        var descriptionText: String? = null

                        // 1. Intentar obtener la descripción en el idioma principal (lang)
                        val primaryLangEntries = speciesData.flavorTextEntries.filter { it.language.name == lang }
                        val entryFromPreferredPrimary = primaryLangEntries.firstOrNull { entry ->
                            preferredVersions.any { version ->
                                entry.version.name.contains(version, ignoreCase = true) // Añadir ignoreCase por si acaso
                            }
                        }
                        descriptionText = entryFromPreferredPrimary?.flavorText
                            ?: primaryLangEntries.firstOrNull()?.flavorText

                        // 2. Si no se encontró o está vacía, intentar con inglés ("en")
                        if (descriptionText.isNullOrBlank() && lang != "en") { // Solo buscar en inglés si el idioma principal no es inglés
                            val englishLangEntries = speciesData.flavorTextEntries.filter { it.language.name == "en" }
                            val entryFromPreferredEnglish = englishLangEntries.firstOrNull { entry ->
                                preferredVersions.any { version ->
                                    entry.version.name.contains(version, ignoreCase = true)
                                }
                            }
                            descriptionText = entryFromPreferredEnglish?.flavorText
                                ?: englishLangEntries.firstOrNull()?.flavorText
                        }

                        // --- FIN DE LA LÓGICA MODIFICADA ---

                        _pokemonDescription.value = descriptionText
                            ?.replace("\n", " ")
                            ?.replace("\u000c", " ") // \f (form feed)
                            ?.replace("POKéMON", "Pokémon")

                        speciesData.evolutionChain?.url?.let { evolutionUrl ->
                            if (evolutionUrl.isNotBlank()) {
                                fetchEvolutionChainDetails(evolutionUrl)
                            } else {
                                _evolutionChainDetails.value = null
                                _isLoadingEvolutionChain.value = false
                            }
                        } ?: run {
                            _evolutionChainDetails.value = null
                            _isLoadingEvolutionChain.value = false
                        }
                    } else {
                        _pokemonDescription.value = null // Asegurar que la descripción sea nula si speciesData es nulo
                    }
                } else {
                    _pokemonSpeciesDetails.value = null
                    _pokemonDescription.value = null
                    if (detailResponse.isSuccessful) {
//                        handleError("Species Error: ${speciesResponse.code()} ${speciesResponse.message()}")
                    }
                }

                if (detailResponse.isSuccessful) {
                    _pokemonDetails.value = detailResponse.body()
                    if (detailResponse.body() == null) {
                        handleError("Error: Pokémon data not found (details).")
                    }
                } else {
                    val errorBody = detailResponse.errorBody()?.string() ?: "Unknown detail error"
                    handleError("Details Error: ${detailResponse.code()} ${detailResponse.message()}")
                }

                if (detailResponse.isSuccessful && !errorShownThisFetch) {
                    _error.value = null
                }

            } catch (e: Exception) {
                handleError("Exception: ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingDetails.value = false
            }
        }
    }

    private fun handleError(errorMessage: String) {
        if (!errorShownThisFetch) {
            _error.value = errorMessage
            errorShownThisFetch = true
        }
    }

    fun clearError() {
        _error.value = null
    }


    fun fetchGenerations() {
        if (_isLoadingGenerations.value == true || _generations.value?.isNotEmpty() == true) return
        _isLoadingGenerations.value = true
        _areAllPokemonDetailsAttempted.value = false // Resetear al buscar nuevas generaciones
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getGenerationList()
                }
                if (response.isSuccessful) {
                    val generationList = response.body()?.results ?: emptyList()
                    _generations.value = generationList
                    _totalGenerationsCount.value = generationList.size // Actualizar el conteo total esperado
                    if (generationList.isEmpty()) { // Si no hay generaciones, consideramos que "todo" está cargado (vacío)
                        _areAllPokemonDetailsAttempted.value = true
                    }
                } else {
                    handleError("Error fetching generations: ${response.code()}")
                    _areAllPokemonDetailsAttempted.value = true // Error, no más por cargar
                }
            } catch (e: Exception) {
                handleError("Exception (generations): ${e.message ?: "Unknown exception"}")
                _areAllPokemonDetailsAttempted.value = true // Error, no más por cargar
            } finally {
                _isLoadingGenerations.value = false
            }
        }
    }

    private fun NamedApiResource.getGenerationIdFromUrl(): Int? {
        return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
    }

    //-------------------------
/*
    // --- Carga todo de golpe ---
    fun fetchPokemonForGeneration(generationId: Int?, forceRefresh: Boolean = false) {
        if (generationId == null) {
            _error.value = "Invalid generation ID."
            _isLoadingPokemonForCurrentGeneration.value = false
            return
        }

        if (!forceRefresh && _pokemonByGenerationCache.value?.containsKey(generationId) == true) {
            if (_pokemonByGenerationCache.value?.get(generationId)?.isNotEmpty() == true) {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
            return
        }

        if (_isLoadingPokemonForCurrentGeneration.value == true && !forceRefresh) {
            return
        }

        _isLoadingPokemonForCurrentGeneration.value = true
        errorShownThisFetch = false

        viewModelScope.launch {
            try {
                val generationDetailResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getGenerationDetails(generationId)
                }

                if (generationDetailResponse.isSuccessful) {
                    val generationData = generationDetailResponse.body()
                    val pokemonSpeciesListFromGen = generationData?.pokemonSpecies ?: emptyList()
                    if (pokemonSpeciesListFromGen.isNotEmpty()) {
                        val pokemonSummariesDeferred =
                            pokemonSpeciesListFromGen.map { speciesResource ->
                                async(Dispatchers.IO) { // Este es el inicio del bloque async para cada Pokémon
                                    val originalPokemonName = speciesResource.name
                                    val speciesUrl = speciesResource.url
                                    var localizedName = originalPokemonName
                                    var pokemonColor: String? = null
                                    var pokemonSpriteUrl: String? = null
                                    var pokemonTypesList: List<String> = emptyList()
                                    var finalPokemonId: Int? = null

                                    val idFromSpeciesUrl = speciesUrl.split("/")
                                        .dropLastWhile { it.isEmpty() }
                                        .lastOrNull()
                                        ?.toIntOrNull()

                                    try {
                                        if (idFromSpeciesUrl == null) {
                                            Log.e("PokemonViewModel", "Could not parse ID from species URL: $speciesUrl for $originalPokemonName (Gen: $generationId). Skipping.")
                                            return@async null
                                        }

                                        // Variable para almacenar el identificador (ID o nombre) que se usará para getPokemonDetails.
                                        // Inicialmente, es el ID de la especie, pero podría actualizarse al ID de la forma "default".
                                        var resourceIdentifierForDetails = idFromSpeciesUrl.toString()

                                        // 1. Fetch Species Details (para nombre localizado, color y para encontrar la variedad 'default')
                                        val speciesDetailsResponse =
                                            RetrofitClient.instance.getPokemonSpeciesDetails(idFromSpeciesUrl.toString())

                                        if (speciesDetailsResponse.isSuccessful) {
                                            speciesDetailsResponse.body()?.let { speciesDetails ->
                                                // Obtener nombre localizado
                                                speciesDetails.localizedNames.firstOrNull { it.language.name == "es" }?.name?.let {
                                                    localizedName = it
                                                }
                                                // Considera fallback a inglés aquí si es necesario
                                                // ej: localizedName = speciesDetails.localizedNames.firstOrNull { it.language.name == "en" }?.name ?: originalPokemonName

                                                pokemonColor = speciesDetails.color?.name

                                                // OPCIONAL PERO MÁS ROBUSTO:
                                                // Encuentra la variedad marcada como "default" y usa su URL/ID para getPokemonDetails.
                                                // La PokeAPI a veces tiene múltiples "Pokémon" (con diferentes IDs/URLs en /pokemon/)
                                                // para una única "Especie". Esta lógica asegura que usas la forma principal.
                                                val defaultVariety = speciesDetails.varieties.firstOrNull { it.isDefault }
                                                if (defaultVariety != null) {
                                                    val defaultPokemonUrl = defaultVariety.pokemon.url
                                                    val idFromDefaultVarietyUrl = defaultPokemonUrl.split("/")
                                                        .dropLastWhile { it.isEmpty() }
                                                        .lastOrNull()
                                                    // Si el ID de la variedad default es un número, úsalo.
                                                    // Si no (raro, pero podría ser un nombre), podrías usar el nombre o el ID original como fallback.
                                                    if (idFromDefaultVarietyUrl?.toIntOrNull() != null) {
                                                        resourceIdentifierForDetails = idFromDefaultVarietyUrl
                                                        Log.d("PokemonViewModel", "Using default variety ID $resourceIdentifierForDetails for $originalPokemonName (original species ID $idFromSpeciesUrl)")
                                                    } else if (idFromDefaultVarietyUrl != null && idFromDefaultVarietyUrl.isNotBlank()) {
                                                        // Podría ser un nombre, o algo que no es un ID numérico puro.
                                                        // Si tu getPokemonDetails puede manejar nombres, podrías usarlo.
                                                        // Por simplicidad, si no es un ID numérico claro, podríamos revertir o loguear.
                                                        // resourceIdentifierForDetails = idFromDefaultVarietyUrl // Si getPokemonDetails puede manejar nombres
                                                        Log.w("PokemonViewModel", "Default variety for $originalPokemonName has non-numeric identifier $idFromDefaultVarietyUrl from URL. Reverting to species ID $idFromSpeciesUrl for details fetch.")
                                                        // resourceIdentifierForDetails sigue siendo idFromSpeciesUrl.toString() en este caso.
                                                    }
                                                } else {
                                                    Log.w("PokemonViewModel", "No default variety found for $originalPokemonName (ID: $idFromSpeciesUrl). Using species ID for details.")
                                                    // resourceIdentifierForDetails sigue siendo idFromSpeciesUrl.toString()
                                                }
                                            }
                                        } else {
                                            Log.w("PokemonViewModel", "Failed to get species details for $originalPokemonName (ID: $idFromSpeciesUrl, Gen: $generationId, Code: ${speciesDetailsResponse.code()}). Using fallback name/color.")
                                        }

                                        // 2. Fetch Pokemon Details (para ID REAL, sprite, tipos) - USANDO resourceIdentifierForDetails
                                        val pokemonDetailsResponse =
                                            RetrofitClient.instance.getPokemonDetails(resourceIdentifierForDetails)

                                        if (pokemonDetailsResponse.isSuccessful) {
                                            pokemonDetailsResponse.body()?.let { detail ->
                                                finalPokemonId = detail.id
                                                pokemonSpriteUrl = detail.sprites.other?.officialArtwork?.frontDefault
                                                    ?: detail.sprites.frontDefault
                                                pokemonTypesList = detail.types.map { it.type.name.replaceFirstChar(Char::titlecase) }
                                            }
                                        } else {
                                            Log.e("PokemonViewModel", "Error fetching Pokemon details using identifier '$resourceIdentifierForDetails' (Original Name: $originalPokemonName, Species ID: $idFromSpeciesUrl, Gen: $generationId, Code: ${pokemonDetailsResponse.code()}) - ${pokemonDetailsResponse.message()}")
                                            return@async null
                                        }

                                        // 3. Asegúrate de tener un ID para crear el PokemonSummary
                                        if (finalPokemonId != null) {
                                            PokemonSummary(
                                                id = finalPokemonId!!,
                                                name = localizedName,
                                                spriteUrl = pokemonSpriteUrl,
                                                types = pokemonTypesList,
                                                colorName = pokemonColor
                                            )
                                        } else {
                                            Log.e("PokemonViewModel", "finalPokemonId is null after successful details fetch for identifier '$resourceIdentifierForDetails' (Original Name: $originalPokemonName, Species ID: $idFromSpeciesUrl, Gen: $generationId). Cannot create summary.")
                                            null
                                        }

                                    } catch (e: Exception) {
//                                        Log.e("PokemonViewModel", "Exception fetching summary for $originalPokemonName (Attempted Identifier: $resourceIdentifierForDetails, Species ID: $idFromSpeciesUrl, Gen: $generationId)", e)
                                        null
                                    }
                                } // Fin del bloque async para cada Pokémon
                            }
                        // Wait for all summaries to be fetched (or fail)
                        val newSummaries = pokemonSummariesDeferred.awaitAll().filterNotNull().sortedBy { it.id }

                        val currentCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        currentCache[generationId] = newSummaries
                        _pokemonByGenerationCache.value = currentCache
                    } else {
                        val currentCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        currentCache[generationId] = emptyList()
                        _pokemonByGenerationCache.value = currentCache
                    }
                    if (!errorShownThisFetch) _error.value = null

                } else {
                    handleError("Error fetching generation $generationId details: ${generationDetailResponse.code()}")
                }
            } catch (e: Exception) {
                handleError("Exception (fetching gen $generationId Pokémon): ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
        }
    }
*/
    //----------------------

    //-------------------------
    // --- Carga secuencialmente ---
    private val isLoadingPokemonByGenerationMap = mutableMapOf<Int, Boolean>()

    fun fetchPokemonForGeneration(generationId: Int?, forceRefresh: Boolean = false) {
        if (generationId == null) {
            _error.value = "Invalid generation ID."
            if (isLoadingPokemonByGenerationMap.none { it.value }) {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
            return
        }

        if (!forceRefresh && _pokemonByGenerationCache.value?.containsKey(generationId) == true &&
            _pokemonByGenerationCache.value?.get(generationId)?.isNotEmpty() == true
        ) {
            isLoadingPokemonByGenerationMap[generationId] = false
            if (isLoadingPokemonByGenerationMap.none { it.value }) {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
            return
        }

        if (isLoadingPokemonByGenerationMap[generationId] == true && !forceRefresh) {
            return
        }

        isLoadingPokemonByGenerationMap[generationId] = true
        _isLoadingPokemonForCurrentGeneration.value = true
        errorShownThisFetch = false
        // _currentlyFetchingGenerationId.value = generationId

        viewModelScope.launch { // Corrutina principal para esta operación de generación
            // Obtener la lista actual para esta generación desde el caché o empezar con una vacía.
            // Esta lista se irá poblando secuencialmente.
            val progressivelyLoadedListForThisGen =
                (_pokemonByGenerationCache.value?.get(generationId) ?: emptyList()).toMutableList()

            try {
                // Inicializar la entrada en el caché para esta generación si no existe,
                // así la UI puede observarla desde el principio.
                // Usamos la lista 'progressivelyLoadedListForThisGen' que ya podría tener datos de una carga previa interrumpida.
                val initialCache = _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                if (!initialCache.containsKey(generationId)) {
                    initialCache[generationId] = ArrayList(progressivelyLoadedListForThisGen) // Postea la lista actual (podría estar vacía)
                    _pokemonByGenerationCache.postValue(initialCache)
                }

                val generationDetailResponse = withContext(Dispatchers.IO) {
                    pokemonApiService.getGenerationDetails(generationId)
                }

                if (generationDetailResponse.isSuccessful) {
                    val generationData = generationDetailResponse.body()
                    // Asumimos que la API devuelve pokemonSpecies en un orden razonable (ej. por ID de especie)
                    val pokemonSpeciesListFromGen = generationData?.pokemonSpecies ?: emptyList()

                    if (pokemonSpeciesListFromGen.isNotEmpty()) {
                        // *** INICIO DEL PROCESAMIENTO SECUENCIAL ***
                        for (speciesResource in pokemonSpeciesListFromGen) {
                            // Si ya hemos cargado este Pokémon en una ejecución anterior interrumpida,
                            // y está en `progressivelyLoadedListForThisGen`, podríamos saltarlo.
                            // Esto requiere una forma de mapear speciesResource a un ID que ya podría estar en la lista.
                            // Por simplicidad, lo procesaremos; la lógica de duplicados más abajo lo manejará.

                            val originalPokemonName = speciesResource.name
                            val speciesUrl = speciesResource.url
                            var localizedName = originalPokemonName
                            var pokemonColor: String? = null
                            var pokemonSpriteUrl: String? = null
                            var pokemonTypesList: List<String> = emptyList()
                            var finalPokemonId: Int? = null
                            var newSummary: PokemonSummary? = null // Para almacenar el resultado

                            try { // Try-catch para cada Pokémon individual dentro del bucle
                                val idFromSpeciesUrl = speciesUrl.split("/")
                                    .dropLastWhile { it.isEmpty() }
                                    .lastOrNull()
                                    ?.toIntOrNull()

                                if (idFromSpeciesUrl == null) {
                                    Log.e("PokemonViewModel", "Could not parse ID from species URL: $speciesUrl for $originalPokemonName (Gen: $generationId). Skipping.")
                                    continue // Saltar al siguiente Pokémon en el bucle
                                }

                                var resourceIdentifierForDetails = idFromSpeciesUrl.toString()

                                // 1. Fetch Species Details (dentro de withContext para IO)
                                val speciesDetailsResponse = withContext(Dispatchers.IO) {
                                    pokemonApiService.getPokemonSpeciesDetails(idFromSpeciesUrl.toString())
                                }

                                if (speciesDetailsResponse.isSuccessful) {
                                    speciesDetailsResponse.body()?.let { speciesDetails ->
                                        speciesDetails.localizedNames.firstOrNull { it.language.name == "es" }?.name?.let {
                                            localizedName = it
                                        }
                                        pokemonColor = speciesDetails.color?.name
                                        val defaultVariety = speciesDetails.varieties.firstOrNull { it.isDefault }
                                        if (defaultVariety != null) {
                                            val defaultPokemonUrl = defaultVariety.pokemon.url
                                            val idFromDefaultVarietyUrl = defaultPokemonUrl.split("/")
                                                .dropLastWhile { it.isEmpty() }.lastOrNull()
                                            if (idFromDefaultVarietyUrl?.toIntOrNull() != null) {
                                                resourceIdentifierForDetails = idFromDefaultVarietyUrl
                                            } else if (idFromDefaultVarietyUrl != null && idFromDefaultVarietyUrl.isNotBlank()) {
                                                Log.w("PokemonViewModel", "Default variety for $originalPokemonName has non-numeric identifier $idFromDefaultVarietyUrl. Using species ID.")
                                            }
                                        }
                                    }
                                } else {
                                    Log.w("PokemonViewModel", "Failed to get species details for $originalPokemonName (Gen: $generationId, Code: ${speciesDetailsResponse.code()}).")
                                    // Continuar con el siguiente Pokémon, este podría no tener todos los detalles.
                                }

                                // 2. Fetch Pokemon Details (dentro de withContext para IO)
                                val pokemonDetailsResponse = withContext(Dispatchers.IO) {
                                    pokemonApiService.getPokemonDetails(resourceIdentifierForDetails)
                                }

                                if (pokemonDetailsResponse.isSuccessful) {
                                    pokemonDetailsResponse.body()?.let { detail ->
                                        finalPokemonId = detail.id
                                        pokemonSpriteUrl = detail.sprites.other?.officialArtwork?.frontDefault
                                            ?: detail.sprites.frontDefault
                                        pokemonTypesList = detail.types.map { it.type.name.replaceFirstChar(Char::titlecase) }
                                    }
                                } else {
                                    Log.e("PokemonViewModel", "Error fetching Pokemon details for $originalPokemonName (Gen: $generationId, Code: ${pokemonDetailsResponse.code()}). Skipping this Pokémon.")
                                    continue // Saltar al siguiente Pokémon en el bucle
                                }

                                // 3. Crear PokemonSummary
                                if (finalPokemonId != null) {
                                    newSummary = PokemonSummary(
                                        id = finalPokemonId!!,
                                        name = localizedName,
                                        spriteUrl = pokemonSpriteUrl,
                                        types = pokemonTypesList,
                                        colorName = pokemonColor
                                    )
                                } else {
                                    Log.e("PokemonViewModel", "finalPokemonId is null for $originalPokemonName (Gen: $generationId). Cannot create summary.")
                                    // No se pudo crear el sumario, así que no se añade nada.
                                }

                            } catch (e: Exception) {
                                Log.e("PokemonViewModel", "Exception processing summary for $originalPokemonName (Gen: $generationId) in sequential load", e)
                                // Un error procesando este Pokémon, no lo añadimos y continuamos con el siguiente.
                                newSummary = null
                            }
                            // Si se creó un sumario, añadirlo a nuestra lista progresiva y actualizar LiveData
                            if (newSummary != null) {
                                if (progressivelyLoadedListForThisGen.none { it.id == newSummary.id }) { // Quitado el !! innecesario
                                    progressivelyLoadedListForThisGen.add(newSummary) // Quitado el !! innecesario
                                    // No es estrictamente necesario ordenar aquí si pokemonSpeciesListFromGen está ordenada
                                    // y los estamos añadiendo en ese orden. Pero para ser seguros y manejar cualquier caso:
                                    progressivelyLoadedListForThisGen.sortBy { it.id }
                                }

                                // Actualiza el LiveData DESPUÉS de procesar CADA Pokémon
                                // Es crucial crear una nueva instancia del Map y de la List para que LiveData detecte el cambio.
                                val currentGlobalCache = _pokemonByGenerationCache.value ?: emptyMap()
                                val newGlobalCache = currentGlobalCache.toMutableMap()
                                newGlobalCache[generationId] = ArrayList(progressivelyLoadedListForThisGen) // ¡Nueva instancia de la lista!
                                _pokemonByGenerationCache.postValue(newGlobalCache)
                            }
                        } // *** FIN DEL PROCESAMIENTO SECUENCIAL (for loop) ***
                    } else { // pokemonSpeciesListFromGen está vacía
                        val currentGlobalCache = _pokemonByGenerationCache.value ?: emptyMap()
                        if (!currentGlobalCache.containsKey(generationId)) {
                            val newGlobalCache = currentGlobalCache.toMutableMap()
                            newGlobalCache[generationId] = emptyList()
                            _pokemonByGenerationCache.postValue(newGlobalCache)
                        }
                        Log.i("PokemonViewModel", "No Pokémon species found for generation $generationId.")
                    }
                    if (!errorShownThisFetch) _error.postValue(null)

                } else { // generationDetailResponse no fue successful
                    handleError("Error fetching generation $generationId details: ${generationDetailResponse.code()}")
                }
            } catch (e: Exception) {
                handleError("Exception (outer scope, fetching gen $generationId Pokémon list): ${e.message ?: "Unknown exception"}")
            } finally {
                // Toda la carga (secuencial) para esta generación ha terminado o fallado.
                isLoadingPokemonByGenerationMap[generationId] = false
                if (isLoadingPokemonByGenerationMap.none { it.value }) {
                    _isLoadingPokemonForCurrentGeneration.postValue(false)
                }
                // if (_currentlyFetchingGenerationId.value == generationId) {
                //     _currentlyFetchingGenerationId.postValue(null)
                // }
            }
        }
    }
    //-------------------

    private fun fetchEvolutionChainDetails(evolutionChainUrl: String) {
        if (evolutionChainUrl.isBlank()) {
            _evolutionChainDetails.value = null
            _isLoadingEvolutionChain.value = false
            return
        }

        _isLoadingEvolutionChain.value = true
        _evolutionChainDetails.value = null

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getEvolutionChainDetailsByUrl(evolutionChainUrl)
                }

                if (response.isSuccessful) {
                    _evolutionChainDetails.value = response.body()
                } else {
                    // Log error or handle as needed
                }
            } catch (e: Exception) {
                // Log error or handle as needed
            } finally {
                _isLoadingEvolutionChain.value = false
            }
        }
    }

    internal suspend fun fetchLocalizedName(
        resourceUrl: String,
        fallbackApiName: String,
        resourceTypeHint: String,
        languageCode: String = "es"
    ): String {
        if (resourceUrl.isBlank()) {
            return formatApiName(fallbackApiName)
        }

        if (resourceTypeHint.lowercase() == "trigger") {
            return formatApiName(fallbackApiName)
        }

        try {
            val response: Response<out Any> = withContext(Dispatchers.IO) {
                when (resourceTypeHint.lowercase()) {
                    "item" -> RetrofitClient.instance.getItemDetailsByUrl(resourceUrl)
                    "move" -> RetrofitClient.instance.getMoveDetailsByUrl(resourceUrl)
                    "pokemon-species" -> RetrofitClient.instance.getPokemonSpeciesDetailsByUrl(resourceUrl)
                    "type" -> {
                        // Extract name or ID from URL if it's a full URL to a type resource
                        // e.g., "https://pokeapi.co/api/v2/type/fire/" -> "fire"
                        val typeNameOrId = resourceUrl.split("/").dropLast(1).lastOrNull()
                        if (typeNameOrId != null) {
                            RetrofitClient.instance.getTypeDetailsByName(typeNameOrId)
                            // Or use getTypeDetailsById if it's an ID and you have that service method
                        } else {
                            // Cannot determine type name/ID from URL, return error or fallback
                            // For simplicity, causing an error that will be caught below
                            throw IllegalArgumentException("Invalid URL for type resource: $resourceUrl")
                        }
                    }
                    "location" -> RetrofitClient.instance.getGenericNamedResourceDetailsByUrl(resourceUrl)
                    "region" -> RetrofitClient.instance.getGenericNamedResourceDetailsByUrl(resourceUrl)
                    "generation" -> RetrofitClient.instance.getGenericNamedResourceDetailsByUrl(resourceUrl)
                    else -> RetrofitClient.instance.getGenericNamedResourceDetailsByUrl(resourceUrl)
                }
            }

            if (response.isSuccessful) {
                val body = response.body()
                val namesList: List<NameEntry>? = when (body) {
                    is ItemDetailResponse -> body.names
                    is MoveDetailResponse -> body.names.map { NameEntry(it.language, it.name) }
                    is PokemonSpeciesResponse -> body.localizedNames
                    is TypeDetailResponse -> body.names
                    is GenericNamedResourceDetail -> body.names
                    else -> null
                }

                val localizedName = namesList?.find { it.language.name == languageCode }?.name
                if (localizedName != null) return localizedName

                val englishNameFromApi = namesList?.find { it.language.name == "en" }?.name
                if (englishNameFromApi != null) return englishNameFromApi

                return formatApiName(fallbackApiName)
            } else {
                return formatApiName(fallbackApiName)
            }
        } catch (e: Exception) {
            return formatApiName(fallbackApiName)
        }
    }
    suspend fun buildEvolutionConditionString(detail: EvolutionDetail): String {
        var condition: String

        // Initial condition is based on the trigger's translated name
        condition = translateEvolutionTrigger(detail.trigger.name)

        detail.minLevel?.let { level ->
            condition += " $level"
        }

        detail.item?.let { itemResource ->
            val itemName = fetchLocalizedName(
                resourceUrl = itemResource.url,
                fallbackApiName = itemResource.name,
                resourceTypeHint = "item"
            )
            condition += "\nUsando $itemName"
        }

        detail.heldItem?.let { heldItemResource ->
            val heldItemName = fetchLocalizedName(
                resourceUrl = heldItemResource.url,
                fallbackApiName = heldItemResource.name,
                resourceTypeHint = "item"
            )
            condition += "\nCon $heldItemName equipado"
        }

        detail.minHappiness?.let { happiness ->
            condition += "\nFelicidad mín.: $happiness"
        }

        detail.timeOfDay?.takeIf { it.isNotEmpty() }?.let { time ->
            val timeInSpanish = when (time.lowercase()) {
                "day" -> "día"
                "night" -> "noche"
                else -> formatApiName(time)
            }
            condition += "\nDurante el $timeInSpanish"
        }

        detail.knownMove?.let { moveResource ->
            val moveName = fetchLocalizedName(
                resourceUrl = moveResource.url,
                fallbackApiName = moveResource.name,
                resourceTypeHint = "move"
            )
            condition += "\nConociendo $moveName"
        }

        detail.knownMoveType?.let { typeResource ->
            val typeName = fetchLocalizedName(
                resourceUrl = typeResource.url,
                fallbackApiName = typeResource.name,
                resourceTypeHint = "type"
            )
            condition += "\nConociendo mov. tipo $typeName"
        }

        detail.minAffection?.let { affection ->
            condition += "\nAfecto mín.: $affection"
        }

        detail.minBeauty?.let { beauty ->
            condition += "\nBelleza mín.: $beauty"
        }

        detail.location?.let { locationResource ->
            val locationName = fetchLocalizedName(
                resourceUrl = locationResource.url,
                fallbackApiName = locationResource.name,
                resourceTypeHint = "location"
            )
            condition += "\nEn $locationName"
        }

        detail.gender?.let { genderId ->
            val genderName = when (genderId) {
                1 -> "Hembra" // Female
                2 -> "Macho"  // Male
                else -> ""
            }
            if (genderName.isNotEmpty()) condition += "\nSiendo $genderName"
        }

        detail.partySpecies?.let { speciesResource ->
            val partyPokemonName = fetchLocalizedName(
                resourceUrl = speciesResource.url,
                fallbackApiName = speciesResource.name,
                resourceTypeHint = "pokemon-species"
            )
            condition += "\nCon $partyPokemonName en el equipo"
        }

        detail.partyType?.let { typeResource ->
            val partyTypeName = fetchLocalizedName(
                resourceUrl = typeResource.url,
                fallbackApiName = typeResource.name,
                resourceTypeHint = "type"
            )
            condition += "\nCon un Pokémon tipo $partyTypeName en el equipo"
        }

        detail.tradeSpecies?.let { speciesResource ->
            val tradePokemonName = fetchLocalizedName(
                resourceUrl = speciesResource.url,
                fallbackApiName = speciesResource.name,
                resourceTypeHint = "pokemon-species"
            )
            condition += "\nIntercambiado por $tradePokemonName"
        }

        if (detail.needsOverworldRain == true) { // Explicitly check for true if it's Boolean?
            condition += "\nCon lluvia en el mundo exterior"
        }

        if (detail.turnUpsideDown == true) { // Explicitly check for true if it's Boolean?
            condition += "\nGirando la consola"
        }

        detail.relativePhysicalStats?.let { relativeStats ->
            val comparison = when {
                relativeStats > 0 -> "Ataque > Defensa"
                relativeStats < 0 -> "Ataque < Defensa"
                relativeStats == 0 -> "Ataque = Defensa"
                else -> ""
            }
            if (comparison.isNotEmpty()) condition += "\nCon $comparison"
        }
        return condition.trim()
    }

    // Helper function to format API names (e.g., "thunder-stone" -> "Thunder Stone")
    private fun formatApiName(apiName: String): String {
        return apiName.split('-').joinToString(" ") { it.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString()
        } }
    }

    // Helper function to translate trigger names (simple example, expand as needed)
    private fun translateEvolutionTrigger(triggerName: String): String {
        return when (triggerName.lowercase()) {
            "level-up" -> "Nivel"
            "trade" -> "Intercambio"
            "use-item" -> "Usar objeto"
            "shed" -> "Muda" // Example: Shedinja
            "push-block" -> "Empujar bloque" // Example for specific game mechanics if any
            "three-critical-hits" -> "3 Golpes Críticos" // Farfetch'd Galar
            "take-damage" -> "Recibir daño" // Yamask Galar (49+ HP and walk under stone bridge)
            "agile-style-move" -> "Mov. Estilo Ágil" // Stantler in PLA
            "strong-style-move" -> "Mov. Estilo Fuerte" // Scyther in PLA
            "spin" -> "Girar" // Milcery
            // Add more translations as needed
            else -> formatApiName(triggerName) // Fallback to formatted name
        }
    }

    /*
    fun fetchAllPokemonTypes() {
        if (_pokemonTypes.value != ALL_POKEMON_TYPES && _pokemonTypes.value?.isNotEmpty() == true) return // Ya cargados desde API o no es el valor inicial
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getAllPokemonTypes() // Asumiendo esta función en tu service
                if (response.isSuccessful) {
                    val typesFromApi = response.body()?.results?.map { it.name.replaceFirstChar(Char::titlecase) } ?: emptyList()
                    if (typesFromApi.isNotEmpty()) {
                        _pokemonTypes.value = listOf(NO_TYPE_SELECTED) + typesFromApi.sorted()
                    } else {
                        _pokemonTypes.value = ALL_POKEMON_TYPES // Fallback a tu lista estática
                        handleError("No types returned from API, using default list.")
                    }
                } else {
                    _pokemonTypes.value = ALL_POKEMON_TYPES // Fallback
                    handleError("Error fetching Pokémon types: ${response.code()}")
                }
            } catch (e: Exception) {
                _pokemonTypes.value = ALL_POKEMON_TYPES // Fallback
                handleError("Exception fetching Pokémon types: ${e.message}")
            }
        }
    }
    */

    private val _pokemonFormsAndVarieties = MutableLiveData<List<DisplayablePokemonVariety>>()
    val pokemonFormsAndVarieties: LiveData<List<DisplayablePokemonVariety>> = _pokemonFormsAndVarieties

    private val _isLoadingForms = MutableLiveData<Boolean>(false)
    val isLoadingForms: LiveData<Boolean> = _isLoadingForms

    fun fetchPokemonFormsAndVarieties(pokemonId: Int, speciesName: String) {
        _isLoadingForms.value = true
        _pokemonFormsAndVarieties.value = emptyList()
        errorShownThisFetch = false

        viewModelScope.launch {
            try {
                // Primero, obtén los detalles de la especie para las variedades
                val speciesResponse = RetrofitClient.instance.getPokemonSpeciesDetailsById(pokemonId) // O por nombre si lo prefieres
                // También puedes obtener los detalles del Pokémon base para la lista de 'forms'
                // val detailResponse = RetrofitClient.instance.getPokemonDetailsById(pokemonId)


                if (speciesResponse.isSuccessful) {
                    val speciesData = speciesResponse.body()
                    val varietiesFromSpecies = speciesData?.varieties ?: emptyList()

                    val displayableVarieties = mutableListOf<DisplayablePokemonVariety>()

                    // Mapea las variedades de la especie
                    // Las variedades 'isDefault = true' suelen ser el Pokémon base.
                    // Las que no son 'default' son formas distintas (Mega, Alola, etc.)
                    val formFetchJobs = varietiesFromSpecies.map { variety ->
                        async(Dispatchers.IO) {
                            try {
                                val formPokemonUrl = variety.pokemon.url
                                val formPokemonIdOrName = formPokemonUrl.split("/").dropLast(1).last()

                                // Llama a getPokemonDetails o getPokemonFormDetails para obtener el sprite de esta forma
                                val formDetailResponse = RetrofitClient.instance.getPokemonDetails(formPokemonIdOrName)
                                // O si prefieres usar el endpoint /pokemon-form/
                                // val formDetailResponse = RetrofitClient.instance.getPokemonFormDetails(formPokemonIdOrName)


                                if (formDetailResponse.isSuccessful) {
                                    val formDetail = formDetailResponse.body()
                                    formDetail?.let {
                                        // Intenta obtener el nombre localizado de la forma desde speciesData si es posible,
                                        // o usa el nombre de la forma del detalle.
                                        // Aquí la lógica de localización puede ser compleja.
                                        // Por ahora, usaremos el nombre del 'pokemon' de la variedad
                                        // y el sprite de 'formDetail'.

                                        var displayName = variety.pokemon.name // Fallback
                                        var localizedFormName: String? = null

                                        // Intento 1: Usar PokemonFormDetailResponse si ese es el endpoint que llamaste
                                        if (formDetail is PokemonFormDetailResponse) { // Necesitas un cast seguro o type check
                                            localizedFormName = formDetail.localizedFormNames.firstOrNull { it.language.name == "es" }?.name
                                            if (localizedFormName.isNullOrBlank()) {
                                                localizedFormName = formDetail.localizedPokemonNames.firstOrNull { it.language.name == "es" }?.name
                                            }
                                        }

                                        // Intento 2 (Fallback o si usaste PokemonDetailResponse para la forma):
                                        // Para localizar nombres de formas específicas (ej: "Mega Charizard X"),
                                        // a veces el nombre está en PokemonSpeciesResponse -> varieties -> pokemon.name ("charizard-mega-x")
                                        // y necesitas obtener PokemonFormDetailResponse para `localizedFormNames` o `localizedPokemonNames`.
                                        // O, si la forma tiene su propia entrada en PokemonSpecies, puedes obtener `localizedNames` desde ahí.

                                        if (localizedFormName.isNullOrBlank()) {
                                            // Intenta obtener el nombre del Pokémon base y añadirle algo como "Forma Alola"
                                            // Esto es un placeholder, la localización real es más compleja.
                                            val speciesBaseName = speciesData?.localizedNames?.firstOrNull {it.language.name == "es"}?.name ?: (speciesData?.name
                                                ?: "error")
                                            displayName = if (!variety.isDefault && variety.pokemon.name.contains("-")) {
                                                // Intenta formatear el nombre de la variedad
                                                // ej: "charizard-mega-x" -> "Mega Charizard X"
                                                // Necesitarás una función de formateo para esto
                                                formatPokemonFormName(variety.pokemon.name, speciesBaseName)
                                            } else {
                                                speciesBaseName // Para la forma default
                                            }
                                        } else {
                                            displayName = localizedFormName
                                        }


                                        DisplayablePokemonVariety(
                                            id = formDetail.id, // ID del Pokémon de esta forma específica
                                            name = displayName,
                                            spriteUrl = formDetail.sprites.other?.officialArtwork?.frontDefault
                                                ?: formDetail.sprites.frontDefault,
                                            isDefault = variety.isDefault
                                        )
                                    }
                                } else {
                                    Log.e("PokemonViewModel", "Failed to get details for form ${variety.pokemon.name}")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("PokemonViewModel", "Exception fetching form ${variety.pokemon.name}", e)
                                null
                            }
                        }
                    }

                    _pokemonFormsAndVarieties.value = formFetchJobs.awaitAll().filterNotNull()
                        // Puedes querer filtrar isDefault=true si solo quieres mostrar formas alternativas
                        // O ordenarlas para que la default aparezca primero.
                        .sortedWith(compareByDescending<DisplayablePokemonVariety> { it.isDefault }.thenBy { it.id })


                } else {
                    handleError("Failed to fetch species details for forms: ${speciesResponse.code()}")
                }
            } catch (e: Exception) {
                handleError("Exception fetching forms: ${e.message}")
            } finally {
                _isLoadingForms.value = false
            }
        }
    }

    // Función de ayuda para formatear nombres de formas (ejemplo simple)
    fun formatPokemonFormName(formApiName: String, basePokemonName: String): String {
        // "charizard-mega-x" -> "Mega Charizard X"
        // "raticate-alola" -> "Raticate Alola"
        // "deoxys-attack" -> "Deoxys Forma Ataque"
        val parts = formApiName.split('-')
        if (parts.firstOrNull()?.equals(basePokemonName.lowercase(), ignoreCase = true) == true) {
            // Empieza con el nombre del Pokémon base
            val formSuffix = parts.drop(1).joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
            return "$basePokemonName $formSuffix" // ej: "Pikachu Cosplay"
        } else {
            // Si no, es un nombre completamente diferente o una forma compleja
            return formApiName.split('-').joinToString(" ") { it.replaceFirstChar(Char::titlecase) }
        }
    }

    fun isFetchingForGenerationId(generationId: Int?): Boolean {
        return _isLoadingPokemonForCurrentGeneration.value == true && _currentlyFetchingGenerationId.value == generationId
    }
}

