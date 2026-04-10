package com.david.pokedex_api.api.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.david.pokedex_api.api.model.PokemonSummary

@Entity(tableName = "pokemon_summary")
data class PokemonSummaryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val spriteUrl: String?,
    val types: String,       // Tipos separados por coma: "Fire,Flying"
    val colorName: String?,
    val generationId: Int,
    val evoChainLength: Int = 0
) {
    fun toPokemonSummary(): PokemonSummary = PokemonSummary(
        id = id,
        name = name,
        spriteUrl = spriteUrl,
        types = types.split(",").filter { it.isNotBlank() },
        colorName = colorName,
        evoChainLength = evoChainLength
    )

    companion object {
        fun fromPokemonSummary(summary: PokemonSummary, generationId: Int): PokemonSummaryEntity =
            PokemonSummaryEntity(
                id = summary.id,
                name = summary.name,
                spriteUrl = summary.spriteUrl,
                types = summary.types.joinToString(","),
                colorName = summary.colorName,
                generationId = generationId,
                evoChainLength = summary.evoChainLength
            )
    }
}
