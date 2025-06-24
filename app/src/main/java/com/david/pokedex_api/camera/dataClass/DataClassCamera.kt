package com.david.pokedex_api.camera.dataClass

// Unused imports can be removed:
// import kotlinx.serialization.Contextual // Not directly used if @Serializable(with=...) is on property
// import kotlinx.serialization.Polymorphic // Used within SerializersModule block
// import kotlinx.serialization.json.jsonArray // Used by JsonDecoder extension
// import kotlinx.serialization.json.jsonObject // Used by JsonDecoder extension
// import kotlinx.serialization.json.jsonPrimitive // Used by JsonDecoder extension
// import kotlinx.serialization.modules.contextual // Not directly used if @Serializable(with=...) is on property

import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

// --- Data classes for OpenAI API ---

@Serializable
data class OpenAIChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int = 300, // Slightly increased for potentially descriptive responses
    val temperature: Float = 0.7f
)

// Base sealed interface for different parts of a message content
@Serializable
sealed interface ContentPart // Using interface, common practice

// Represents a text part of the message content
@Serializable
@SerialName("text") // This is the VALUE for the classDiscriminator for this type
data class ContentPartText(
    val type: String = "text", // DATA property required by OpenAI
    val text: String
) : ContentPart

// Represents an image part of the message content
@Serializable
@SerialName("image_url") // This is the VALUE for the classDiscriminator for this type
data class ContentPartImage(
    val type: String = "image_url", // DATA property required by OpenAI
    @SerialName("image_url") // JSON key for the ImageUrlSpec object
    val imageUrl: ImageUrlSpec
) : ContentPart

@Serializable
data class ImageUrlSpec(
    val url: String, // e.g., "data:image/jpeg;base64,{base64_image_string}"
    val detail: String = "low" // Or "high", "auto"
)

// Represents a single message in the chat
@Serializable
data class ChatMessage(
    val role: String, // e.g., "user", "assistant"
    // Content can be a simple String (for text-only models/messages)
    // or a List<ContentPart> (for multimodal messages)
    @Serializable(with = ChatMessageContentSerializer::class)
    val content: Any
)

// --- Custom Serializer for ChatMessage.content ---
object ChatMessageContentSerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.david.pokedex_api.camera.dataClass.AnyChatMessageContent")

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("This serializer can only be used with JSON output for serialization.")

        val currentJsonConfiguration = jsonEncoder.json.configuration
        Log.d("SerializerDebug", "ChatMessageContentSerializer: Encoder's Json classDiscriminator IS: '${currentJsonConfiguration.classDiscriminator}'")

        val currentSerializersModule = jsonEncoder.json.serializersModule

        when (value) {
            is String -> jsonEncoder.encodeString(value)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                try {
                    val contentPartList = value.map { it as? ContentPart ?: throw SerializationException("...") }
                    val contentPartElementSerializer = currentSerializersModule.serializer<ContentPart>() // Debe heredar la config. del módulo
                    val listContentPartSerializer = ListSerializer(contentPartElementSerializer)
                    jsonEncoder.encodeSerializableValue(listContentPartSerializer, contentPartList)
                } catch (e: Exception) {
                    // Envuelve la excepción para añadir contexto si no es ya SerializationException
                    if (e is SerializationException) throw e
                    throw SerializationException("Failed to obtain serializer or serialize List<ContentPart> in ChatMessageContentSerializer: ${e.message}", e)
                }
            }
            else -> throw SerializationException("Unsupported type for ChatMessage.content: ${value::class.simpleName}")
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("This serializer can only be used with JSON input for deserialization.")
        val jsonElement = jsonDecoder.decodeJsonElement()

        val contentPartElementSerializerForDeserialization = openAiJson.serializersModule.serializer<ContentPart>()
        val listContentPartSerializerForDeserialization = ListSerializer(contentPartElementSerializerForDeserialization)
        return when (jsonElement) {
            is JsonPrimitive -> if (jsonElement.isString) jsonElement.content else throw SerializationException("...")
            is JsonArray -> openAiJson.decodeFromJsonElement(listContentPartSerializerForDeserialization, jsonElement)
            else -> throw SerializationException("...")
        }
    }
}

// --- OpenAI API Response Data Classes ---
@Serializable
data class OpenAIChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: OpenAIError? = null // For API-level errors
)

@Serializable
data class Choice(
    val index: Int? = null,
    val message: ChatMessage? = null, // Assistant's message
    @SerialName("finish_reason")
    val finishReason: String? = null // e.g., "stop", "length"
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)

// Represents an error object from the OpenAI API
@Serializable
data class OpenAIError(
    val message: String,
    val type: String,
    val param: String?, // Nullable
    val code: String?  // Nullable
)
/*
// --- SINGLE, CORRECTLY CONFIGURED JSON INSTANCE ---
val openAiJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true // Essential for API changes
    isLenient = true         // Helpful for minor JSON variations

    // *** THE KEY FIX for "property name that conflicts with JSON class discriminator 'type'" ***
    // We change the JSON KEY used by kotlinx.serialization for its class discriminator.
    // OpenAI uses "type" as a data property, so we must avoid that for the library's discriminator key.
    classDiscriminator = "_custom_discriminator_key_" // Or any name NOT "type", e.g., "#type", "$type"

    serializersModule = SerializersModule {
        polymorphic(ContentPart::class) {
            // The @SerialName on the subclass (e.g., "text") becomes the VALUE
            // for the classDiscriminator key (now "_custom_discriminator_key_").
            subclass(ContentPartText::class) // Serializer is derived: ContentPartText.serializer()
            subclass(ContentPartImage::class) // Serializer is derived: ContentPartImage.serializer()
        }
        // No need to register ChatMessageContentSerializer with contextual(Any::class, ...)
        // because we are using @Serializable(with = ChatMessageContentSerializer::class)
        // directly on the ChatMessage.content property.
        // If you had other properties of type 'Any' that needed this specific serializer
        // and used @Contextual, then you would add:
        // contextual(ChatMessageContentSerializer) // or contextual(Any::class, ChatMessageContentSerializer)
    }
}
*/
val openAiJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "_custom_discriminator_key_" // O tu nombre

    serializersModule = SerializersModule {
        polymorphic(ContentPart::class) {
            subclass(ContentPartText::class)
            subclass(ContentPartImage::class)
        }
    }
}
