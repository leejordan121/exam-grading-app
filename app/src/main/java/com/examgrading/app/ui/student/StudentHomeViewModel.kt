package com.examgrading.app.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.ExamSummary
import com.examgrading.app.domain.models.StudentSubmission
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.StudentExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudentHomeState(
    val exams: List<ExamSummary> = emptyList(),
    val results: List<StudentSubmission> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val studentExamRepository: StudentExamRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StudentHomeState())
    val state: StateFlow<StudentHomeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    val exams = studentExamRepository.getAssignedExams(profile.id).getOrDefault(emptyList())
                    val results = studentExamRepository.getMySubmissions(profile.id).getOrDefault(emptyList())
                    _state.update { it.copy(isLoading = false, exams = exams, results = results) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
