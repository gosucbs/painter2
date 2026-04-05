package com.painterai.app.data.repository

import com.painterai.app.data.remote.dto.ConversationDto
import com.painterai.app.domain.model.Conversation
import com.painterai.app.domain.model.Message
import com.painterai.app.domain.model.MessageRole
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class MessageJson(
    val role: String,
    val content: String,
    val images: List<String>? = null,
    val timestamp: String
)

@Singleton
class ConversationRepository @Inject constructor(
    private val postgrest: Postgrest
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getConversation(jobId: String): Result<Conversation?> {
        return try {
            val results = postgrest.from("conversations")
                .select {
                    filter { eq("job_id", jobId) }
                }
                .decodeList<ConversationDto>()

            val dto = results.firstOrNull()
            Result.success(dto?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createConversation(jobId: String): Result<Conversation> {
        return try {
            val dto = ConversationDto(jobId = jobId)
            val result = postgrest.from("conversations")
                .insert(dto) { select() }
                .decodeSingle<ConversationDto>()
            Result.success(result.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMessage(conversationId: String, messages: List<Message>): Result<Unit> {
        return try {
            val messageJsonList = messages.map {
                MessageJson(
                    role = it.role.name.lowercase(),
                    content = it.content,
                    images = it.images,
                    timestamp = it.timestamp.toString()
                )
            }
            val messagesStr = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(MessageJson.serializer()),
                messageJsonList
            )
            postgrest.from("conversations")
                .update({
                    set("messages", messagesStr)
                    set("updated_at", Instant.now().toString())
                }) {
                    filter { eq("id", conversationId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSummary(conversationId: String, summary: String): Result<Unit> {
        return try {
            postgrest.from("conversations")
                .update({
                    set("analysis_summary", summary)
                }) {
                    filter { eq("id", conversationId) }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ConversationDto.toDomain(): Conversation {
        val messageList = try {
            json.decodeFromString<List<MessageJson>>(messages).map {
                Message(
                    role = if (it.role == "assistant") MessageRole.ASSISTANT else MessageRole.USER,
                    content = it.content,
                    images = it.images,
                    timestamp = try { Instant.parse(it.timestamp) } catch (_: Exception) { Instant.now() }
                )
            }
        } catch (_: Exception) {
            emptyList()
        }

        return Conversation(
            id = id ?: "",
            jobId = jobId,
            messages = messageList,
            analysisSummary = analysisSummary,
            createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.now(),
            updatedAt = updatedAt?.let { Instant.parse(it) } ?: Instant.now()
        )
    }
}
