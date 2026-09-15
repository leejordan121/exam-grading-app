package com.examgrading.app.ui.admin.exams

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.examgrading.app.domain.models.AdminExamSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExamsScreen(
    onBack: () -> Unit,
    viewModel: AdminExamsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Exams") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.exams.isEmpty() -> Text(
                    "No exams have been created yet.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.exams, key = { it.examId }) { exam ->
                        AdminExamCard(exam = exam, onDeleteClick = { viewModel.requestDelete(exam) })
                    }
                }
            }
            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                )
            }
        }
    }

    val examPendingDelete = state.examPendingDelete
    if (examPendingDelete != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete exam?") },
            text = { Text("\"${examPendingDelete.title}\" and all its submissions and grades will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete, enabled = !state.isDeleting) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete, enabled = !state.isDeleting) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminExamCard(exam: AdminExamSummary, onDeleteClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    listOfNotNull(exam.subjectName, exam.teacherName, exam.examDate, exam.status)
                        .joinToString(" • "),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete exam", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
