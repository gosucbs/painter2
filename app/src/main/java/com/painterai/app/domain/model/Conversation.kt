package com.painterai.app.domain.model

import java.time.Instant

data class Conversation(
    val id: String = "",
    val jobId: String = "",
    val messages: List<Message> = emptyList(),
    val analysisSummary: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class Message(
    val role: MessageRole,
    val content: String,
    val images: List<String>? = null,
    val timestamp: Instant = Instant.now()
)

enum class MessageRole {
    USER, ASSISTANT
}

data class Photo(
    val id: String = "",
    val jobId: String = "",
    val type: PhotoType = PhotoType.STD,
    val storagePath: String = "",
    val angle: String? = null,
    val createdAt: Instant = Instant.now()
)

enum class PhotoType {
    STD, SAMPLE, VEHICLE, DETAIL
}
