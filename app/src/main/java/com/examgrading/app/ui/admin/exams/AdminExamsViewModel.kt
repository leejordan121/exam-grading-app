package com.examgrading.app.ui.admin.exams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.AdminExamSummary
import com.examgrading.app.domain.repositories.AdminRepository
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminExamsState(
    val exams: List<AdminExamSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val examPendingDelete: AdminExamSummary? = null,
    val isDeleting: Boolean = false
)

@HiltViewModel
class AdminExamsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val examRepository: ExamRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminExamsState())
    val state: StateFlow<AdminExamsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    adminRepository.getAllExams(profile.schoolId)
                        .onSuccess { exams -> _state.update { it.copy(isLoading = false, exams = exams) } }
                        .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun requestDelete(exam: AdminExamSummary) = _state.update { it.copy(examPendingDelete = exam) }

    fun cancelDelete() = _state.update { it.copy(examPendingDelete = null) }

    fun confirmDelete() {
        val exam = _state.value.examPendingDelete ?: return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true) }
            examRepository.deleteExam(exam.examId)
                .onSuccess {
                    _state.update { it.copy(isDeleting = false, examPendingDelete = null) }
                    refresh()
                }
                .onFailure { e ->
                    _state.update { it.copy(isDeleting = false, examPendingDelete = null, error = e.message ?: "Failed to delete exam") }
                }
        }
    }
}
