package com.examgrading.app.ui.teacher.examcreate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun UploadPaperStep(state: ExamWizardState, viewModel: ExamWizardViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadPaper(context, it) }
    }

    UploadStepContent(
        heading = "Upload the exam paper",
        description = "PDF is preferred; JPG and PNG are also supported. Students will download and print this.",
        fileName = state.paperFileName,
        canContinue = state.paperFileName != null,
        onPick = { launcher.launch("application/pdf") },
        onContinue = { viewModel.goToStep(2) }
    )
}

@Composable
fun UploadAnswerKeyStep(state: ExamWizardState, viewModel: ExamWizardViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAnswerKey(context, it) }
    }

    UploadStepContent(
        heading = "Upload the official answer key",
        description = "This is stored separately and is never accessible to students.",
        fileName = state.answerKeyFileName,
        canContinue = state.answerKeyFileName != null,
        onPick = { launcher.launch("application/pdf") },
        onContinue = { viewModel.goToStep(3) }
    )
}

@Composable
private fun UploadStepContent(
    heading: String,
    description: String,
    fileName: String?,
    canContinue: Boolean,
    onPick: () -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(heading, style = MaterialTheme.typography.titleLarge)
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        if (fileName != null) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Text(fileName, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        }

        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Text(if (fileName == null) "  Choose File" else "  Replace File", modifier = Modifier.padding(start = 4.dp))
        }

        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Next")
        }
    }
}
