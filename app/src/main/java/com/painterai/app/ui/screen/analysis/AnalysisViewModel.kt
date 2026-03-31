package com.painterai.app.ui.screen.analysis

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.remote.ClaudeApiService
import com.painterai.app.data.remote.dto.*
import com.painterai.app.data.repository.ConversationRepository
import com.painterai.app.data.repository.JobRepository
import com.painterai.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val job: Job? = null,
    val stdSpecimenUri: Uri? = null,
    val sampleSpecimenUri: Uri? = null,
    val stdRecipeUri: Uri? = null,
    val sampleRecipeUri: Uri? = null,
    val analysisResult: String? = null,
    val isAnalyzing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val claudeApiService: ClaudeApiService,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val jobId: String = savedStateHandle["jobId"] ?: ""

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadJob()
    }

    private fun loadJob() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            jobRepository.getJob(jobId)
                .onSuccess { job ->
                    _uiState.update { it.copy(job = job, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }

            conversationRepository.getConversation(jobId)
                .onSuccess { conv ->
                    if (conv != null) {
                        _uiState.update { it.copy(conversationId = conv.id) }
                        val lastAssistant = conv.messages.lastOrNull { it.role == MessageRole.ASSISTANT }
                        if (lastAssistant != null) {
                            _uiState.update { it.copy(analysisResult = lastAssistant.content) }
                        }
                    } else {
                        conversationRepository.createConversation(jobId)
                            .onSuccess { newConv ->
                                _uiState.update { it.copy(conversationId = newConv.id) }
                            }
                    }
                }
        }
    }

    fun onStdSpecimenSelected(uri: Uri) {
        _uiState.update { it.copy(stdSpecimenUri = uri) }
    }

    fun onSampleSpecimenSelected(uri: Uri) {
        _uiState.update { it.copy(sampleSpecimenUri = uri) }
    }

    fun onStdRecipePhotoSelected(uri: Uri) {
        _uiState.update { it.copy(stdRecipeUri = uri) }
    }

    fun onSampleRecipePhotoSelected(uri: Uri) {
        _uiState.update { it.copy(sampleRecipeUri = uri) }
    }

    fun analyzeWithPhotos(imageBase64List: List<String>) {
        val state = _uiState.value
        val job = state.job ?: return

        if (imageBase64List.isEmpty()) {
            _uiState.update { it.copy(error = "사진을 1장 이상 촬영해주세요") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, analysisResult = null) }

            val contentBlocks = mutableListOf<ContentBlock>()

            // 이미지 추가
            imageBase64List.forEach { base64 ->
                contentBlocks.add(ContentBlock.Image(ImageSource(data = base64)))
            }

            // 텍스트 프롬프트
            val labels = mutableListOf<String>()
            if (state.stdSpecimenUri != null) labels.add("STD 시편")
            if (state.sampleSpecimenUri != null) labels.add("조색 시편")
            if (state.stdRecipeUri != null) labels.add("STD 배합표")
            if (state.sampleRecipeUri != null) labels.add("조색 배합표")

            val prompt = buildString {
                appendLine("## 작업 정보")
                appendLine("차량: ${job.vehicleModel} ${job.vehicleYear ?: ""}년식")
                appendLine("컬러코드: ${job.colorCode}")
                appendLine("도료사: ${job.paintBrand.displayName}")
                appendLine()
                appendLine("## 첨부 사진 (순서대로)")
                labels.forEachIndexed { i, label -> appendLine("${i + 1}. $label") }
                appendLine()
                appendLine("위 사진들을 보고 STD와 조색 시편을 3각도(15°/45°/105°) 기준으로 비교 분석해주세요.")
                appendLine("배합표 사진에서 토너 코드와 g수를 읽어서 어떤 토너가 부족하거나 과다한지 알려주세요.")
            }

            contentBlocks.add(ContentBlock.Text(prompt))

            val request = AnalyzeRequest(
                messages = listOf(ChatMessage(role = "user", content = contentBlocks)),
                jobId = jobId
            )

            claudeApiService.analyzeColor(request)
                .onSuccess { response ->
                    val resultText = response.content.joinToString("") { it.text }
                    _uiState.update { it.copy(isAnalyzing = false, analysisResult = resultText) }

                    // 대화 저장
                    val convId = state.conversationId ?: return@onSuccess
                    val messages = listOf(
                        Message(MessageRole.USER, prompt),
                        Message(MessageRole.ASSISTANT, resultText)
                    )
                    conversationRepository.addMessage(convId, messages)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isAnalyzing = false, error = "AI 분석 실패: ${e.message}") }
                }
        }
    }

    fun saveJobResult(result: JobResult) {
        viewModelScope.launch {
            jobRepository.updateJobResult(jobId, result.name.lowercase())
        }
    }
}
