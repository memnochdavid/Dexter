package com.david.pokedex_api.api.wikidex

import android.util.Log
import com.david.pokedex_api.api.db.PokemonDao
import com.david.pokedex_api.api.db.WikiDexCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document

class WikiDexRepository(private val dao: PokemonDao) {

    companion object {
        private const val TAG = "WikiDexRepository"
        private const val BASE_URL = "https://www.wikidex.net/wiki/"
        private const val DATA_TYPE_FLAVOR = "flavor_text"
        private const val DATA_TYPE_LOCATION = "location"
    }

    private val fetcher = WikiDexFetcher()
    private val flavorParser = FlavorTextParser()
    private val locationParser = LocationParser()
    private val formSpritesParser = FormSpritesParser()

    // Cache del Document por Pokemon para evitar descargar la misma pagina dos veces
    private var cachedDocName: String? = null
    private var cachedDoc: Document? = null
    private val docMutex = Mutex()

    private suspend fun getDocument(spanishName: String): Document? {
        docMutex.withLock {
            if (cachedDocName == spanishName && cachedDoc != null) return cachedDoc
            val url = BASE_URL + spanishName.replace(" ", "_")
            Log.d(TAG, "Scrapeando WikiDex: $url")
            val doc = fetcher.fetchDocument(url)
            cachedDocName = spanishName
            cachedDoc = doc
            return doc
        }
    }

    /**
     * Logica generica: cache Room → fetch → parse → mapear → cachear.
     */
    private suspend fun getMappedData(
        spanishName: String,
        dataType: String,
        parser: WikiDexParser<List<Pair<String, String>>>
    ): Map<String, String> {
        // 1. Consultar cache
        val cached = withContext(Dispatchers.IO) {
            dao.getWikiDexCache(spanishName, dataType)
        }
        if (cached.isNotEmpty()) {
            Log.d(TAG, "Cache hit '$dataType' para '$spanishName' (${cached.size} entradas)")
            return cached.associate { it.dataKey to it.value }
        }

        // 2. Obtener documento (reutiliza si ya se descargo)
        val doc = getDocument(spanishName) ?: return emptyMap()

        // 3. Parsear
        val wikiEntries = parser.parse(doc) ?: return emptyMap()

        // 4. Mapear nombres de edicion WikiDex → identificadores PokeAPI
        val mapped = mutableMapOf<String, String>()
        for ((edition, value) in wikiEntries) {
            val apiVersion = WikiDexGameMapper.toApiVersionName(edition)
            if (apiVersion != null && !mapped.containsKey(apiVersion)) {
                mapped[apiVersion] = value
            }
        }

        // 5. Cachear en Room
        if (mapped.isNotEmpty()) {
            val entities = mapped.map { (key, value) ->
                WikiDexCacheEntity(
                    pokemonName = spanishName,
                    dataType = dataType,
                    dataKey = key,
                    value = value,
                    fetchedAtMillis = System.currentTimeMillis()
                )
            }
            withContext(Dispatchers.IO) { dao.insertWikiDexCache(entities) }
            Log.d(TAG, "Cacheadas ${entities.size} entradas '$dataType' para '$spanishName'")
        }

        return mapped
    }

    /**
     * Descripciones de Pokedex en español.
     * @return Map<apiVersionName, descripcion>
     */
    suspend fun getFlavorTexts(spanishName: String): Map<String, String> =
        getMappedData(spanishName, DATA_TYPE_FLAVOR, flavorParser)

    /**
     * Localizaciones/encuentros en español.
     * @return Map<apiVersionName, textoLocalizacion>
     */
    suspend fun getLocations(spanishName: String): Map<String, String> =
        getMappedData(spanishName, DATA_TYPE_LOCATION, locationParser)

    /**
     * Sprites HOME de formas desde WikiDex.
     * @return Map<clave_normalizada, url_imagen> donde la clave es el nombre de recurso
     *         en minúscula (ej. "arceus_fuego", "unown_b").
     */
    suspend fun getFormSprites(spanishName: String): Map<String, String> {
        val doc = getDocument(spanishName) ?: return emptyMap()
        return formSpritesParser.parse(doc) ?: emptyMap()
    }
}
