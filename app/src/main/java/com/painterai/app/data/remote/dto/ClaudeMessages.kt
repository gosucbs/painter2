package com.painterai.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzeRequest(
    val messages: List<ChatMessage>,
    @SerialName("job_id") val jobId: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: List<ContentBlock>
)

@Serializable
sealed class ContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : ContentBlock()

    @Serializable
    @SerialName("image")
    data class Image(val source: ImageSource) : ContentBlock()
}

@Serializable
data class ImageSource(
    val type: String = "base64",
    @SerialName("media_type") val mediaType: String = "image/jpeg",
    val data: String
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
