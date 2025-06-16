package com.david.pokedex_api.api.viewModel

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.pokedex_api.api.client.RetrofitClient
import com.david.pokedex_api.api.model.*
import com.david.pokedex_api.api.service.PokeApiService
import com.david.pokedex_api.ui.composables.ALL_POKEMON_TYPES
import com.david.pokedex_api.ui.composables.NO_TYPE_SELECTED
import com.david.pokedex_api.util.TypeInteraction
import com.david.pokedex_api.util.getCombinedDefensiveInteractions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Response

class PokemonViewModel : ViewModel() {

    val pokemonApiService: PokeApiService = RetrofitClient.instance // O RetrofitClient.api si así se llama


    // --- Para la vista de un solo Pokémon (detalle) ---
    private val _pokemonDetails = MutableLiveData<PokemonDetailResponse?>()
    val pokemonDetails: LiveData<PokemonDetailResponse?> = _pokemonDetails

    private val _pokemonDescription = MutableLiveData<String?>()
    val pokemonDescription: LiveData<String?> = _pokemonDescription

    private val _isLoadingDetails = MutableLiveData<Boolean>(false)
    val isLoadingDetails: LiveData<Boolean> = _isLoadingDetails

    // --- Para la lista de Pokémon ---
    private val _pokemonList =
        MutableLiveData<List<PokemonSummary>>(emptyList()) // Inicializa con lista vacía
    val pokemonList: LiveData<List<PokemonSummary>> = _pokemonList

    private val _isLoadingList = MutableLiveData<Boolean>(false)
    val isLoadingList: LiveData<Boolean> = _isLoadingList

    // --- Para las Generaciones y la Lista de Pokémon por Generación ---
    private val _generations = MutableLiveData<List<NamedApiResource>>(emptyList())
    val generations: LiveData<List<NamedApiResource>> = _generations

    private val _pokemonByGenerationCache =
        MutableLiveData<Map<Int, List<PokemonSummary>>>(emptyMap())
    val pokemonByGenerationCache: LiveData<Map<Int, List<PokemonSummary>>> =
        _pokemonByGenerationCache

    // Estado de carga para la lista de Pokémon de la generación actual
    private val _isLoadingPokemonForCurrentGeneration = MutableLiveData<Boolean>(false)
    val isLoadingPokemonForCurrentGeneration: LiveData<Boolean> =
        _isLoadingPokemonForCurrentGeneration

    private val _isLoadingGenerations = MutableLiveData<Boolean>(false)
    val isLoadingGenerations: LiveData<Boolean> = _isLoadingGenerations

    private val _evolutionChainDetails = MutableLiveData<EvolutionChainDetailResponse?>()
    val evolutionChainDetails: LiveData<EvolutionChainDetailResponse?> = _evolutionChainDetails

    private val _specialForms = MutableStateFlow<List<Pair<String, PokemonSpeciesVariety>>>(emptyList())
    val specialForms: StateFlow<List<Pair<String, PokemonSpeciesVariety>>> = _specialForms.asStateFlow()
// o si prefieres LiveData:
// private val _specialForms = MutableLiveData<List<Pair<String, PokemonSpeciesVariety>>>(emptyList())
// val specialForms: LiveData<List<Pair<String, PokemonSpeciesVariety>>> = _specialForms

    private val _isLoadingSpecialForms = MutableStateFlow(false)
    val isLoadingSpecialForms: StateFlow<Boolean> = _isLoadingSpecialForms.asStateFlow()

    private var currentOffset = 0
    private val POKEMON_LIST_LIMIT = 20 // Cuántos cargar a la vez
    private var canLoadMore = true // Flag para saber si hay más páginas

    // --- Común ---
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private var errorShownThisFetch =
        false // Para evitar múltiples toasts por una sola acción del usuario

    private val _typeInteractions = MutableLiveData<List<TypeInteraction>>(emptyList())
    val typeInteractions: LiveData<List<TypeInteraction>> = _typeInteractions

    private val _isLoadingTypeInteractions = MutableLiveData(false)
    val isLoadingTypeInteractions: LiveData<Boolean> = _isLoadingTypeInteractions


    init {
        // Carga la primera página al iniciar el ViewModel si es necesario
        // O puedes llamarlo desde la UI cuando la lista sea visible por primera vez
        // fetchInitialPokemonList()
    }

    // --- Lógica para la Lista de Pokémon ---

    fun fetchInitialPokemonList() {
        if (_pokemonList.value?.isNotEmpty() == true) return // Ya cargado, o si quieres refresh, llama a clearAndFetch
        currentOffset = 0
        canLoadMore = true
        _pokemonList.value = emptyList() // Limpia para la carga inicial
        fetchMorePokemonItems()
    }

    fun fetchMorePokemonItems() {
        if (_isLoadingList.value == true || !canLoadMore) return // Evitar cargas múltiples o si no hay más

        _isLoadingList.value = true
        errorShownThisFetch = false // Resetea para esta operación de carga

        viewModelScope.launch {
            try {
                Log.d(
                    "PokemonViewModel",
                    "Fetching list. Offset: $currentOffset, Limit: $POKEMON_LIST_LIMIT"
                )
                val listResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getPokemonList(
                        offset = currentOffset,
                        limit = POKEMON_LIST_LIMIT
                    ).execute()
                }

                if (listResponse.isSuccessful) {
                    val pokemonListFromApi = listResponse.body()?.results ?: emptyList()
                    canLoadMore = listResponse.body()?.next != null // Hay más si 'next' no es null

                    if (pokemonListFromApi.isNotEmpty()) {
                        // Obtener detalles para cada Pokémon en la lista (esto es costoso)
                        val detailedPokemonSummariesDeferred = pokemonListFromApi.map { listItem ->
                            async(Dispatchers.IO) {
                                try {
                                    Log.d(
                                        "PokemonViewModel",
                                        "Fetching details for list item: ${listItem.name}"
                                    )
                                    val detailResponseCall =
                                        RetrofitClient.instance.getPokemonDetails(listItem.name)
                                            .execute()
                                    if (detailResponseCall.isSuccessful) {
                                        detailResponseCall.body()?.let { detail ->
                                            PokemonSummary(
                                                id = detail.id,
                                                name = detail.name,
                                                spriteUrl = detail.sprites.other?.officialArtwork?.frontDefault
                                                    ?: detail.sprites.frontDefault,
                                                types = detail.types.map { it.type.name },
                                                colorName = null
                                                )
                                        }
                                    } else {
                                        Log.e(
                                            "PokemonViewModel",
                                            "Error fetching details for ${listItem.name}: ${detailResponseCall.code()}"
                                        )
                                        null // Si falla la obtención de detalles para un ítem
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "PokemonViewModel",
                                        "Exception fetching details for ${listItem.name}",
                                        e
                                    )
                                    null
                                }
                            }
                        }
                        // Esperar a que todos los detalles se carguen y filtrar los nulos (fallidos)
                        val newSummaries =
                            detailedPokemonSummariesDeferred.awaitAll().filterNotNull()

                        // Añadir los nuevos items a la lista existente
                        val currentList = _pokemonList.value ?: emptyList()
                        _pokemonList.value = currentList + newSummaries
                        currentOffset += pokemonListFromApi.size // Actualizar el offset para la siguiente carga
                    } else if (currentOffset == 0) {
                        _pokemonList.value = emptyList() // No results and it was the first page
                    }
                    _error.value =
                        null // Limpia errores anteriores si la lista principal fue exitosa
                } else {
                    Log.e(
                        "PokemonViewModel",
                        "Error fetching Pokemon list: ${listResponse.code()} - ${listResponse.message()}"
                    )
                    handleError("Error fetching list: ${listResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("PokemonViewModel", "Exception fetching Pokemon list", e)
                handleError("Exception (list): ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingList.value = false
            }
        }
    }


    // --- Lógica para la Vista de Detalles de un Pokémon ---
    fun fetchPokemonDetailsByName(name: String, lang: String) {
        Log.e(
            "PVM_CRITICAL_DEBUG",
            "fetchPokemonDetailsByName called with NAME: '$name'"
        ) // LOG MUY VISIBLE

        if (_isLoadingDetails.value == true && _pokemonDetails.value?.name?.equals(
                name,
                ignoreCase = true
            ) == true
        ) {
            // Ya está cargando este Pokémon o ya está cargado
            Log.d("PokemonViewModel", "Details for '$name' already loading or loaded. Skipping.")
            return
        }

        _isLoadingDetails.value = true
        _pokemonDetails.value = null // Limpia datos anteriores al buscar uno nuevo
        _pokemonDescription.value = null // Limpia descripción anterior
        errorShownThisFetch = false // Resetea para esta operación de carga

        val pokemonNameLower = name.lowercase().trim()
        Log.e(
            "PVM_CRITICAL_DEBUG",
            "Identifier for API calls will be: '$pokemonNameLower'"
        ) // LOG MUY VISIBLE


        viewModelScope.launch {
            try {
                // Lanzar ambas llamadas de red en paralelo usando async
                val detailResponseDeferred = async(Dispatchers.IO) {
                    Log.d(
                        "PokemonViewModel",
                        "Fetching details for '$pokemonNameLower' on ${Thread.currentThread().name}"
                    )
                    RetrofitClient.instance.getPokemonDetails(pokemonNameLower).execute()
                }
                val speciesResponseDeferred = async(Dispatchers.IO) {
                    Log.d(
                        "PokemonViewModel",
                        "Fetching species for '$pokemonNameLower' on ${Thread.currentThread().name}"
                    )
                    RetrofitClient.instance.getPokemonSpeciesDetails(pokemonNameLower).execute()
                }

                // Esperar a que ambas completen
                val detailResponse = detailResponseDeferred.await()
                val speciesResponse = speciesResponseDeferred.await()

                // ---- INICIO DEBUG LOGS PARA SPECIES RESPONSE ----
                Log.d(
                    "PVM_Species_Check",
                    "Species Call for '$pokemonNameLower'. Successful: ${speciesResponse.isSuccessful}, Code: ${speciesResponse.code()}"
                )
                if (!speciesResponse.isSuccessful) {
                    val errorBody = speciesResponse.errorBody()?.string() ?: "Unknown species error"
                    Log.e(
                        "PVM_Species_Check",
                        "Species Call Error Body for '$pokemonNameLower': $errorBody"
                    )
                }
                // ---- FIN DEBUG LOGS PARA SPECIES RESPONSE ----

                // Procesar respuesta de detalles
                if (detailResponse.isSuccessful) {
                    _pokemonDetails.value = detailResponse.body()
                    if (detailResponse.body() == null) {
                        Log.w(
                            "PokemonViewModel",
                            "Detail response for '$pokemonNameLower' successful but body is null. Code: ${detailResponse.code()}"
                        )
                        handleError("Error: Pokémon data not found (details).")
                    } else {
                        Log.d(
                            "PokemonViewModel",
                            "Detail response for '$pokemonNameLower' successful. Name from details: ${_pokemonDetails.value?.name}"
                        )
                    }
                } else {
                    val errorBody = detailResponse.errorBody()?.string() ?: "Unknown detail error"
                    Log.e(
                        "PokemonViewModel",
                        "Error fetching Pokemon details for '$pokemonNameLower': ${detailResponse.code()} - ${detailResponse.message()} - $errorBody"
                    )
                    handleError("Details Error: ${detailResponse.code()} ${detailResponse.message()}")
                }

                // Procesar respuesta de especie (descripción)
                if (speciesResponse.isSuccessful) {
                    val speciesData = speciesResponse.body()
                    // ---- INICIO DEBUG LOGS PARA SPECIES DATA ----
                    Log.d(
                        "PVM_Species_Check",
                        "Species data for '$pokemonNameLower' is null: ${speciesData == null}"
                    )
                    // ---- FIN DEBUG LOGS PARA SPECIES DATA ----

                    if (speciesData != null) {
                        Log.d(
                            "PokemonViewModel",
                            "Species data for '$pokemonNameLower' successfully obtained. Species name from data: ${speciesData.name}"
                        )

                        // ---- INICIO DEBUG LOGS PARA FLAVOR TEXTS ----
                        Log.d(
                            "PVM_Flavor_Texts",
                            "For '$pokemonNameLower', total flavor_text_entries: ${speciesData.flavorTextEntries.size}"
                        )
                        speciesData.flavorTextEntries.forEachIndexed { index, fe ->
                            Log.d(
                                "PVM_Flavor_Texts",
                                "Entry $index: Lang='${fe.language.name}', Version='${fe.version.name}', Text non-null: ${fe.flavorText != null}, Text='${
                                    fe.flavorText?.replace(
                                        "\n",
                                        " "
                                    )
                                }'"
                            )
                        }
                        // ---- FIN DEBUG LOGS PARA FLAVOR TEXTS ----

                        val preferredVersions = listOf(
                            "sword",
                            "shield",
                            "scarlet",
                            "violet",
                            "legends-arceus",
                            "ultra-sun",
                            "alpha-sapphire"
                        ) // Considera expandir o revisar estas versiones

                        val allEnglishEntries =
                            speciesData.flavorTextEntries.filter { it.language.name == lang }
                        Log.d(
                            "PVM_Flavor_Texts",
                            "For '$pokemonNameLower', count of English entries: ${allEnglishEntries.size}"
                        )

                        val entryFromPreferred = allEnglishEntries.firstOrNull { entry ->
                            val foundInPreferred = preferredVersions.any { version ->
                                entry.version.name.contains(version)
                            }
                            Log.d(
                                "PVM_Flavor_Texts_Preferred",
                                "Checking entry version '${entry.version.name}' (lang ${entry.language.name}) against preferred. Found: $foundInPreferred"
                            )
                            foundInPreferred
                        }
                        Log.d(
                            "PVM_Flavor_Texts_Preferred",
                            "For '$pokemonNameLower', entry from preferred versions is null: ${entryFromPreferred == null}. FlavorText: '${
                                entryFromPreferred?.flavorText?.replace(
                                    "\n",
                                    " "
                                )
                            }'"
                        )

                        val fallbackEntry = if (entryFromPreferred == null) {
                            allEnglishEntries.firstOrNull() // Fallback a la primera entrada en inglés si no hay de versiones preferidas
                        } else {
                            null // No necesitamos fallback si ya tenemos una de preferredVersions
                        }
                        Log.d(
                            "PVM_Flavor_Texts_Fallback",
                            "For '$pokemonNameLower', fallback English entry is null: ${fallbackEntry == null}. FlavorText: '${
                                fallbackEntry?.flavorText?.replace(
                                    "\n",
                                    " "
                                )
                            }'"
                        )

                        val englishDescription =
                            entryFromPreferred?.flavorText ?: fallbackEntry?.flavorText

                        // ---- INICIO DEBUG LOGS PARA DESCRIPCIÓN SELECCIONADA ----
                        Log.d(
                            "PVM_Desc_Selection",
                            "For '$pokemonNameLower', selected englishDescription is null: ${englishDescription == null}"
                        )
                        if (englishDescription != null) {
                            Log.d(
                                "PVM_Desc_Selection",
                                "Selected englishDescription for '$pokemonNameLower': '$englishDescription'"
                            )
                        }
                        // ---- FIN DEBUG LOGS PARA DESCRIPCIÓN SELECCIONADA ----

                        _pokemonDescription.value = englishDescription
                            ?.replace("\n", " ")
                            ?.replace("\u000c", " ")
                            ?.replace("POKéMON", "Pokémon")

                        if (englishDescription == null && _pokemonDescription.value == null) { // Doble check
                            Log.w(
                                "PokemonViewModel",
                                "No English description ultimately found or set for '$pokemonNameLower'."
                            )
                        } else if (_pokemonDescription.value != null) {
                            Log.i(
                                "PokemonViewModel",
                                "Final description for '$pokemonNameLower' set to: '${_pokemonDescription.value}'"
                            )
                        } else {
                            Log.w(
                                "PokemonViewModel",
                                "EnglishDescription was not null, but _pokemonDescription IS null after replace for '$pokemonNameLower'. This is odd."
                            )
                        }
// --- AQUÍ ES DONDE SE OBTIENE LA URL Y SE LLAMA A fetchEvolutionChainDetails ---
                        speciesData.evolutionChain?.url?.let { evolutionUrl ->
                            if (evolutionUrl.isNotBlank()) {
                                Log.d(
                                    "ViewModelEvolution",
                                    "Calling fetchEvolutionChainDetails with URL: $evolutionUrl"
                                )
                                fetchEvolutionChainDetails(evolutionUrl) // <--- ¡LA LLAMADA ESTÁ AQUÍ!
                            } else {
                                Log.w(
                                    "ViewModelEvolution",
                                    "Evolution chain URL is blank for '$pokemonNameLower'."
                                )
                                _evolutionChainDetails.value = null
                                _isLoadingEvolutionChain.value =
                                    false // Importante resetear el loading
                            }
                        } ?: run {
                            // Esto se ejecuta si speciesData.evolutionChain es null O speciesData.evolutionChain.url es null
                            Log.w(
                                "ViewModelEvolution",
                                "No evolution_chain data or URL found for '$pokemonNameLower'."
                            )
                            _evolutionChainDetails.value = null
                            _isLoadingEvolutionChain.value = false // Importante resetear el loading
                        }
                    } else {
                        Log.w(
                            "PokemonViewModel",
                            "Species response for '$pokemonNameLower' successful but body (speciesData) is null. Code: ${speciesResponse.code()}"
                        )
                        // No es necesariamente un error fatal si los detalles básicos están bien y ya se manejó el error de detalles.
                    }
                } else {
                    // speciesResponse no fue successful, el log ya está arriba.
                    // Asegurarse de que la descripción se establece en null si la llamada falla.
                    _pokemonDescription.value = null
                    Log.w(
                        "PokemonViewModel",
                        "Species call for '$pokemonNameLower' was not successful, description will be null."
                    )
                    // Decide si mostrar este error si la llamada de detalles ya falló.
                    if (detailResponse.isSuccessful) { // Solo muestra este error si los detalles principales se cargaron bien
                        handleError("Species Error: ${speciesResponse.code()} ${speciesResponse.message()}")
                    }
                }




                // Limpia el error general si al menos la llamada principal (detalles) fue exitosa y no se mostró un error específico.
                if (detailResponse.isSuccessful && !errorShownThisFetch) {
                    _error.value = null
                }



            } catch (e: Exception) {
                Log.e(
                    "PokemonViewModel",
                    "Exception fetching Pokemon data for $pokemonNameLower",
                    e
                )
                handleError("Exception: ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingDetails.value = false
            }
        }
    }

    // --- Funciones de Ayuda ---
    private fun handleError(errorMessage: String) {
        if (!errorShownThisFetch) {
            _error.value = errorMessage
            errorShownThisFetch =
                true // Marca que un error ya se ha mostrado para esta operación de "fetch"
        }
    }

    fun clearError() {
        _error.value = null
        // No reseteamos errorShownThisFetch aquí, porque se resetea al inicio de una nueva operación de fetch.
    }

    fun clearPokemonDetails() {
        _pokemonDetails.value = null
        _pokemonDescription.value = null
        _isLoadingDetails.value = false
        _error.value =
            null // También limpia el error al salir de la pantalla de detalles, por ejemplo
    }

    // Para la paginación de la lista
    fun loadNextPokemonListPage() {
        if (canLoadMore && _isLoadingList.value == false) {
            fetchMorePokemonItems()
        }
    }

    fun fetchGenerations() {
        if (_isLoadingGenerations.value == true || _generations.value?.isNotEmpty() == true) return
        _isLoadingGenerations.value = true
        viewModelScope.launch {
            try {
                Log.d("PokemonViewModel", "Fetching generations...")
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getGenerationList().execute()
                }
                if (response.isSuccessful) {
                    val generationList = response.body()?.results ?: emptyList()
                    _generations.value = generationList
                    Log.d("PokemonViewModel", "Generations fetched: ${generationList.size}")
                    // Opcionalmente, podrías cargar los Pokémon de la primera generación aquí
                    // if (generationList.isNotEmpty()) {
                    //     fetchPokemonForGeneration(generationList.first().getGenerationIdFromUrl())
                    // }
                } else {
                    Log.e(
                        "PokemonViewModel",
                        "Error fetching generations: ${response.code()} - ${response.message()}"
                    )
                    handleError("Error fetching generations: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PokemonViewModel", "Exception fetching generations", e)
                handleError("Exception (generations): ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingGenerations.value = false
            }
        }
    }


    // Extensión para obtener el ID de la generación desde la URL de NamedApiResource
    private fun NamedApiResource.getGenerationIdFromUrl(): Int? {
        // URL es como "https://pokeapi.co/api/v2/generation/1/"
        return url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()
    }

    fun fetchPokemonForGeneration(generationId: Int?, forceRefresh: Boolean = false) {
        if (generationId == null) {
            _error.value = "Invalid generation ID."
            _isLoadingPokemonForCurrentGeneration.value = false // Asegúrate de resetear el loading
            return
        }

        // Si ya está en caché y no se está forzando la recarga, no hacer nada.
        if (!forceRefresh && _pokemonByGenerationCache.value?.containsKey(generationId) == true) {
            Log.d("PokemonViewModel", "Pokémon for generation $generationId already in cache.")
            // Si la UI podría estar mostrando "cargando" incorrectamente
            if (_pokemonByGenerationCache.value?.get(generationId)?.isNotEmpty() == true) {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
            return
        }

        // Evitar múltiples cargas simultáneas para la misma generación o cualquier otra
        if (_isLoadingPokemonForCurrentGeneration.value == true && !forceRefresh) {
            Log.d("PokemonViewModel", "Already loading Pokémon for a generation.")
            return
        }

        _isLoadingPokemonForCurrentGeneration.value = true
        errorShownThisFetch = false // Resetea para esta operación

        viewModelScope.launch {
            try {
                Log.d("PokemonViewModel", "Fetching generation details for ID: $generationId")
                // 1. Obtener los detalles de la generación (que contiene la lista de pokemon_species)
                val generationDetailResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.instance.getGenerationDetails(generationId).execute()
                }

                if (generationDetailResponse.isSuccessful) {
                    val generationData = generationDetailResponse.body()
                    val pokemonSpeciesListFromGen = generationData?.pokemonSpecies ?: emptyList()

                    if (pokemonSpeciesListFromGen.isNotEmpty()) {
                        Log.d(
                            "PokemonViewModel",
                            "Found ${pokemonSpeciesListFromGen.size} species for generation $generationId. Fetching summaries..."
                        )
                        val pokemonSummariesDeferred =
                            pokemonSpeciesListFromGen.map { speciesResource ->
                                // Para cada especie en la lista de la generación, lanzamos una tarea asíncrona
                                async(Dispatchers.IO) {
                                    val originalPokemonName =
                                        speciesResource.name // Nombre clave/original (ej: "pikachu")
                                    try {
                                        // 2. Obtener PokemonSpeciesDetails para esta especie (para nombres localizados y otros datos)
                                        val speciesDetailsCall =
                                            RetrofitClient.instance.getPokemonSpeciesDetails(
                                                originalPokemonName
                                            ).execute()
                                        var localizedName =
                                            originalPokemonName // Fallback al nombre original
                                        // var generationFromSpecies: NamedApiResource? = null // No la estamos usando activamente

                                        if (speciesDetailsCall.isSuccessful) {
                                            speciesDetailsCall.body()?.let { speciesDetails ->
                                                speciesDetails.localizedNames.firstOrNull { it.language.name == "es" }?.name?.let {
                                                    localizedName =
                                                        it // Nombre en Español encontrado
                                                }
                                                // generationFromSpecies = speciesDetails.generation // Para referencia
                                            }
                                        } else {
                                            Log.w(
                                                "PokemonViewModel",
                                                "Failed to get species details for $originalPokemonName (Gen: $generationId, Code: ${speciesDetailsCall.code()}). Using original name."
                                            )
                                        }

                                        // 3. Obtener PokemonDetails (para sprites, tipos básicos, ID numérico)
                                        val pokemonDetailsCall =
                                            RetrofitClient.instance.getPokemonDetails(
                                                originalPokemonName
                                            ).execute()

                                        if (pokemonDetailsCall.isSuccessful) {
                                            pokemonDetailsCall.body()?.let { detail ->
                                                PokemonSummary(
                                                    id = detail.id,
                                                    name = localizedName, // Usar el nombre localizado (español o fallback)
                                                    spriteUrl = detail.sprites.other?.officialArtwork?.frontDefault
                                                        ?: detail.sprites.frontDefault,
                                                    types = detail.types.map { it.type.name },
                                                    colorName = speciesDetailsCall.body()?.color?.name
                                                )
                                            }
                                        } else {
                                            Log.e(
                                                "PokemonViewModel",
                                                "Error fetching Pokemon details for $originalPokemonName (Gen: $generationId, Code: ${pokemonDetailsCall.code()}) - ${pokemonDetailsCall.message()}"
                                            )
                                            null // Falló la obtención de detalles cruciales
                                        }
                                    } catch (e: Exception) {
                                        Log.e(
                                            "PokemonViewModel",
                                            "Exception fetching summary for ${speciesResource.name} (Gen: $generationId)",
                                            e
                                        )
                                        null // Excepción durante el proceso para este Pokémon
                                    }
                                }
                            }
                        // Esperar a que todos los Pokémon de la generación se procesen
                        val newSummaries =
                            pokemonSummariesDeferred.awaitAll().filterNotNull().sortedBy { it.id }

                        // Actualizar el caché
                        val currentCache =
                            _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        currentCache[generationId] = newSummaries
                        _pokemonByGenerationCache.value = currentCache
                        Log.d(
                            "PokemonViewModel",
                            "Fetched ${newSummaries.size} Pokémon summaries for generation $generationId."
                        )
                    } else {
                        // Generación sin Pokémon o error al obtener la lista de especies
                        val currentCache =
                            _pokemonByGenerationCache.value?.toMutableMap() ?: mutableMapOf()
                        currentCache[generationId] =
                            emptyList() // Guardar lista vacía para no reintentar innecesariamente
                        _pokemonByGenerationCache.value = currentCache
                        Log.w(
                            "PokemonViewModel",
                            "No Pokémon species found for generation $generationId."
                        )
                    }
                    if (!errorShownThisFetch) _error.value =
                        null // Limpiar errores si esta parte fue bien

                } else {
                    Log.e(
                        "PokemonViewModel",
                        "Error fetching generation details for ID $generationId: ${generationDetailResponse.code()} - ${generationDetailResponse.message()}"
                    )
                    handleError("Error fetching generation $generationId details: ${generationDetailResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e(
                    "PokemonViewModel",
                    "Exception fetching Pokémon for generation $generationId",
                    e
                )
                handleError("Exception (fetching gen $generationId Pokémon): ${e.message ?: "Unknown exception"}")
            } finally {
                _isLoadingPokemonForCurrentGeneration.value = false
            }
        }
    }


    // (Opcional) Estado de carga específico para la cadena de evolución
    private val _isLoadingEvolutionChain = MutableLiveData<Boolean>(false)
    val isLoadingEvolutionChain: LiveData<Boolean> = _isLoadingEvolutionChain

    private fun fetchEvolutionChainDetails(evolutionChainUrl: String) {
        Log.d(
            "ViewModelEvolution",
            "fetchEvolutionChainDetails - URL: $evolutionChainUrl, isLoadingEvolutionChain: ${_isLoadingEvolutionChain.value}"
        )

        // Validar que la URL no esté vacía antes de proceder.
        if (evolutionChainUrl.isBlank()) {
            Log.w("PokemonViewModel", "fetchEvolutionChainDetails fue llamada con una URL vacía.")
            _evolutionChainDetails.value = null // Asegurar que el LiveData esté limpio.
            _isLoadingEvolutionChain.value = false // Resetear el estado de carga.
            return
        }

        _isLoadingEvolutionChain.value = true // Indicar que la carga ha comenzado.
        _evolutionChainDetails.value = null // Limpiar datos anteriores mientras se carga.

        viewModelScope.launch {
            try {
                Log.d(
                    "PokemonViewModel",
                    "Iniciando la obtención de detalles de la cadena de evolución desde: $evolutionChainUrl"
                )

                // Realizar la llamada de red en un contexto de IO.
                val response = withContext(Dispatchers.IO) {
                    // La instancia de Retrofit y el servicio se encargan de la llamada usando la @Url.
                    RetrofitClient.instance.getEvolutionChainDetails(evolutionChainUrl).execute()
                }

                // Procesar la respuesta.
                if (response.isSuccessful) {
                    val chainData = response.body()
                    _evolutionChainDetails.value =
                        chainData // Actualizar LiveData con los datos obtenidos.

                    if (chainData != null) {
                        Log.i(
                            "PokemonViewModel",
                            "Detalles de la cadena de evolución obtenidos con éxito. ID de Cadena: ${chainData.id}, Especie Base: ${chainData.chain.species.name}"
                        )
                    } else {
                        // Esto puede ocurrir si el servidor devuelve un 200 OK pero con cuerpo nulo.
                        Log.w(
                            "PokemonViewModel",
                            "Respuesta de cadena de evolución exitosa pero el cuerpo es nulo. URL: $evolutionChainUrl"
                        )
                    }
                } else {
                    // La llamada no fue exitosa (ej. error 404, 500).
                    val errorBody =
                        response.errorBody()?.string() ?: "Error desconocido en cadena de evolución"
                    Log.e(
                        "PokemonViewModel",
                        "Error al obtener detalles de la cadena de evolución. Código: ${response.code()}, Mensaje: ${response.message()}, Cuerpo del Error: $errorBody, URL: $evolutionChainUrl"
                    )
                    // Considera si quieres propagar este error a la UI a través de tu LiveData _error.
                    // handleError("Error en Cadena de Evolución: ${response.code()}")
                }
            } catch (e: Exception) {
                // Capturar cualquier excepción durante la llamada de red o procesamiento.
                Log.e(
                    "PokemonViewModel",
                    "Excepción al obtener detalles de la cadena de evolución desde URL: $evolutionChainUrl",
                    e
                )
                // Considera propagar este error.
                // handleError("Excepción (Cadena Evolución): ${e.message ?: "Excepción desconocida"}")
            } finally {
                // Asegurar que el estado de carga se resetea independientemente del resultado.
                _isLoadingEvolutionChain.value = false
            }
        }
    }

    fun fetchTypeInteractionsForPokemon(pokemonDetail: PokemonDetailResponse) {
        if (pokemonDetail.types.isEmpty()) {
            _typeInteractions.value = emptyList()
            return
        }
        _isLoadingTypeInteractions.value = true
        viewModelScope.launch {
            val interactions = getCombinedDefensiveInteractions(pokemonDetail.types, pokemonApiService) // pokemonApiService debe estar disponible en tu ViewModel
            _typeInteractions.postValue(interactions)
            _isLoadingTypeInteractions.postValue(false)
        }
    }

    private fun loadSpecialFormsForCurrentPokemon(speciesApiUrl: String, basePokemonName: String) {
        viewModelScope.launch {
            _isLoadingSpecialForms.value = true
            // No reseteamos _specialForms.value = emptyList() aquí, porque ya se hizo al inicio de fetchPokemonDetailsByName

            Log.d("PVM_Forms", "Loading special forms for species URL: $speciesApiUrl")
            try {
                // Llama al método del servicio que definimos para obtener PokemonSpeciesDetailResponse
                // Esta debe ser una función suspendida en tu PokeApiService
                val speciesDetailResponse = pokemonApiService.getSpeciesDetailsByUrl(speciesApiUrl)

                if (speciesDetailResponse.isSuccessful) {
                    val formsFound = mutableListOf<Pair<String, PokemonSpeciesVariety>>()
                    speciesDetailResponse.body()?.let { speciesDetails -> // speciesDetails es PokemonSpeciesDetailResponse
                        Log.d("PVM_Forms", "Successfully fetched species details for forms. Found ${speciesDetails.varieties.size} varieties.")
                        speciesDetails.varieties.forEach { variety ->
                            if (!variety.isDefault &&
                                (variety.pokemon.name.contains("-mega", ignoreCase = true) ||
                                        variety.pokemon.name.contains("-gmax", ignoreCase = true) ||
                                        variety.pokemon.name.contains("-gigantamax", ignoreCase = true) ||
                                        variety.pokemon.name.endsWith("-x", ignoreCase = true) ||
                                        variety.pokemon.name.endsWith("-y", ignoreCase = true) ||
                                        variety.pokemon.name.contains("-primal", ignoreCase = true)
                                        // Añade más condiciones si es necesario (ej. -alola, -galar, si las quieres aquí)
                                        )
                            ) {
                                Log.d("PVM_Forms", "Found special form: ${variety.pokemon.name} for base: $basePokemonName")
                                formsFound.add(Pair(basePokemonName, variety))
                            }
                        }
                    }
                    _specialForms.value = formsFound // Actualiza el StateFlow
                    Log.d("PVM_Forms", "Finished loading forms. Total found: ${formsFound.size}")
                } else {
                    Log.e("PVM_Forms", "Error fetching species details for forms: ${speciesDetailResponse.code()} - ${speciesDetailResponse.message()}")
                    _specialForms.value = emptyList() // Limpia si hay error
                    // Considera llamar a handleError si quieres mostrar un mensaje específico para este fallo
                }
            } catch (e: Exception) {
                Log.e("PVM_Forms", "Exception loading special forms for $speciesApiUrl", e)
                _specialForms.value = emptyList() // Limpia si hay excepción
                // Considera llamar a handleError
            } finally {
                _isLoadingSpecialForms.value = false
            }
        }
    }


    private val _pokemonTypes = MutableLiveData<List<String>>(listOf(NO_TYPE_SELECTED) + ALL_POKEMON_TYPES) // Valor inicial/fallback
    val pokemonTypes: LiveData<List<String>> = _pokemonTypes

//    private val _isLoadingTypes = MutableLiveData<Boolean>(false)
//    val isLoadingTypes: LiveData<Boolean> = _isLoadingTypes
//
//    fun fetchAllPokemonTypes() {
//        if (_isLoadingTypes.value == true || (_pokemonTypes.value != null && _pokemonTypes.value!!.size > ALL_POKEMON_TYPES.size + 1)) {
//            // Ya está cargando o ya tiene más tipos que la lista por defecto (asume que se cargaron de la API)
//            return
//        }
//        _isLoadingTypes.value = true
//        viewModelScope.launch {
//            try {
//                val response = pokemonApiService.getAllPokemonTypes()
//                if (response.isSuccessful && response.body() != null) {
//                    val typeNames = response.body()!!.results.map { it.name }
//                    _pokemonTypes.postValue(listOf(NO_TYPE_SELECTED) + typeNames.sorted()) // Añade "Sin tipo" y ordena
//                } else {
//                    _error.postValue("Failed to load Pokémon types: ${response.message()}")
//                    // Mantener el valor de fallback si falla
//                    if (_pokemonTypes.value == null || _pokemonTypes.value!!.size <=1) { // si solo tenia "sin tipo" o estaba vacio
//                        _pokemonTypes.postValue(listOf(NO_TYPE_SELECTED) + ALL_POKEMON_TYPES)
//                    }
//                }
//            } catch (e: Exception) {
//                _error.postValue("Error fetching Pokémon types: ${e.localizedMessage}")
//                if (_pokemonTypes.value == null || _pokemonTypes.value!!.size <=1) {
//                    _pokemonTypes.postValue(listOf(NO_TYPE_SELECTED) + ALL_POKEMON_TYPES)
//                }
//            } finally {
//                _isLoadingTypes.value = false
//            }
//        }
//    }



}

