package com.examgrading.app.ui.student.submission

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.core.utils.createCaptureUri
import com.examgrading.app.core.utils.readBytesFromUri
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.StudentExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubmissionState(
    val schoolId: String? = null,
    val submissionId: String? = null,
    val pageNumbers: List<Int> = emptyList(),
    val pageThumbnails: Map<Int, String> = emptyMap(),
    val pendingCaptureUri: Uri? = null,
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false
)

@HiltViewModel
class SubmissionViewModel @Inject constructor(
    private val studentExamRepository: StudentExamRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val examId: String = checkNotNull(savedStateHandle["examId"])

    private val _state = MutableStateFlow(SubmissionState())
    val state: StateFlow<SubmissionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    studentExamRepository.getOrCreateDraftSubmission(examId, profile.id)
                        .onSuccess { submissionId ->
                            val count = studentExamRepository.getSubmissionPageCount(submissionId).getOrDefault(0)
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    schoolId = profile.schoolId,
                                    submissionId = submissionId,
                                    pageNumbers = (1..count).toList()
                                )
                            }
                        }
                        .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    /** Prepares a destination Uri for the camera app and returns it for the caller to launch. */
    fun prepareCapture(context: Context): Uri {
        val nextPage = _state.value.pageNumbers.size + 1
        val uri = createCaptureUri(context, "page_$nextPage.jpg")
        _state.update { it.copy(pendingCaptureUri = uri) }
        return uri
    }

    fun onCaptureResult(context: Context, success: Boolean) {
        val uri = _state.value.pendingCaptureUri ?: return
        _state.update { it.copy(pendingCaptureUri = null) }
        if (!success) return

        val schoolId = _state.value.schoolId ?: return
        val submissionId = _state.value.submissionId ?: return
        val pageNumber = _state.value.pageNumbers.size + 1
        val bytes = readBytesFromUri(context, uri) ?: run {
            _state.update { it.copy(error = "Couldn't read the captured photo.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, error = null) }
            studentExamRepository.uploadSubmissionPage(schoolId, submissionId, pageNumber, bytes)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isUploading = false,
                            pageNumbers = it.pageNumbers + pageNumber,
                            pageThumbnails = it.pageThumbnails + (pageNumber to uri.toString())
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isUploading = false, error = e.message ?: "Upload failed") } }
        }
    }

    fun deletePage(pageNumber: Int) {
        val schoolId = _state.value.schoolId ?: return
        val submissionId = _state.value.submissionId ?: return
        viewModelScope.launch {
            studentExamRepository.deleteSubmissionPage(schoolId, submissionId, pageNumber)
                .onSuccess {
                    _state.update {
                        it.copy(
                            pageNumbers = it.pageNumbers.filterNot { n -> n == pageNumber },
                            pageThumbnails = it.pageThumbnails - pageNumber
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    fun submit() {
        val submissionId = _state.value.submissionId ?: return
        if (_state.value.pageNumbers.isEmpty()) {
            _state.update { it.copy(error = "Scan at least one page before submitting.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            studentExamRepository.submitExam(submissionId)
                .onSuccess { _state.update { it.copy(isLoading = false, submitted = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to submit") } }
        }
    }
}
