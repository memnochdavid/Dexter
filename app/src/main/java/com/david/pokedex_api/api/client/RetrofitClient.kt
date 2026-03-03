package com.david.pokedex_api.api.client

import com.david.pokedex_api.api.service.PokeApiService
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://pokeapi.co/api/v2/"

    // Aumentamos el número de conexiones simultáneas permitidas
    private val dispatcher = Dispatcher().apply {
        maxRequests = 100
        maxRequestsPerHost = 20
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: PokeApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(PokeApiService::class.java)
    }
}
