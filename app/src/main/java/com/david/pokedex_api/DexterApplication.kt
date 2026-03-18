package com.david.pokedex_api

import android.app.Application
import com.david.pokedex_api.api.client.RetrofitClient
import com.david.pokedex_api.api.db.DexterDatabase

class DexterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        database = DexterDatabase.getInstance(this)
    }

    companion object {
        lateinit var database: DexterDatabase
            private set
    }
}
