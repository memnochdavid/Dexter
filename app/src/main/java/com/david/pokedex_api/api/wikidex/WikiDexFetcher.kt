package com.david.pokedex_api.api.wikidex

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class WikiDexFetcher {

    companion object {
        private const val TAG = "WikiDexFetcher"
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:136.0) Gecko/20100101 Firefox/136.0"
        private const val TIMEOUT_MS = 10_000
    }

    /**
     * Descarga y parsea una pagina de WikiDex.
     * Retorna null si falla la conexion o se detecta Anubis.
     */
    suspend fun fetchDocument(url: String): Document? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .get()

            // Verificar que no es una pagina de Anubis (sin contenido real)
            if (doc.selectFirst("table.pokedex") == null &&
                doc.getElementById("mw-content-text") == null
            ) {
                Log.w(TAG, "Pagina sin contenido wiki (posible Anubis): $url")
                return@withContext null
            }

            doc
        } catch (e: Exception) {
            Log.e(TAG, "Error al descargar $url: ${e.message}")
            null
        }
    }
}
