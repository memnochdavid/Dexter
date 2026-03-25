package com.david.pokedex_api.api.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.david.pokedex_api.api.model.BerrySummary

@Entity(tableName = "berry_summary")
data class BerrySummaryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val localizedName: String,
    val naturalGiftType: String?,
    val naturalGiftPower: Int,
    val growthTime: Int,
    val size: Int,
    val smoothness: Int,
    val maxHarvest: Int,
    val spriteUrl: String?,
    val flavors: String // JSON-like "spicy:10,sweet:0,..."
) {
    fun toBerrySummary(): BerrySummary = BerrySummary(
        id, name, localizedName, naturalGiftType, naturalGiftPower,
        growthTime, size, smoothness, maxHarvest, spriteUrl,
        flavors.split(",").filter { it.contains(":") }.associate {
            val parts = it.split(":")
            parts[0] to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
    )

    companion object {
        fun from(s: BerrySummary): BerrySummaryEntity = BerrySummaryEntity(
            s.id, s.name, s.localizedName, s.naturalGiftType, s.naturalGiftPower,
            s.growthTime, s.size, s.smoothness, s.maxHarvest, s.spriteUrl,
            s.flavors.entries.joinToString(",") { "${it.key}:${it.value}" }
        )
    }
}
