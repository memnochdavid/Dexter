package com.david.pokedex_api.api.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PokemonSummaryEntity::class, MoveSummaryEntity::class, ItemSummaryEntity::class, BerrySummaryEntity::class, WikiDexCacheEntity::class],
    version = 4,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wikidex_cache` (
                        `pokemonName` TEXT NOT NULL,
                        `dataType` TEXT NOT NULL,
                        `dataKey` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `fetchedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`pokemonName`, `dataType`, `dataKey`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `item_summary` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `localizedName` TEXT NOT NULL,
                        `category` TEXT,
                        `cost` INTEGER,
                        `effect` TEXT,
                        `spriteUrl` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `berry_summary` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `localizedName` TEXT NOT NULL,
                        `naturalGiftType` TEXT,
                        `naturalGiftPower` INTEGER NOT NULL,
                        `growthTime` INTEGER NOT NULL,
                        `size` INTEGER NOT NULL,
                        `smoothness` INTEGER NOT NULL,
                        `maxHarvest` INTEGER NOT NULL,
                        `spriteUrl` TEXT,
                        `flavors` TEXT NOT NULL
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { INSTANCE = it }
            }
        }
    }
}
