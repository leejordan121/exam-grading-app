package com.examgrading.app.ui.student.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.SubmissionResultDetail
import com.examgrading.app.domain.repositories.StudentExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultDetailState(
    val detail: SubmissionResultDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ResultDetailViewModel @Inject constructor(
    private val studentExamRepository: StudentExamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val submissionId: String = checkNotNull(savedStateHandle["submissionId"])

    private val _state = MutableStateFlow(ResultDetailState())
    val state: StateFlow<ResultDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            studentExamRepository.getSubmissionResultDetail(submissionId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
