package com.david.pokedex_api.api.gemini

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.david.pokedex_api.BuildConfig
import com.david.pokedex_api.DexterApplication
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import java.time.LocalDate
import java.time.ZoneId

object GeminiClient {

    private const val TAG = "GeminiClient"
    private const val PREFS_NAME = "gemini_client"
    private const val KEY_START_INDEX = "start_index"
    private const val KEY_LAST_DAY = "last_day_pacific"

    // El free tier de Gemini resetea a medianoche hora Pacífico.
    private val pacificZone: ZoneId = ZoneId.of("America/Los_Angeles")

    data class Combo(val key: String, val modelName: String) {
        val label: String get() = "${modelName}@${key.takeLast(4)}"
    }

    private val combos: List<Combo> = buildList {
        val keys = listOfNotNull(
            BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() },
            BuildConfig.GEMINI_API_KEY_2.takeIf { it.isNotBlank() },
        )
        val models = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash")
        for (k in keys) for (m in models) add(Combo(k, m))
    }

    private val prefs: SharedPreferences? by lazy {
        runCatching {
            DexterApplication.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }.getOrNull()
    }

    @Volatile
    private var startIndex: Int = loadStartIndex()

    private val modelCache = HashMap<Int, GenerativeModel>()

    val comboCount: Int get() = combos.size

    @Synchronized
    fun modelAt(offset: Int): Pair<Combo, GenerativeModel> {
        require(combos.isNotEmpty()) { "No hay keys de Gemini configuradas" }
        val idx = normalize(startIndex + offset)
        val combo = combos[idx]
        val model = modelCache.getOrPut(idx) { buildModel(combo) }
        return combo to model
    }

    @Synchronized
    fun markWorking(offset: Int) {
        if (combos.isEmpty()) return
        val newStart = normalize(startIndex + offset)
        if (newStart == startIndex) {
            persist(newStart) // refresca el día aunque el índice no cambie
            return
        }
        Log.d(TAG, "Rotando startIndex: $startIndex -> $newStart (${combos[newStart].label})")
        startIndex = newStart
        persist(newStart)
    }

    private fun loadStartIndex(): Int {
        if (combos.isEmpty()) return 0
        val p = prefs ?: return 0
        val today = LocalDate.now(pacificZone).toString()
        val savedDay = p.getString(KEY_LAST_DAY, null)
        return if (savedDay == today) {
            p.getInt(KEY_START_INDEX, 0).let { normalize(it) }
        } else {
            // Día nuevo: reset. No escribimos aún: lo hará markWorking cuando algo funcione.
            0
        }
    }

    private fun persist(index: Int) {
        val p = prefs ?: return
        val today = LocalDate.now(pacificZone).toString()
        p.edit()
            .putInt(KEY_START_INDEX, index)
            .putString(KEY_LAST_DAY, today)
            .apply()
    }

    private fun normalize(i: Int): Int {
        val n = combos.size
        return ((i % n) + n) % n
    }

    private fun buildModel(combo: Combo): GenerativeModel = GenerativeModel(
        modelName = combo.modelName,
        apiKey = combo.key,
        generationConfig = generationConfig {
            temperature = 0.1f
            maxOutputTokens = 150
        },
    )
}
