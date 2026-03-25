package com.david.pokedex_api.api.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.david.pokedex_api.api.model.ItemSummary

@Entity(tableName = "item_summary")
data class ItemSummaryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val localizedName: String,
    val category: String?,
    val cost: Int?,
    val effect: String?,
    val spriteUrl: String?
) {
    fun toItemSummary(): ItemSummary = ItemSummary(id, name, localizedName, category, cost, effect, spriteUrl)

    companion object {
        fun from(s: ItemSummary): ItemSummaryEntity =
            ItemSummaryEntity(s.id, s.name, s.localizedName, s.category, s.cost, s.effect, s.spriteUrl)
    }
}
