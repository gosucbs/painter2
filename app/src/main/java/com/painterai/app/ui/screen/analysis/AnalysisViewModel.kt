package com.painterai.app.ui.screen.analysis

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.remote.ClaudeApiService
import com.painterai.app.data.remote.dto.*
import android.content.Context
import com.painterai.app.data.repository.ConversationRepository
import com.painterai.app.data.repository.JobRepository
import com.painterai.app.data.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.buildJsonArray
import com.painterai.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalysisUiState(
    val job: Job? = null,
    val vehiclePhotoUri: Uri? = null,
    val sampleSpecimenUri: Uri? = null,
    val sampleRecipeUri: Uri? = null,
    val analysisResult: String? = null,
    val isAnalyzing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversationId: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val jobRepository: JobRepository,
    private val claudeApiService: ClaudeApiService,
    private val conversationRepository: ConversationRepository,
    private val photoRepository: PhotoRepository
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

    fun onVehiclePhotoSelected(uri: Uri) {
        _uiState.update { it.copy(vehiclePhotoUri = uri) }
    }

    fun onSampleSpecimenSelected(uri: Uri) {
        _uiState.update { it.copy(sampleSpecimenUri = uri) }
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

            // 사진 Supabase Storage에 업로드
            val photoTypes = listOf("vehicle" to state.vehiclePhotoUri, "sample" to state.sampleSpecimenUri, "recipe" to state.sampleRecipeUri)
            for ((type, uri) in photoTypes) {
                if (uri != null) {
                    photoRepository.uploadPhoto(context, jobId, "anonymous", uri, type)
                }
            }

            val prompt = buildString {
                appendLine("## 작업 정보")
                appendLine("차량: ${job.vehicleModel} ${job.vehicleYear ?: ""}년식")
                appendLine("컬러코드: ${job.colorCode}")
                appendLine("도료사: ${job.paintBrand.displayName}")
                appendLine()
                appendLine("## 첨부 사진 (순서대로)")
                appendLine("1. 차량사진 (목표 색상)")
                appendLine("2. 조색시편사진 (현재 조색 결과)")
                appendLine("3. 조색시편배합 (현재 배합표)")
                appendLine()
                appendLine("조색 시편이 차량 색상에 더 가까워지려면 어떻게 해야 하는지 분석해주세요.")
                appendLine("45°/105°/15° 각도별 차이, Delta 값을 추정하고,")
                appendLine("KCC SUMIX 토너 코드 기준으로 어떤 토너를 증량/감량해야 하는지 구체적으로 알려주세요.")
            }

            val contentArray = buildJsonArray {
                imageBase64List.forEach { base64 ->
                    add(ContentBlocks.image(base64))
                }
                add(ContentBlocks.text(prompt))
            }

            val request = AnalyzeRequest(
                messages = listOf(ChatMessage(role = "user", content = contentArray)),
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
                .onSuccess {
                    _uiState.update { it.copy(isSaved = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = "저장 실패: ${e.message}") }
                }
        }
    }
}
