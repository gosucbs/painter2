package com.painterai.app.ui.screen.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.remote.ClaudeApiService
import com.painterai.app.data.remote.dto.*
import com.painterai.app.data.repository.ConversationRepository
import com.painterai.app.data.repository.JobRepository
import com.painterai.app.domain.model.*
import kotlinx.serialization.json.buildJsonArray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val job: Job? = null,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val conversationId: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val conversationRepository: ConversationRepository,
    private val claudeApiService: ClaudeApiService
) : ViewModel() {

    private val jobId: String = savedStateHandle["jobId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            jobRepository.getJob(jobId).onSuccess { job ->
                _uiState.update { it.copy(job = job) }
            }

            conversationRepository.getConversation(jobId).onSuccess { conv ->
                if (conv != null) {
                    _uiState.update {
                        it.copy(
                            messages = conv.messages,
                            conversationId = conv.id,
                            isLoading = false
                        )
                    }
                } else {
                    conversationRepository.createConversation(jobId).onSuccess { newConv ->
                        _uiState.update {
                            it.copy(conversationId = newConv.id, isLoading = false)
                        }
                    }
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = "대화 로드 실패") }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank() || state.isSending) return

        val userMessage = Message(MessageRole.USER, text)
        val updatedMessages = state.messages + userMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputText = "",
                isSending = true,
                error = null
            )
        }

        viewModelScope.launch {
            // Build API messages from conversation history
            val apiMessages = updatedMessages.map { msg ->
                ChatMessage(
                    role = if (msg.role == MessageRole.USER) "user" else "assistant",
                    content = buildJsonArray { add(ContentBlocks.text(msg.content)) }
                )
            }

            val request = AnalyzeRequest(messages = apiMessages, jobId = jobId)

            claudeApiService.analyzeColor(request)
                .onSuccess { response ->
                    val responseText = response.content.joinToString("") { it.text }
                    val assistantMessage = Message(MessageRole.ASSISTANT, responseText)
                    val allMessages = updatedMessages + assistantMessage

                    _uiState.update {
                        it.copy(messages = allMessages, isSending = false)
                    }

                    // Save to Supabase
                    val convId = state.conversationId ?: return@onSuccess
                    conversationRepository.addMessage(convId, allMessages)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isSending = false, error = "전송 실패: ${e.message}")
                    }
                }
        }
    }
}
