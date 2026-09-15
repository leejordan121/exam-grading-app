package com.examgrading.app.ui.admin.people

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.domain.models.CreatedUserResult
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.StaffSummary
import com.examgrading.app.domain.repositories.AdminRepository
import com.examgrading.app.domain.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PeopleState(
    val role: String = "teacher",
    val schoolId: String? = null,
    val people: List<StaffSummary> = emptyList(),
    val classes: List<SchoolClass> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val lastCreated: CreatedUserResult? = null
)

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val role: String = checkNotNull(savedStateHandle["role"])

    private val _state = MutableStateFlow(PeopleState(role = role))
    val state: StateFlow<PeopleState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    val people = adminRepository.getStaff(profile.schoolId, role).getOrDefault(emptyList())
                    val classes = if (role == "student") {
                        adminRepository.getClasses(profile.schoolId).getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    _state.update {
                        it.copy(isLoading = false, schoolId = profile.schoolId, people = people, classes = classes)
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun addPerson(email: String, fullName: String, classId: String?) {
        if (email.isBlank() || fullName.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            adminRepository.createUser(email.trim(), fullName.trim(), role, classId)
                .onSuccess { result ->
                    _state.update { it.copy(isSaving = false, lastCreated = result) }
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to create account") } }
        }
    }

    fun dismissCreatedResult() = _state.update { it.copy(lastCreated = null) }
}
