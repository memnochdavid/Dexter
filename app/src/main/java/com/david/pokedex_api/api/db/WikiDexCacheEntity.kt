package com.david.pokedex_api.api.db

import androidx.room.Entity

@Entity(tableName = "wikidex_cache", primaryKeys = ["pokemonName", "dataType", "dataKey"])
data class WikiDexCacheEntity(
    val pokemonName: String,
    val dataType: String,
    val dataKey: String,
    val value: String,
    val fetchedAtMillis: Long
)
