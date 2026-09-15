package com.examgrading.app.ui.teacher.examcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.examgrading.app.domain.models.QuestionDraft
import com.examgrading.app.domain.models.QuestionType

@Composable
fun QuestionsStep(state: ExamWizardState, viewModel: ExamWizardViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            items(state.questions, key = { it.localId }) { question ->
                QuestionCard(
                    question = question,
                    onChange = { transform -> viewModel.updateQuestion(question.localId, transform) },
                    onRemove = { viewModel.removeQuestion(question.localId) }
                )
            }
            item {
                OutlinedButton(
                    onClick = viewModel::addQuestion,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Add Question")
                }
            }
        }

        Button(
            onClick = viewModel::saveQuestionsAndContinue,
            enabled = state.questions.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Next: Review")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionCard(
    question: QuestionDraft,
    onChange: ((QuestionDraft) -> QuestionDraft) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Question ${question.questionNumber}", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove question")
                }
            }

            OutlinedTextField(
                value = question.questionText,
                onValueChange = { text -> onChange { it.copy(questionText = text) } },
                label = { Text("Question text") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = question.questionType.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    QuestionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.label) },
                            onClick = {
                                onChange { it.copy(questionType = type) }
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = question.maxMarks,
                onValueChange = { marks -> onChange { it.copy(maxMarks = marks) } },
                label = { Text("Marks") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            OutlinedTextField(
                value = question.correctAnswer,
                onValueChange = { answer -> onChange { it.copy(correctAnswer = answer) } },
                label = { Text("Correct answer / expected answer") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
