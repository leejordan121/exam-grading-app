package com.examgrading.app.ui.admin.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.repositories.AdminRepository
import com.examgrading.app.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassesState(
    val schoolId: String? = null,
    val classes: List<SchoolClass> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ClassesViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ClassesState())
    val state: StateFlow<ClassesState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    adminRepository.getClasses(profile.schoolId)
                        .onSuccess { classes ->
                            _state.update { it.copy(isLoading = false, schoolId = profile.schoolId, classes = classes) }
                        }
                        .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun addClass(grade: String, section: String, academicYear: String) {
        val schoolId = _state.value.schoolId ?: return
        if (grade.isBlank()) return
        val name = "Grade $grade" + if (section.isNotBlank()) " - $section" else ""
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            adminRepository.createClass(schoolId, name, grade, section.trim().ifBlank { null }, academicYear.trim().ifBlank { null })
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to add class") } }
        }
    }
}
