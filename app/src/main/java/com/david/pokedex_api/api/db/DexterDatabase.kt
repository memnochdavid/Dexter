package com.david.pokedex_api.api.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PokemonSummaryEntity::class, MoveSummaryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DexterDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao

    companion object {
        @Volatile
        private var INSTANCE: DexterDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `move_summary` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `localizedName` TEXT NOT NULL,
                        `typeName` TEXT,
                        `damageClass` TEXT,
                        `power` INTEGER,
                        `pp` INTEGER,
                        `accuracy` INTEGER,
                        `description` TEXT
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): DexterDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    DexterDatabase::class.java,
                    "dexter_db"
                )
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
            }
        }
    }
}
