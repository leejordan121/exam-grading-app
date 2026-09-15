package com.examgrading.app.ui.student.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.examgrading.app.domain.models.ResultQuestionRow
import com.examgrading.app.ui.common.submissionStatusLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    onBack: () -> Unit,
    viewModel: ResultDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.examTitle ?: "Result") },
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
            detail == null -> Text(state.error ?: "Not found", modifier = Modifier.padding(padding).padding(16.dp))
            !detail.resultVisible -> Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(submissionStatusLabel(detail.status), style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your result isn't available yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(
                        "${detail.finalScore} / ${detail.totalMarks}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        listOfNotNull(
                            detail.percentage?.let { "${"%.1f".format(it)}%" },
                            detail.grade
                        ).joinToString("   "),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (detail.showBreakdown && detail.questions.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        items(detail.questions) { q -> QuestionRow(q) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(question: ResultQuestionRow) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Question ${question.questionNumber}", style = MaterialTheme.typography.titleLarge)
            Text(question.questionText, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Text(
                "${question.score ?: "-"} / ${question.maxMarks}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            if (!question.feedback.isNullOrBlank()) {
                Text(question.feedback, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
