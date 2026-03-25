package com.david.pokedex_api.api.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.david.pokedex_api.api.model.MoveSummary

@Entity(tableName = "move_summary")
data class MoveSummaryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val localizedName: String,
    val typeName: String?,
    val damageClass: String?,
    val power: Int?,
    val pp: Int?,
    val accuracy: Int?,
    val description: String?
) {
    fun toMoveSummary(): MoveSummary = MoveSummary(
        id = id,
        name = name,
        localizedName = localizedName,
        typeName = typeName,
        damageClass = damageClass,
        power = power,
        pp = pp,
        accuracy = accuracy,
        description = description
    )

    companion object {
        fun fromMoveSummary(summary: MoveSummary): MoveSummaryEntity =
            MoveSummaryEntity(
                id = summary.id,
                name = summary.name,
                localizedName = summary.localizedName,
                typeName = summary.typeName,
                damageClass = summary.damageClass,
                power = summary.power,
                pp = summary.pp,
                accuracy = summary.accuracy,
                description = summary.description
            )
    }
}
