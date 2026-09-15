package com.examgrading.app.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.TeacherExamSummary
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.TeacherSubmissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeState(
    val exams: List<TeacherExamSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val teacherSubmissionsRepository: TeacherSubmissionsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeacherHomeState())
    val state: StateFlow<TeacherHomeState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    teacherSubmissionsRepository.getMyExams(profile.id)
                        .onSuccess { exams -> _state.update { it.copy(isLoading = false, exams = exams) } }
                        .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
