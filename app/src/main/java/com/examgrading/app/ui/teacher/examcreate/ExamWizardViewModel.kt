package com.examgrading.app.ui.teacher.examcreate

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examgrading.app.core.utils.readPickedFile
import com.examgrading.app.domain.models.GradingMode
import com.examgrading.app.domain.models.QuestionDraft
import com.examgrading.app.domain.models.QuestionType
import com.examgrading.app.domain.repositories.AuthRepository
import com.examgrading.app.domain.repositories.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExamWizardViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExamWizardState())
    val state: StateFlow<ExamWizardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getCurrentProfile()
                .onSuccess { profile ->
                    _state.update { it.copy(schoolId = profile.schoolId, teacherId = profile.id) }
                    loadSubjectsAndClasses(profile.schoolId)
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private suspend fun loadSubjectsAndClasses(schoolId: String) {
        examRepository.getSubjects(schoolId).onSuccess { subjects ->
            _state.update { it.copy(subjects = subjects) }
        }
        examRepository.getClasses(schoolId).onSuccess { classes ->
            _state.update { it.copy(classes = classes) }
        }
    }

    fun updateTitle(value: String) = _state.update { it.copy(title = value) }
    fun updateDescription(value: String) = _state.update { it.copy(description = value) }
    fun updateInstructions(value: String) = _state.update { it.copy(instructions = value) }
    fun selectSubject(id: String) = _state.update { it.copy(subjectId = id) }
    fun selectClass(id: String) = _state.update { it.copy(classId = id) }
    fun updateExamDate(value: String) = _state.update { it.copy(examDate = value) }
    fun updateDuration(value: String) = _state.update { it.copy(durationMinutes = value) }

    /** Step 0 -> 1: creates (or updates) the draft exam row. */
    fun saveBasicInfoAndContinue() {
        val s = _state.value
        val schoolId = s.schoolId ?: return
        val teacherId = s.teacherId ?: return
        val subjectId = s.subjectId ?: return
        if (!s.basicInfoValid) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val duration = s.durationMinutes.toIntOrNull()
            val result = if (s.examId == null) {
                examRepository.createDraftExam(
                    schoolId, teacherId, s.title, s.description, s.instructions,
                    subjectId, s.examDate.ifBlank { null }, duration
                ).map { examId -> examId }
            } else {
                examRepository.updateExamBasicInfo(
                    s.examId, s.title, s.description, s.instructions,
                    subjectId, s.examDate.ifBlank { null }, duration
                ).map { s.examId }
            }
            result
                .onSuccess { examId ->
                    _state.update { it.copy(isLoading = false, examId = examId, step = 1) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to save exam") }
                }
        }
    }

    fun uploadPaper(context: Context, uri: Uri) {
        val s = _state.value
        val schoolId = s.schoolId ?: return
        val examId = s.examId ?: return
        val file = readPickedFile(context, uri) ?: run {
            _state.update { it.copy(error = "Couldn't read the selected file.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            examRepository.uploadExamPaper(schoolId, examId, file.name, file.bytes)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, paperFileName = file.name) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Upload failed") }
                }
        }
    }

    fun uploadAnswerKey(context: Context, uri: Uri) {
        val s = _state.value
        val schoolId = s.schoolId ?: return
        val examId = s.examId ?: return
        val teacherId = s.teacherId ?: return
        val file = readPickedFile(context, uri) ?: run {
            _state.update { it.copy(error = "Couldn't read the selected file.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            examRepository.uploadAnswerKey(schoolId, examId, teacherId, file.name, file.bytes)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, answerKeyFileName = file.name) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Upload failed") }
                }
        }
    }

    fun goToStep(step: Int) = _state.update { it.copy(step = step, error = null) }

    fun addQuestion() = _state.update {
        val nextNumber = it.questions.size + 1
        it.copy(
            questions = it.questions + QuestionDraft(
                localId = UUID.randomUUID().toString(),
                questionNumber = nextNumber,
                questionText = "",
                questionType = QuestionType.SHORT_ANSWER,
                maxMarks = "",
                correctAnswer = "",
                gradingMode = GradingMode.AI
            )
        )
    }

    fun updateQuestion(localId: String, transform: (QuestionDraft) -> QuestionDraft) {
        _state.update { s ->
            s.copy(questions = s.questions.map { if (it.localId == localId) transform(it) else it })
        }
    }

    fun removeQuestion(localId: String) {
        _state.update { s ->
            val remaining = s.questions.filterNot { it.localId == localId }
                .mapIndexed { index, q -> q.copy(questionNumber = index + 1) }
            s.copy(questions = remaining)
        }
    }

    fun saveQuestionsAndContinue() {
        val s = _state.value
        val examId = s.examId ?: return
        if (s.questions.isEmpty()) {
            _state.update { it.copy(error = "Add at least one question.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            examRepository.saveQuestions(examId, s.questions)
                .onSuccess { _state.update { it.copy(isLoading = false, step = 4) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to save questions") } }
        }
    }

    fun publish() {
        val s = _state.value
        val examId = s.examId ?: return
        val classId = s.classId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            examRepository.publishExam(examId, classId)
                .onSuccess { _state.update { it.copy(isLoading = false, published = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to publish") } }
        }
    }
}
