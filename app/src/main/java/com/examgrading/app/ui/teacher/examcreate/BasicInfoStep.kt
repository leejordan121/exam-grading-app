package com.examgrading.app.ui.teacher.examcreate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.examgrading.app.ui.common.DatePickerField
import com.examgrading.app.ui.common.SimpleDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoStep(state: ExamWizardState, viewModel: ExamWizardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Exam name") },
            modifier = Modifier.fillMaxWidth()
        )

        SimpleDropdown(
            label = "Subject",
            options = state.subjects,
            optionLabel = { it.name },
            idOf = { it.id },
            selectedId = state.subjectId,
            onSelect = { viewModel.selectSubject(it.id) },
            modifier = Modifier.padding(top = 12.dp)
        )

        SimpleDropdown(
            label = "Class",
            options = state.classes,
            optionLabel = { it.name },
            idOf = { it.id },
            selectedId = state.classId,
            onSelect = { viewModel.selectClass(it.id) },
            modifier = Modifier.padding(top = 12.dp)
        )

        DatePickerField(
            label = "Exam date",
            isoDate = state.examDate,
            onDateSelected = viewModel::updateExamDate,
            modifier = Modifier.padding(top = 12.dp)
        )

        OutlinedTextField(
            value = state.durationMinutes,
            onValueChange = viewModel::updateDuration,
            label = { Text("Duration (minutes)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        OutlinedTextField(
            value = state.instructions,
            onValueChange = viewModel::updateInstructions,
            label = { Text("Instructions for students") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )

        Button(
            onClick = viewModel::saveBasicInfoAndContinue,
            enabled = state.basicInfoValid,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            Text("Next: Upload Paper")
        }
    }
}
