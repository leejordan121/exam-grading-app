package com.examgrading.app.ui.student.examdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailScreen(
    onBack: () -> Unit,
    onStartSubmission: (examId: String) -> Unit,
    viewModel: ExamDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.downloadUrl) {
        state.downloadUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            viewModel.consumeDownloadUrl()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.exam?.title ?: "Exam") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        val exam = state.exam
        if (exam == null) {
            Text(state.error ?: "Loading...", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                listOfNotNull(exam.subjectName, exam.examDate).joinToString(" • "),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Duration: ${exam.durationMinutes ?: "-"} minutes   Total Marks: ${exam.totalMarks}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (!exam.instructions.isNullOrBlank()) {
                Text("Instructions", style = MaterialTheme.typography.titleLarge)
                Text(exam.instructions, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
            }

            OutlinedButton(
                onClick = viewModel::downloadPaper,
                enabled = exam.paperFilePath != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Download / Preview Paper")
            }

            Button(
                onClick = { onStartSubmission(exam.examId) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("Scan & Submit")
            }
        }
    }
}
