package com.examgrading.app.ui.student

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.examgrading.app.domain.models.ExamSummary
import com.examgrading.app.domain.models.StudentSubmission

@Composable
fun StudentHomeScreen(
    onOpenExam: (examId: String) -> Unit,
    viewModel: StudentHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Exams") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Results") })
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (tab == 0) {
                ExamsList(exams = state.exams, onOpenExam = onOpenExam)
            } else {
                ResultsList(results = state.results)
            }
        }
    }
}

@Composable
private fun ExamsList(exams: List<ExamSummary>, onOpenExam: (String) -> Unit) {
    if (exams.isEmpty()) {
        EmptyState("No exams assigned yet.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(exams, key = { it.examId }) { exam ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onOpenExam(exam.examId) }
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(exam.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(exam.subjectName, exam.examDate).joinToString(" • "),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Status: ${exam.assignmentStatus}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsList(results: List<StudentSubmission>) {
    if (results.isEmpty()) {
        EmptyState("No results yet.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(results, key = { it.id }) { result ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(result.examTitle, style = MaterialTheme.typography.titleLarge)
                    val scoreText = if (result.resultVisible && result.finalScore != null) {
                        "${result.finalScore} / ${result.totalMarks}  (${result.grade ?: "-"})"
                    } else {
                        "Status: ${result.status}"
                    }
                    Text(scoreText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

