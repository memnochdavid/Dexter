package com.david.pokedex_api.api.wikidex

import android.util.Log
import com.david.pokedex_api.api.db.PokemonDao
import com.david.pokedex_api.api.db.WikiDexCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WikiDexRepository(private val dao: PokemonDao) {

    companion object {
        private const val TAG = "WikiDexRepository"
        private const val BASE_URL = "https://www.wikidex.net/wiki/"
        private const val DATA_TYPE_FLAVOR = "flavor_text"
    }

    private val fetcher = WikiDexFetcher()
    private val flavorParser = FlavorTextParser()

    /**
     * Obtiene las descripciones de Pokedex en español desde WikiDex.
     * Primero consulta la cache Room; si no hay datos, scrapea y cachea.
     *
     * @param spanishName Nombre del Pokemon en español (para construir la URL)
     * @return Map<apiVersionName, descripcion> o emptyMap si falla
     */
    suspend fun getFlavorTexts(spanishName: String): Map<String, String> {
        // 1. Consultar cache
        val cached = withContext(Dispatchers.IO) {
            dao.getWikiDexCache(spanishName, DATA_TYPE_FLAVOR)
        }
        if (cached.isNotEmpty()) {
            Log.d(TAG, "Cache hit para '$spanishName' (${cached.size} entradas)")
            return cached.associate { it.dataKey to it.value }
        }

        // 2. Scrapear WikiDex
        val url = BASE_URL + spanishName.replace(" ", "_")
        Log.d(TAG, "Scrapeando WikiDex: $url")
        val doc = fetcher.fetchDocument(url) ?: return emptyMap()

        // 3. Parsear
        val wikiEntries = flavorParser.parse(doc) ?: return emptyMap()

        // 4. Mapear nombres de edicion WikiDex → identificadores PokeAPI
        val mapped = mutableMapOf<String, String>()
        for ((edition, desc) in wikiEntries) {
            val apiVersion = WikiDexGameMapper.toApiVersionName(edition)
            if (apiVersion != null && !mapped.containsKey(apiVersion)) {
                mapped[apiVersion] = desc
            }
        }

        // 5. Cachear en Room
        if (mapped.isNotEmpty()) {
            val entities = mapped.map { (version, desc) ->
                WikiDexCacheEntity(
                    pokemonName = spanishName,
                    dataType = DATA_TYPE_FLAVOR,
                    dataKey = version,
                    value = desc,
                    fetchedAtMillis = System.currentTimeMillis()
                )
            }
            withContext(Dispatchers.IO) { dao.insertWikiDexCache(entities) }
            Log.d(TAG, "Cacheadas ${entities.size} descripciones para '$spanishName'")
        }

        return mapped
    }
}
