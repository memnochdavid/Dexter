package com.david.pokedex_api.api.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PokemonSummaryEntity::class], version = 1, exportSchema = false)
abstract class DexterDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    companion object {
        @Volatile
        private var INSTANCE: DexterDatabase? = null

        fun getInstance(context: Context): DexterDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DexterDatabase::class.java,
                    "dexter_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
