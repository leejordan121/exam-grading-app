package com.examgrading.app.ui.teacher.submissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.SubmissionListItem
import com.examgrading.app.domain.repositories.TeacherSubmissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExamSubmissionsState(
    val submissions: List<SubmissionListItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExamSubmissionsViewModel @Inject constructor(
    private val teacherSubmissionsRepository: TeacherSubmissionsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val examId: String = checkNotNull(savedStateHandle["examId"])

    private val _state = MutableStateFlow(ExamSubmissionsState())
    val state: StateFlow<ExamSubmissionsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            teacherSubmissionsRepository.getExamSubmissions(examId)
                .onSuccess { list -> _state.update { it.copy(isLoading = false, submissions = list) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
