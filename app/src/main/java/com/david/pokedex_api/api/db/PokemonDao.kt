package com.david.pokedex_api.api.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PokemonDao {

    @Query("SELECT * FROM pokemon_summary WHERE generationId = :generationId ORDER BY id ASC")
    suspend fun getSummariesByGeneration(generationId: Int): List<PokemonSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummaries(summaries: List<PokemonSummaryEntity>)

    @Query("SELECT COUNT(*) FROM pokemon_summary WHERE generationId = :generationId")
    suspend fun countByGeneration(generationId: Int): Int

    // --- Move Summary ---
    @Query("SELECT * FROM move_summary ORDER BY id ASC")
    suspend fun getAllMoveSummaries(): List<MoveSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoveSummaries(summaries: List<MoveSummaryEntity>)

    @Query("SELECT COUNT(*) FROM move_summary")
    suspend fun countMoveSummaries(): Int

    // --- Item Summary ---
    @Query("SELECT * FROM item_summary ORDER BY id ASC")
    suspend fun getAllItemSummaries(): List<ItemSummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItemSummaries(summaries: List<ItemSummaryEntity>)

    // --- Berry Summary ---
    @Query("SELECT * FROM berry_summary ORDER BY id ASC")
    suspend fun getAllBerrySummaries(): List<BerrySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBerrySummaries(summaries: List<BerrySummaryEntity>)

    // --- WikiDex Cache ---
    @Query("SELECT * FROM wikidex_cache WHERE pokemonName = :name AND dataType = :type")
    suspend fun getWikiDexCache(name: String, type: String): List<WikiDexCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWikiDexCache(entries: List<WikiDexCacheEntity>)
}
