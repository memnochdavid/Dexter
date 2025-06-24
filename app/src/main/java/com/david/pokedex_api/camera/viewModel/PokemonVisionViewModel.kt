package com.david.pokedex_api.camera.viewModel

import android.graphics.Bitmap
import android.net.http.HttpResponseCache
import android.util.Base64 // Para codificar la imagen si el LLM la soporta
import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.semantics.text
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.pokedex_api.camera.dataClass.ChatMessage
import com.david.pokedex_api.camera.dataClass.ContentPart
import com.david.pokedex_api.camera.dataClass.ContentPartImage
import com.david.pokedex_api.camera.dataClass.ContentPartText
import com.david.pokedex_api.camera.dataClass.ImageUrlSpec
import com.david.pokedex_api.camera.dataClass.OpenAIChatRequest
import com.david.pokedex_api.camera.dataClass.OpenAIChatResponse
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.* // O el motor que prefieras (CIO, OkHttp)
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
//import com.david.pokedex_api.camera.dataClass.openAiJson
import com.google.mlkit.vision.label.ImageLabeler
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import com.david.pokedex_api.camera.dataClass.openAiJson
import io.ktor.client.plugins.DefaultRequest

class PokemonVisionViewModel : ViewModel() {

    // --- Estados para ML Kit Image Labeling ---
    private val _imageLabels = MutableStateFlow<List<String>>(emptyList())
    val imageLabels: StateFlow<List<String>> = _imageLabels.asStateFlow()

    private val _isLabelingLoading = MutableStateFlow(false)
    val isLabelingLoading: StateFlow<Boolean> = _isLabelingLoading.asStateFlow()

    private val _labelingError = MutableStateFlow<String?>(null)
    val labelingError: StateFlow<String?> = _labelingError.asStateFlow()

    // --- Estados para la Interacción con LLM ---
    private val _llmPokemonResponse = MutableStateFlow<String?>(null)
    val llmPokemonResponse: StateFlow<String?> = _llmPokemonResponse.asStateFlow()

    private val _isLlmLoading = MutableStateFlow(false)
    val isLlmLoading: StateFlow<Boolean> = _isLlmLoading.asStateFlow()

    private val _llmError = MutableStateFlow<String?>(null)
    val llmError: StateFlow<String?> = _llmError.asStateFlow()

    // Image Labeler de ML Kit
    private val labeler: ImageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)


//    val openAiJson = Json { // This should be a top-level val
//        prettyPrint = true
//        ignoreUnknownKeys = true
//        isLenient = true
//        serializersModule = SerializersModule {
//            polymorphic(ContentPart::class) {
//                subclass(ContentPartText::class)
//                subclass(ContentPartImage::class)
//            }
//            // For Option 1 (explicit @Serializable(with=...) on property):
//            // contextual(ChatMessageContentSerializer) // Still good to have if you use @Contextual elsewhere for Any
//
//            // For Option 2 (@Contextual on property without explicit with=):
//            contextual(Any::class, ChatMessageContentSerializer) // Or contextual(ChatMessageContentSerializer)
//        }
//    }

    // --- Cliente Ktor para llamadas de red ---
    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                json = openAiJson, // La instancia que ahora funciona para la serialización
                contentType = ContentType.Application.Json
            )
        }
        install(Logging) {
            // Tu configuración de Logging, por ejemplo:
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("KtorLogger", message)
                }
            }
            level = LogLevel.ALL // O LogLevel.BODY para ver request/response bodies
        }

        // --- ASEGÚRATE DE QUE ESTA SECCIÓN ESTÉ PRESENTE Y CORRECTA ---
        install(DefaultRequest) {
            // Aquí es donde se usa tu variable openAIApiKey
            header(HttpHeaders.Authorization, "Bearer $openAIApiKey")

            // Opcional: si todas tus llamadas son a la misma URL base de OpenAI
            // esto es útil para no repetir la URL completa en cada llamada post/get
            // url {
            //     protocol = URLProtocol.HTTPS
            //     host = "api.openai.com"
            //     path("/v1/") // Asegúrate de que esto termine en '/' si luego solo usas "chat/completions"
            // }

            // También puedes añadir otros headers por defecto aquí si los necesitas
            // header(HttpHeaders.ContentType, ContentType.Application.Json) // Aunque Ktor suele manejar esto bien con ContentNegotiation
        }
        // -----------------------------------------------------------
    }


    // Deberías almacenar tu API Key de forma segura (NO hardcodearla aquí)
    // Por ejemplo, usando BuildConfig o un archivo de propiedades no versionado.
    private val openAIApiKey = "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" // ¡¡¡REEMPLAZA ESTO!!!

    companion object {
        private const val TAG = "PokemonVisionVM"
        private const val OPENAI_CHAT_URL = "https://api.openai.com/v1/chat/completions"
        // Cambia a true si usas un modelo como gpt-4-vision-preview y envías la imagen
        private const val USE_MULTIMODAL_LLM = true
        // Cambia al modelo que vayas a usar, ej: "gpt-4-vision-preview", "gpt-3.5-turbo"
        private const val LLM_MODEL_NAME = "gpt-4-vision-preview"
    }


    
private fun fetchPokemonInfoFromLLM(labels: List<String>, imageBitmap: Bitmap?) {
    viewModelScope.launch {
        _isLlmLoading.value = true
        _llmError.value = null
        _llmPokemonResponse.value = null

        val promptText: String
        // 'content' para ChatMessage puede ser String o List<ContentPart>.
        // El serializador ChatMessageContentSerializer se encarga de 'Any'.
        // Para la prueba, construimos la estructura como OpenAI la esperaría
        // (una lista de partes para multimodal, o un string para solo texto).
        val contentForChatMessage: Any

        if (USE_MULTIMODAL_LLM && imageBitmap != null) {
            val multimodalParts = mutableListOf<ContentPart>()
            promptText = if (labels.isNotEmpty()) {
                "Identifica el Pokémon en la imagen adjunta. Las siguientes etiquetas podrían ayudar: ${labels.joinToString(", ")}. Describe al Pokémon, su tipo, generación y algún dato curioso."
            } else {
                "Identifica el Pokémon en la imagen adjunta. Describe al Pokémon, su tipo, generación y algún dato curioso."
            }
            multimodalParts.add(ContentPartText(text = promptText))

            try {
                val byteArrayOutputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
                multimodalParts.add(
                    ContentPartImage(
                        imageUrl = ImageUrlSpec(url = "data:image/jpeg;base64,$base64Image")
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al codificar imagen a Base64", e)
                _llmError.value = "Error al procesar la imagen para el LLM."
                _isLlmLoading.value = false
                return@launch
            }
            contentForChatMessage = multimodalParts // Asigna la lista de ContentPart
        } else {
            // Modelo solo Texto
            if (labels.isEmpty() && !USE_MULTIMODAL_LLM) { // Condición más precisa
                Log.w(TAG, "No hay etiquetas y no se usa LLM multimodal. No se puede consultar al LLM.")
                _llmError.value = "No hay información de la imagen para consultar al LLM."
                _isLlmLoading.value = false
                return@launch
            }
            promptText = "Basándome en las siguientes características visuales: ${labels.joinToString(", ")}, ¿qué Pokémon podría ser? Describe al candidato más probable, mencionando su tipo, generación y un dato curioso."
            contentForChatMessage = promptText // Asigna el String
        }

        val requestMessages = listOf(ChatMessage(role = "user", content = contentForChatMessage))

        val requestBody = OpenAIChatRequest(
            model = LLM_MODEL_NAME,
            messages = requestMessages
        )

        // ------------- INICIO DE CAMBIOS PARA LA PRUEBA --------------
        try {
            Log.d(TAG, "Probando serialización con Array Polymorphism para la solicitud...") // PUEDES QUITAR ESTE LOG O CAMBIARLO
            Log.d(TAG, "Modelo: $LLM_MODEL_NAME. Multimodal: $USE_MULTIMODAL_LLM") // ÍDEM

            // Solo serializa a String para ver cómo se ve
            // ESTA LÍNEA ES LA QUE GENERA EL LOG "JSON de prueba con Array Polymorphism"
            // PUEDES QUITARLA. KTOR HARÁ LA SERIALIZACIÓN INTERNAMENTE.
            val jsonStringDePrueba = openAiJson.encodeToString(OpenAIChatRequest.serializer(), requestBody)
            httpClient.post(OPENAI_CHAT_URL) {
                contentType(ContentType.Application.Json)
                setBody(jsonStringDePrueba)
            }
            Log.d(TAG, "JSON de prueba con Array Polymorphism: $jsonStringDePrueba") // ¡QUITA ESTA LÍNEA!

            // Simula una finalización para que el loading se oculte y puedas ver el log
            // _llmPokemonResponse.value = null // Ya no es necesario simular

            // ¡¡¡ESTA ES LA LÍNEA QUE GENERA EL "Error del LLM" QUE VES!!!
            // ¡¡¡QUÍTALA!!!
            // _llmError.value = "PRUEBA DE SERIALIZACIÓN CON ARRAY POLYMORPHISM COMPLETADA. REVISA LOGS PARA VER EL JSON Y SI HUBO EXCEPCIÓN."
            // Log.i(TAG, "Prueba de serialización con Array Polymorphism finalizada. Revisa el Logcat.") // ¡QUITA ESTA LÍNEA!


            // // // ¡¡¡ASEGÚRATE DE QUE LA LLAMADA REAL A LA RED ESTÉ DESCOMENTADA!!! // // //
            val response: OpenAIChatResponse = httpClient.post(OPENAI_CHAT_URL) { // <--- ¡DEBE ESTAR ACTIVA!
                contentType(ContentType.Application.Json)
                setBody(requestBody) // Ktor usará tu 'openAiJson' de ContentNegotiation aquí
            }.body()

            // ... (el resto de tu lógica para manejar la respuesta real de OpenAI)
            // if (response.error != null) { ... } else if (response.choices.isNullOrEmpty() ... )

        } catch (e: Exception) { // Este catch ahora capturará errores REALES de la red o de la API
            Log.e(TAG, "Excepción durante la llamada a OpenAI o procesamiento de respuesta: ", e)
            _llmError.value = "Error al contactar al LLM: ${e.message}" // Mensaje de error real
        } finally {
            _isLlmLoading.value = false
        }
        // ------------- FIN DE CAMBIOS PARA LA PRUEBA --------------
    }
}

    fun analyzeImageForPokemon(imageBitmap: Bitmap) {
        _isLlmLoading.value = true // Indicar carga desde el inicio del proceso
        _llmError.value = null
        _llmPokemonResponse.value = null
        _identifiedPokemonNameOrId.value = null
        _labelingError.value = null

        val image = InputImage.fromBitmap(imageBitmap, 0)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    Log.w(TAG, "ML Kit no encontró etiquetas en la imagen.")
                    // Decide si quieres llamar al LLM multimodal sin etiquetas o mostrar un error/mensaje
                    if (USE_MULTIMODAL_LLM) {
                        Log.i(TAG, "Continuando con LLM multimodal sin etiquetas de ML Kit.")
                        fetchPokemonInfoFromLLM(emptyList(), imageBitmap) // Pasar bitmap para multimodal
                    } else {
                        _labelingError.value = "No se pudo extraer información de la imagen con ML Kit."
                        _isLlmLoading.value = false // Detener carga si no se puede proceder
                    }
                } else {
                    val labelTexts = labels.map { it.text }
                    Log.i(TAG, "Etiquetas de ML Kit: $labelTexts")
                    // Llama a fetchPokemonInfoFromLLM con las etiquetas y el bitmap (si es multimodal)
                    fetchPokemonInfoFromLLM(labelTexts, if (USE_MULTIMODAL_LLM) imageBitmap else null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Fallo el etiquetado de imagen con ML Kit: ", e)
                // Decide si quieres intentar con el LLM multimodal directamente o mostrar error
                if (USE_MULTIMODAL_LLM) {
                    Log.w(TAG, "Fallo ML Kit, intentando con LLM multimodal sin etiquetas.", e)
                    fetchPokemonInfoFromLLM(emptyList(), imageBitmap) // Pasar bitmap para multimodal
                } else {
                    _labelingError.value = "Error de ML Kit: ${e.localizedMessage}"
                    _isLlmLoading.value = false // Detener carga
                }
            }
    }

    fun clearLlmError() {
        _llmError.value = null
        // Opcionalmente, también podrías limpiar el error de etiquetado
        // _labelingError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close() // Cierra el cliente Ktor cuando el ViewModel se destruye
    }

    fun clearLlmResponse() {
        _llmPokemonResponse.value = null
        // Opcionalmente, también podrías limpiar las etiquetas de imagen si ya no son relevantes
        // _imageLabels.value = emptyList()
    }

    fun clearLabelingError() { // Si necesitas limpiarlo independientemente
        _labelingError.value = null
    }
    // --- FIN DE FUNCIONES DE LIMPIEZA ---


    private val _identifiedPokemonNameOrId = MutableStateFlow<String?>(null)
    val identifiedPokemonNameOrId: StateFlow<String?> = _identifiedPokemonNameOrId.asStateFlow()

    // Y una función de parseo (esto es muy dependiente de cómo formatees la respuesta del LLM)
    private fun parsePokemonIdentifierFromLlmResponse(responseText: String): String? {
        // Ejemplo muy básico: busca "Nombre: Pikachu" o "ID: 25"
        // Deberías hacerlo más robusto.
        val nameMatch = Regex("""Nombre:\s*(\w+)""", RegexOption.IGNORE_CASE).find(responseText)
        if (nameMatch != null) return nameMatch.groupValues[1]

        val idMatch = Regex("""ID:\s*(\d+)""", RegexOption.IGNORE_CASE).find(responseText)
        if (idMatch != null) return idMatch.groupValues[1]

        // Si el LLM solo devuelve el nombre, podrías intentar usar eso directamente,
        // pero sé consciente de las variaciones (ej. "Pikachu" vs "Pikáchu").
        // Una opción es simplemente devolver la primera palabra si esperas que sea el nombre.
        // return responseText.split(" ").firstOrNull { it.isNotBlank() } // Demasiado simple, propenso a errores
        return null // O intenta una coincidencia más general si el prompt es más abierto
    }

    // No olvides limpiar este nuevo estado también:
    fun clearIdentifiedPokemon() {
        _identifiedPokemonNameOrId.value = null
    }
}
