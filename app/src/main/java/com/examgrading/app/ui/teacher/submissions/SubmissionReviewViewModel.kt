package com.examgrading.app.ui.teacher.submissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.SubmissionReviewDetail
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.TeacherSubmissionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionReviewState(
    val detail: SubmissionReviewDetail? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubmissionReviewViewModel @Inject constructor(
    private val teacherSubmissionsRepository: TeacherSubmissionsRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val submissionId: String = checkNotNull(savedStateHandle["submissionId"])

    private val _state = MutableStateFlow(SubmissionReviewState())
    val state: StateFlow<SubmissionReviewState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            teacherSubmissionsRepository.getSubmissionReview(submissionId)
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun saveOverride(extractedAnswerId: String, questionId: String, newScore: Double, reason: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    teacherSubmissionsRepository.overrideQuestionScore(
                        submissionId = submissionId,
                        extractedAnswerId = extractedAnswerId,
                        questionId = questionId,
                        newScore = newScore,
                        reason = reason,
                        reviewerId = profile.id
                    )
                        .onSuccess {
                            _state.update { it.copy(isSaving = false) }
                            refresh()
                        }
                        .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") } }
                }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
