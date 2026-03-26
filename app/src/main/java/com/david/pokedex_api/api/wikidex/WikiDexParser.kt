package com.david.pokedex_api.api.wikidex

import org.jsoup.nodes.Document

/**
 * Interface generica para extraer datos de una pagina de WikiDex.
 * Implementar para cada tipo de dato que se quiera scrapear.
 */
interface WikiDexParser<T> {
    fun parse(doc: Document): T?
}

/**
 * Extrae las descripciones de la Pokedex agrupadas por edicion.
 * Retorna lista de pares (nombreEdicionWikiDex, descripcion).
 */
class FlavorTextParser : WikiDexParser<List<Pair<String, String>>> {

    override fun parse(doc: Document): List<Pair<String, String>>? {
        val table = doc.selectFirst("table.pokedex") ?: return null
        val rows = table.select("tr")
        val results = mutableListOf<Pair<String, String>>()

        for (row in rows) {
            val cells = row.select("th, td")
            if (cells.size < 2) continue

            // Descripcion siempre en la ultima celda, edicion en la penultima
            val descCell = cells.last() ?: continue
            val editionCell = cells[cells.size - 2]

            val description = descCell.text().trim()
            if (description.isBlank()) continue
            // Filtrar entradas vacias tipo "No hay entrada de..." o "no aparece en..."
            if (description.contains("No hay entrada de", ignoreCase = true) ||
                description.contains("no aparece en", ignoreCase = true)
            ) continue

            // Extraer nombres de edicion de los <a> tags (maneja ediciones compartidas)
            val links = editionCell.select("a")
            val editionNames = if (links.isNotEmpty()) {
                links.map { it.text().trim() }.filter { it.isNotBlank() }
            } else {
                // Fallback: texto directo de la celda
                val text = editionCell.text().trim()
                if (text.isNotBlank()) listOf(text) else emptyList()
            }

            for (edition in editionNames) {
                results.add(edition to description)
            }
        }

        return results.ifEmpty { null }
    }
}

/**
 * Convierte nombres de edicion de WikiDex a identificadores de version de PokeAPI.
 */
object WikiDexGameMapper {

    private val mapping = mapOf(
        "Rojo" to "red",
        "Azul" to "blue",
        "Amarillo" to "yellow",
        "Oro" to "gold",
        "Plata" to "silver",
        "Cristal" to "crystal",
        "Rubí" to "ruby",
        "Zafiro" to "sapphire",
        "Esmeralda" to "emerald",
        "Rojo Fuego" to "firered",
        "Verde Hoja" to "leafgreen",
        "Diamante" to "diamond",
        "Perla" to "pearl",
        "Platino" to "platinum",
        "Oro HeartGold" to "heartgold",
        "Plata SoulSilver" to "soulsilver",
        "Negro" to "black",
        "Blanco" to "white",
        "Negro 2" to "black-2",
        "Blanco 2" to "white-2",
        "X" to "x",
        "Y" to "y",
        "Rubí Omega" to "omega-ruby",
        "Zafiro Alfa" to "alpha-sapphire",
        "Sol" to "sun",
        "Luna" to "moon",
        "Ultrasol" to "ultra-sun",
        "Ultraluna" to "ultra-moon",
        "Let's Go, Pikachu!" to "lets-go-pikachu",
        "Let's Go, Eevee!" to "lets-go-eevee",
        "Espada" to "sword",
        "Escudo" to "shield",
        "Diamante Brillante" to "brilliant-diamond",
        "Perla Reluciente" to "shining-pearl",
        "Leyendas: Arceus" to "legends-arceus",
        "Escarlata" to "scarlet",
        "Púrpura" to "violet",
        "Leyendas: Z-A" to "legends-za"
    )

    /**
     * Convierte un nombre de edicion WikiDex al identificador de version PokeAPI.
     * Normaliza: quita prefijo "Pokémon ", trim.
     */
    fun toApiVersionName(wikiDexEdition: String): String? {
        val normalized = wikiDexEdition.trim()
            .removePrefix("Pokémon ")
            .removePrefix("Pokemon ")
            .removePrefix("Pokémon: ")
            .trim()
        return mapping[normalized]
    }
}
