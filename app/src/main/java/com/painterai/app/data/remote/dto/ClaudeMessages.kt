package com.painterai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class AnalyzeRequest(
    val messages: List<ChatMessage>,
    @SerialName("job_id") val jobId: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonArray
)

@Serializable
data class AnalyzeResponse(
    val content: List<ResponseContent> = emptyList(),
    val error: String? = null
)

@Serializable
data class ResponseContent(
    val type: String = "text",
    val text: String = ""
)

// Helper functions to build content blocks
object ContentBlocks {
    fun text(text: String): JsonObject = buildJsonObject {
        put("type", "text")
        put("text", text)
    }

    fun image(base64Data: String, mediaType: String = "image/jpeg"): JsonObject = buildJsonObject {
        put("type", "image")
        putJsonObject("source") {
            put("type", "base64")
            put("media_type", mediaType)
            put("data", base64Data)
        }
    }
}
