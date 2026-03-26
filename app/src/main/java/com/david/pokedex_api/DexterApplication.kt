package com.david.pokedex_api

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.david.pokedex_api.api.client.RetrofitClient
import com.david.pokedex_api.api.db.DexterDatabase

class DexterApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        RetrofitClient.init(this)
        database = DexterDatabase.getInstance(this)
    }

    // Coil ImageLoader global optimizado para dispositivos gama baja
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // 20% de la RAM disponible
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizePercent(0.05) // 5% del disco
                    .build()
            }
            .crossfade(false) // Desactivar crossfade global (mejor rendimiento en scroll)
            .allowHardware(true) // Bitmaps hardware = menos memoria, más rápido
            .build()
    }

    companion object {
        lateinit var database: DexterDatabase
            private set
        lateinit var appContext: android.content.Context
            private set
    }
}
