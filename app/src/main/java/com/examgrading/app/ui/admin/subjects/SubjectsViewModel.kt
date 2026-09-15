package com.examgrading.app.ui.admin.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.Subject
import com.examgrading.app.domain.repositories.AdminRepository
import com.examgrading.app.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubjectsState(
    val schoolId: String? = null,
    val subjects: List<Subject> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubjectsViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubjectsState())
    val state: StateFlow<SubjectsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    adminRepository.getSubjects(profile.schoolId)
                        .onSuccess { subjects ->
                            _state.update { it.copy(isLoading = false, schoolId = profile.schoolId, subjects = subjects) }
                        }
                        .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun addSubject(name: String, code: String) {
        val schoolId = _state.value.schoolId ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            adminRepository.createSubject(schoolId, name.trim(), code.trim().ifBlank { null })
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to add subject") } }
        }
    }
}
