package com.painterai.app.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.painterai.app.data.repository.JobRepository
import com.painterai.app.domain.model.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val jobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSearch: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var allJobs: List<Job> = emptyList()

    init {
        loadJobs()
    }

    fun loadJobs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            jobRepository.getJobs()
                .onSuccess { jobs ->
                    allJobs = jobs
                    _uiState.update { it.copy(isLoading = false, jobs = jobs) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun toggleSearch() {
        _uiState.update {
            it.copy(showSearch = !it.showSearch, searchQuery = "", jobs = allJobs)
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            jobRepository.deleteJob(jobId)
                .onSuccess {
                    allJobs = allJobs.filter { it.id != jobId }
                    _uiState.update { it.copy(jobs = allJobs) }
                }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                allJobs
            } else {
                allJobs.filter { job ->
                    job.colorCode.contains(query, ignoreCase = true) ||
                    job.vehicleModel.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, jobs = filtered)
        }
    }
}
