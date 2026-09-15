package com.examgrading.app.ui.student.examdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.ExamDetail
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.StudentExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamDetailState(
    val exam: ExamDetail? = null,
    val downloadUrl: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExamDetailViewModel @Inject constructor(
    private val studentExamRepository: StudentExamRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val examId: String = checkNotNull(savedStateHandle["examId"])

    private val _state = MutableStateFlow(ExamDetailState())
    val state: StateFlow<ExamDetailState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            studentExamRepository.getExamDetail(examId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, exam = detail) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun downloadPaper() {
        val path = _state.value.exam?.paperFilePath ?: return
        viewModelScope.launch {
            studentExamRepository.getPaperDownloadUrl(path)
                .onSuccess { url -> _state.update { it.copy(downloadUrl = url) } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun consumeDownloadUrl() = _state.update { it.copy(downloadUrl = null) }
}
