package com.david.pokedex_api.api.service

import com.david.pokedex_api.api.model.AbilityDetailResponse
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.GenerationDetailResponse
import com.david.pokedex_api.api.model.GenerationListResponse
import com.david.pokedex_api.api.model.GenericNamedResourceDetail
import com.david.pokedex_api.api.model.ItemDetailResponse
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.BerryDetailResponse
import com.david.pokedex_api.api.model.BerryListResponse
import com.david.pokedex_api.api.model.ItemListResponse
import com.david.pokedex_api.api.model.LocationAreaDetailResponse
import com.david.pokedex_api.api.model.LocationDetailResponse
import com.david.pokedex_api.api.model.RegionDetailResponse
import com.david.pokedex_api.api.model.RegionListResponse
import com.david.pokedex_api.api.model.MoveListResponse
import com.david.pokedex_api.api.model.PokemonEncounterResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonFormDetailResponse
import com.david.pokedex_api.api.model.PokemonListResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
import com.david.pokedex_api.api.model.TypeDetailResponse
import com.david.pokedex_api.api.model.TypeListResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PokeApiService {

    // --- Pokémon Details & Species ---
    @GET("pokemon/{name}")
    suspend fun getPokemonDetails(@Path("name") name: String): Response<PokemonDetailResponse>

    @GET("pokemon-form/{name_or_id}") // Can be name or ID from the form URL
    suspend fun getPokemonFormDetails(@Path("name_or_id") nameOrId: String): Response<PokemonFormDetailResponse>

    @GET // For fetching by full URL
    suspend fun getPokemonFormDetailsByUrl(@Url url: String): Response<PokemonFormDetailResponse>


    @GET("pokemon/{id}")
    suspend fun getPokemonDetailsById(@Path("id") id: Int): Response<PokemonDetailResponse>

    @GET("pokemon-species/{name}")
    suspend fun getPokemonSpeciesDetails(@Path("name") name: String): Response<PokemonSpeciesResponse> // Asumiendo PokemonSpeciesResponse es tu modelo detallado para especies

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpeciesDetailsById(@Path("id") id: Int): Response<PokemonSpeciesResponse> // Asumiendo PokemonSpeciesResponse

    // --- Lists ---
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Response<PokemonListResponse>

    @GET("generation")
    suspend fun getGenerationList(): Response<GenerationListResponse>

    @GET("generation/{id}")
    suspend fun getGenerationDetails(@Path("id") id: Int): Response<GenerationDetailResponse> // Contiene lista de pokemon_species para esa generación

    @GET("type")
    suspend fun getAllPokemonTypes(): Response<TypeListResponse> // Lista de todos los tipos (NamedApiResource)

    // --- Dynamic URL Fetches (Specific Resources) ---
    // Estas son para cuando obtienes una URL completa de otra respuesta de la API.

    @GET
    suspend fun getEvolutionChainDetailsByUrl(@Url evolutionChainUrl: String): Response<EvolutionChainDetailResponse>

    @GET
    suspend fun getMoveDetailsByUrl(@Url url: String): Response<MoveDetailResponse>

    @GET
    suspend fun getAbilityDetailsByUrl(@Url url: String): Response<AbilityDetailResponse>

    @GET
    suspend fun getItemDetailsByUrl(@Url url: String): Response<ItemDetailResponse>

    // Para obtener detalles de un tipo específico por su nombre (o ID si lo prefieres)
    // Esto es diferente de getAllPokemonTypes, que solo da la lista de nombres/URLs.
    @GET("type/{name}")
    suspend fun getTypeDetailsByName(@Path("name") typeName: String): Response<TypeDetailResponse>

    @GET("type/{id}")
    suspend fun getTypeDetailsById(@Path("id") id: Int): Response<TypeDetailResponse>


    // Para cuando tienes la URL completa de una 'pokemon-species' desde otra respuesta
    @GET
    suspend fun getPokemonSpeciesDetailsByUrl(@Url url: String): Response<PokemonSpeciesResponse>

    // Función genérica para recursos que tienen una estructura común de 'names' (para localización)
    // y otros campos básicos. Podrías necesitar ser más específico si las estructuras varían mucho.
    // Ejemplos: "location", "region".
    @GET
    suspend fun getGenericNamedResourceDetailsByUrl(@Url url: String): Response<GenericNamedResourceDetail>

    // --- Encounters ---
    @GET("pokemon/{id}/encounters")
    suspend fun getPokemonEncounters(@Path("id") id: Int): Response<List<PokemonEncounterResponse>>

    // --- Move Browser ---
    @GET("move")
    suspend fun getMoveList(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 2000
    ): Response<MoveListResponse>

    @GET("move/{id}")
    suspend fun getMoveDetailsById(@Path("id") id: Int): Response<MoveDetailResponse>

    @GET("move/{name}")
    suspend fun getMoveDetailsByName(@Path("name") name: String): Response<MoveDetailResponse>

    // --- Items ---
    @GET("item")
    suspend fun getItemList(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 2000
    ): Response<ItemListResponse>

    // --- Berries ---
    @GET("berry")
    suspend fun getBerryList(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<BerryListResponse>

    @GET
    suspend fun getBerryDetailsByUrl(@Url url: String): Response<BerryDetailResponse>

    // --- Regions & Locations ---
    @GET("region")
    suspend fun getRegionList(@Query("limit") limit: Int = 100): Response<RegionListResponse>

    @GET
    suspend fun getRegionDetailsByUrl(@Url url: String): Response<RegionDetailResponse>

    @GET
    suspend fun getLocationDetailsByUrl(@Url url: String): Response<LocationDetailResponse>

    @GET
    suspend fun getLocationAreaDetailsByUrl(@Url url: String): Response<LocationAreaDetailResponse>

}