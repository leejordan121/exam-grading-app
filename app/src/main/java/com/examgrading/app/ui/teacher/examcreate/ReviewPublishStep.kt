package com.examgrading.app.ui.teacher.examcreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewPublishStep(state: ExamWizardState, viewModel: ExamWizardViewModel) {
    val totalMarks = state.questions.sumOf { it.maxMarks.toDoubleOrNull() ?: 0.0 }
    val subjectName = state.subjects.find { it.id == state.subjectId }?.name ?: "-"
    val className = state.classes.find { it.id == state.classId }?.name ?: "-"

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(state.title, style = MaterialTheme.typography.headlineMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        SummaryRow("Subject", subjectName)
        SummaryRow("Class", className)
        SummaryRow("Date", state.examDate.ifBlank { "-" })
        SummaryRow("Duration", state.durationMinutes.ifBlank { "-" } + " minutes")
        SummaryRow("Questions", "${state.questions.size}")
        SummaryRow("Total marks", "$totalMarks")
        SummaryRow("Exam paper", state.paperFileName ?: "-")
        SummaryRow("Answer key", state.answerKeyFileName ?: "-")

        Text(
            "Once published, students in this class will see the exam and be able to download the paper.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        Button(onClick = viewModel::publish, modifier = Modifier.fillMaxWidth()) {
            Text("Publish Exam")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
