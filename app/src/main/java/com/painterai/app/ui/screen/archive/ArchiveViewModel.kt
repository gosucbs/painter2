package com.painterai.app.ui.screen.archive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.repository.ConversationRepository
import com.painterai.app.data.repository.JobRepository
import com.painterai.app.domain.model.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val job: Job? = null,
    val analysisSummary: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val jobId: String = savedStateHandle["jobId"] ?: ""

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            jobRepository.getJob(jobId).onSuccess { job ->
                _uiState.update { it.copy(job = job) }
            }

            conversationRepository.getConversation(jobId).onSuccess { conv ->
                _uiState.update {
                    it.copy(analysisSummary = conv?.analysisSummary, isLoading = false)
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
