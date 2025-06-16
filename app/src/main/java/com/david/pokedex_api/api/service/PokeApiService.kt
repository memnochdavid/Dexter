package com.david.pokedex_api.api.service

import com.david.pokedex_api.api.model.AbilityDetailResponse
import com.david.pokedex_api.api.model.EvolutionChainDetailResponse
import com.david.pokedex_api.api.model.GenerationDetailResponse
import com.david.pokedex_api.api.model.GenerationListResponse
import com.david.pokedex_api.api.model.MoveDetailResponse
import com.david.pokedex_api.api.model.PokemonDetailResponse
import com.david.pokedex_api.api.model.PokemonListResponse
import com.david.pokedex_api.api.model.PokemonSpeciesDetailResponse
import com.david.pokedex_api.api.model.PokemonSpeciesResponse
import com.david.pokedex_api.api.model.TypeDetailResponse
import com.david.pokedex_api.api.model.TypeListResponse
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface PokeApiService {
    @GET("pokemon-species/{name}")
    fun getPokemon(@Path("name") name: String): Call<PokemonDetailResponse>

    @GET("pokemon/{id}")
    fun getPokemonById(@Path("id") id: Int): Call<PokemonDetailResponse>

    @GET("type")
    fun getTypes(): Call<List<PokemonDetailResponse>>

    @GET("pokemon")
    fun getPokemonList(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ): Call<PokemonListResponse>

    @GET("pokemon/{name}") // Este endpoint es mejor para la información que buscas inicialmente
    fun getPokemonDetails(@Path("name") name: String): Call<PokemonDetailResponse>


    // Nuevo método para obtener datos de la especie, incluyendo descripción
    @GET("pokemon-species/{name}")
    fun getPokemonSpeciesDetails(@Path("name") name: String): Call<PokemonSpeciesResponse>

    @GET("generation")
    fun getGenerationList(): Call<GenerationListResponse>

    @GET("generation/{id}") // O puedes usar el nombre si lo prefieres
    fun getGenerationDetails(@Path("id") id: Int): Call<GenerationDetailResponse>

    @GET
    fun getEvolutionChainDetails(@Url evolutionChainUrl: String): Call<EvolutionChainDetailResponse>

    @GET // La URL completa se pasará dinámicamente
    suspend fun getMoveDetails(@Url url: String): Response<MoveDetailResponse>
    @GET
    suspend fun getAbilityDetails(@Url url: String): Response<AbilityDetailResponse>

    @GET("type/{name}") // O por ID si prefieres
    suspend fun getTypeDetails(@Path("name") typeName: String): Response<TypeDetailResponse>



    // Ya deberías tener algo como esto para obtener los detalles de un Pokémon específico por nombre
    // que necesitarás para obtener el sprite de la mega/gigantamax forma
    @GET("pokemon/{name}")
    suspend fun getPokemonDetailsByName(@Path("name") name: String): Response<PokemonSpeciesDetailResponse>

    @GET("pokemon/{name}")
    suspend fun getFullPokemonDetails(@Path("name") name: String): Response<PokemonDetailResponse>

    @GET // La URL completa se pasa dinámicamente
    suspend fun getSpeciesDetailsByUrl(@Url speciesUrl: String): Response<PokemonSpeciesDetailResponse>
    @GET("pokemon/{name}")
    suspend fun getPokemonDetailsByNameForSprite(@Path("name") pokemonFormName: String): Response<PokemonDetailResponse>

    @GET("type") // El endpoint para obtener la lista de todos los tipos
    suspend fun getAllPokemonTypes(): Response<TypeListResponse>

    @GET
    suspend fun getPokemonSpeciesByUrl(@Url url: String): Response<PokemonSpeciesResponse> // Necesitarás crear PokemonSpeciesResponse y sus modelos internos

}