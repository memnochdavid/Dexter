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

            val description = descCell.html().trim()
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
 * Extrae las localizaciones/encuentros agrupados por edicion.
 * Retorna lista de pares (nombreEdicionWikiDex, textoLocalizacion).
 * Usa la tabla con class "localizacion" (primera de la pagina = juegos principales).
 */
class LocationParser : WikiDexParser<List<Pair<String, String>>> {

    override fun parse(doc: Document): List<Pair<String, String>>? {
        val table = doc.selectFirst("table.localizacion") ?: return null
        val rows = table.select("tr")
        val results = mutableListOf<Pair<String, String>>()

        for (row in rows) {
            val cells = row.select("th, td")
            if (cells.size < 2) continue

            val locationCell = cells.last() ?: continue
            val editionCell = cells[cells.size - 2]

            val locationText = locationCell.html().trim()
            if (locationText.isBlank()) continue
            if (locationText.contains("no aparece en", ignoreCase = true)) continue

            val links = editionCell.select("a")
            val editionNames = if (links.isNotEmpty()) {
                links.map { it.text().trim() }.filter { it.isNotBlank() }
            } else {
                val text = editionCell.text().trim()
                if (text.isNotBlank()) listOf(text) else emptyList()
            }

            for (edition in editionNames) {
                results.add(edition to locationText)
            }
        }

        return results.ifEmpty { null }
    }
}

/**
 * Extrae sprites HOME de la sección "Formas" de WikiDex.
 * Retorna mapa: clave normalizada (nombre de fichero sin _HOME.png, lowercase) → URL de la imagen.
 * Ejemplo: "arceus_fuego" → "https://images.wikidexcdn.net/.../Arceus_fuego_HOME.png/120px-..."
 */
class FormSpritesParser : WikiDexParser<Map<String, String>> {

    override fun parse(doc: Document): Map<String, String>? {
        // Buscar encabezado "Formas" (h2 o h3)
        val formasHeading = doc.select("h2, h3").firstOrNull { heading ->
            heading.select(".mw-headline").text().equals("Formas", ignoreCase = true)
        } ?: return null

        // Recoger imágenes _HOME.png entre este heading y el siguiente h2/h3
        val result = mutableMapOf<String, String>()
        var sibling = formasHeading.nextElementSibling()
        while (sibling != null && !sibling.tagName().matches(Regex("h[23]"))) {
            for (img in sibling.select("img")) {
                val src = img.attr("src")
                if (!src.contains("_HOME.png", ignoreCase = true)) continue
                // Ignorar iconos pequeños (48px o menos) y tipos/tablas
                val widthAttr = img.attr("width").toIntOrNull() ?: 0
                if (widthAttr in 1..60) continue

                // Extraer nombre de fichero del src
                // Formato: .../Arceus_fuego_HOME.png/120px-Arceus_fuego_HOME.png
                val fileName = src.substringAfterLast("/")       // "120px-Arceus_fuego_HOME.png"
                    .replace(Regex("^\\d+px-"), "")              // "Arceus_fuego_HOME.png"
                val key = fileName
                    .removeSuffix(".png")                        // "Arceus_fuego_HOME"
                    .removeSuffix("_HOME")                       // "Arceus_fuego"
                    .lowercase()                                 // "arceus_fuego"
                    .let { java.net.URLDecoder.decode(it, "UTF-8") } // decode %C3%B3n → ón

                // URL a mayor resolución (120px)
                val url = src.replace(Regex("/\\d+px-"), "/120px-")

                if (key.isNotBlank() && !result.containsKey(key)) {
                    result[key] = url
                }
            }
            sibling = sibling.nextElementSibling()
        }
        return result.ifEmpty { null }
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
