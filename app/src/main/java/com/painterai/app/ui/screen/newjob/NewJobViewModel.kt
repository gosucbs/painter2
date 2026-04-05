package com.painterai.app.ui.screen.newjob

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.remote.dto.JobDto
import com.painterai.app.data.repository.AuthRepository
import com.painterai.app.data.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewJobUiState(
    val vehicleModel: String = "",
    val vehicleYear: String = "",
    val colorCode: String = "",
    val paintBrand: String = "KCC 수믹스",
    val workArea: String = "",
    val notes: String = "",
    val brandExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdJobId: String? = null
)

@HiltViewModel
class NewJobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewJobUiState())
    val uiState: StateFlow<NewJobUiState> = _uiState.asStateFlow()

    fun onVehicleModelChanged(value: String) { _uiState.update { it.copy(vehicleModel = value, error = null) } }
    fun onVehicleYearChanged(value: String) { _uiState.update { it.copy(vehicleYear = value) } }
    fun onColorCodeChanged(value: String) { _uiState.update { it.copy(colorCode = value.uppercase(), error = null) } }
    fun onPaintBrandChanged(value: String) { _uiState.update { it.copy(paintBrand = value) } }
    fun onWorkAreaChanged(value: String) { _uiState.update { it.copy(workArea = value) } }
    fun onNotesChanged(value: String) { _uiState.update { it.copy(notes = value) } }
    fun onBrandExpandedChanged(expanded: Boolean) { _uiState.update { it.copy(brandExpanded = expanded) } }

    fun fillMockData() {
        _uiState.update {
            it.copy(
                vehicleModel = "아이오닉6",
                vehicleYear = "2023",
                colorCode = "T2G",
                paintBrand = "KCC 수믹스",
                workArea = "본넷, 휀다",
                notes = "뒤도어 보수도장"
            )
        }
    }

    fun createJob() {
        val state = _uiState.value
        if (state.vehicleModel.isBlank()) {
            _uiState.update { it.copy(error = "차량 모델을 입력해주세요") }
            return
        }
        if (state.colorCode.isBlank()) {
            _uiState.update { it.copy(error = "컬러코드를 입력해주세요") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userId = authRepository.currentUserId()

            val jobDto = JobDto(
                userId = userId,
                vehicleModel = state.vehicleModel,
                vehicleYear = state.vehicleYear.toIntOrNull(),
                colorCode = state.colorCode,
                paintBrand = if (state.paintBrand == "노루 워터큐") "NOROO_WATERQ" else "KCC_SUMIX",
                workArea = state.workArea.ifBlank { null },
                notes = state.notes.ifBlank { null }
            )

            jobRepository.createJob(jobDto)
                .onSuccess { job ->
                    _uiState.update { it.copy(isLoading = false, createdJobId = job.id) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "작업 생성 실패: ${e.message}") }
                }
        }
    }
}
