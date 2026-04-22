package com.david.pokedex_api.api.gemini

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PokemonIdentification(
    val name: String,
    val confidence: String
)

class GeminiRepository {

    companion object {
        private const val TAG = "GeminiRepository"

        private val PROMPT = """
            Analyze this image and identify the Pokémon shown.
            Respond ONLY with a JSON object, no markdown, no code fences, no extra text:
            {"name": "<english name as pokeapi slug, lowercase, hyphens instead of spaces>", "confidence": "<high|medium|low>"}

            If there is no Pokémon in the image, respond: {"name": "unknown", "confidence": "low"}

            Rules for the name field (must be a valid PokeAPI slug):
            - All lowercase, use hyphens: "mr-mime", "ho-oh", "porygon-z"
            - Nidoran female: "nidoran-f", Nidoran male: "nidoran-m"
            - Farfetch'd: "farfetchd", Sirfetch'd: "sirfetchd"
            - Regional forms: "rattata-alola", "zigzagoon-galar", "wooper-paldea"
            - Mega evolutions: "charizard-mega-x", "charizard-mega-y", "gengar-mega"
            - Gigantimax: "charizard-gmax"
            - Type: Null → "type-null"
            - Flabébé → "flabebe"
            - Mr. Rime → "mr-rime"
            - Mime Jr. → "mime-jr"
        """.trimIndent()

        // Correcciones comunes por si Gemini no clava el slug exacto
        private val NAME_FIXES = mapOf(
            "mr. mime" to "mr-mime",
            "mr mime" to "mr-mime",
            "mr. rime" to "mr-rime",
            "mr rime" to "mr-rime",
            "mime jr." to "mime-jr",
            "mime jr" to "mime-jr",
            "farfetch'd" to "farfetchd",
            "farfetchd" to "farfetchd",
            "sirfetch'd" to "sirfetchd",
            "ho-oh" to "ho-oh",
            "porygon-z" to "porygon-z",
            "porygon z" to "porygon-z",
            "type: null" to "type-null",
            "type null" to "type-null",
            "tapu koko" to "tapu-koko",
            "tapu lele" to "tapu-lele",
            "tapu bulu" to "tapu-bulu",
            "tapu fini" to "tapu-fini",
            "nidoran♀" to "nidoran-f",
            "nidoran♂" to "nidoran-m",
            "nidoran female" to "nidoran-f",
            "nidoran male" to "nidoran-m",
            "flabébé" to "flabebe",
            "flabebe" to "flabebe",
        )
    }

    suspend fun identifyPokemon(bitmap: Bitmap): PokemonIdentification? = withContext(Dispatchers.IO) {
        val scaled = scaleBitmap(bitmap, 768)
        val total = GeminiClient.comboCount
        if (total == 0) {
            Log.e(TAG, "No hay keys de Gemini configuradas")
            return@withContext null
        }

        var lastError: Exception? = null
        for (offset in 0 until total) {
            val (combo, model) = GeminiClient.modelAt(offset)
            try {
                val response = model.generateContent(
                    content {
                        image(scaled)
                        text(PROMPT)
                    }
                )
                val rawText = response.text?.trim()
                if (rawText == null) {
                    Log.w(TAG, "Respuesta vacía de ${combo.label}")
                    return@withContext null
                }
                Log.d(TAG, "Gemini (${combo.label}) raw response: $rawText")
                GeminiClient.markWorking(offset)
                return@withContext parseResponse(rawText)
            } catch (e: Exception) {
                lastError = e
                if (isQuotaError(e) && offset < total - 1) {
                    Log.w(TAG, "Cuota agotada en ${combo.label}, rotando al siguiente combo")
                    continue
                }
                Log.e(TAG, "Error identifying pokemon en ${combo.label}", e)
                return@withContext null
            }
        }
        Log.e(TAG, "Todos los combos de Gemini agotados", lastError)
        null
    }

    private fun isQuotaError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return "quota" in msg || "exceeded" in msg || "429" in msg || "rate" in msg
    }

    private fun parseResponse(rawText: String): PokemonIdentification? {
        return try {
            // Limpiar posibles code fences
            val cleaned = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleaned)
            val rawName = json.getString("name").lowercase().trim()
            val confidence = json.optString("confidence", "low").lowercase().trim()

            if (rawName == "unknown") return null

            val name = NAME_FIXES[rawName] ?: rawName.replace(" ", "-")

            PokemonIdentification(name = name, confidence = confidence)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: $rawText", e)
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSide: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSide && height <= maxSide) return bitmap

        val ratio = maxSide.toFloat() / maxOf(width, height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
