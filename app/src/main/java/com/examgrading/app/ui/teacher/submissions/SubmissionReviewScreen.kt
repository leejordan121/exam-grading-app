package com.examgrading.app.ui.teacher.submissions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.examgrading.app.domain.models.QuestionReview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionReviewScreen(
    onBack: () -> Unit,
    viewModel: SubmissionReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.studentName ?: "Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        val detail = state.detail

        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(padding).padding(24.dp))
            detail == null -> Text(
                state.error ?: "Not found",
                modifier = Modifier.padding(padding).padding(16.dp)
            )
            detail.questions.isEmpty() -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    "Grading not yet complete. This submission hasn't been processed by the AI grading pipeline.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                items(detail.questions, key = { it.extractedAnswerId }) { question ->
                    QuestionReviewCard(
                        question = question,
                        isSaving = state.isSaving,
                        onSave = { newScore, reason ->
                            viewModel.saveOverride(question.extractedAnswerId, question.questionId, newScore, reason)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionReviewCard(
    question: QuestionReview,
    isSaving: Boolean,
    onSave: (newScore: Double, reason: String) -> Unit
) {
    var scoreInput by remember(question.extractedAnswerId) {
        mutableStateOf((question.finalScore ?: question.aiScore ?: 0.0).toString())
    }
    var reason by remember(question.extractedAnswerId) { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Question ${question.questionNumber}", style = MaterialTheme.typography.titleLarge)
            Text(question.questionText, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

            LabeledText("Student's answer", question.rawText ?: "Unable to confidently read answer.")
            LabeledText("Correct answer", question.correctAnswer ?: "-")

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row {
                LabeledText(
                    "AI score",
                    if (question.aiScore != null) "${question.aiScore} / ${question.maxMarks}" else "-",
                    modifier = Modifier.weight(1f)
                )
                LabeledText(
                    "Confidence",
                    question.confidence?.let { "${(it * 100).toInt()}%" } ?: "-",
                    modifier = Modifier.weight(1f)
                )
            }
            if (!question.gradingReason.isNullOrBlank()) {
                LabeledText("AI reasoning", question.gradingReason, modifier = Modifier.padding(top = 8.dp))
            }
            if (question.needsReview) {
                Text(
                    "Flagged for review",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            OutlinedTextField(
                value = scoreInput,
                onValueChange = { scoreInput = it },
                label = { Text("Final score (max ${question.maxMarks})") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for change (optional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            Button(
                onClick = { scoreInput.toDoubleOrNull()?.let { onSave(it, reason) } },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("Save Score")
            }
        }
    }
}

@Composable
private fun LabeledText(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
