package com.david.pokedex_api.api.client

import android.content.Context
import com.david.pokedex_api.api.service.PokeApiService
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://pokeapi.co/api/v2/"

    private var _instance: PokeApiService? = null

    fun init(context: Context) {
        if (_instance != null) return

        // Caché HTTP de 50 MB — la PokeAPI es estática, así que las respuestas
        // se sirven desde disco en lugar de volver a descargarlas.
        val cacheSize = 50L * 1024 * 1024
        val cache = Cache(File(context.cacheDir, "pokeapi_cache"), cacheSize)

        val dispatcher = Dispatcher().apply {
            maxRequests = 100
            maxRequestsPerHost = 50 // Subido de 20 a 50 para mayor paralelismo
        }

        val okHttpClient = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        _instance = retrofit.create(PokeApiService::class.java)
    }

    val instance: PokeApiService
        get() = _instance ?: throw IllegalStateException(
            "RetrofitClient no inicializado. Asegúrate de que DexterApplication.onCreate() se ejecuta primero."
        )
}
