package com.examgrading.app.ui.admin.classes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.ui.common.SimpleDropdown

private val GRADES = (1..12).map { it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassesScreen(
    onBack: () -> Unit,
    viewModel: ClassesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add class")
            }
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.classes.isEmpty() -> Text(
                    "No classes yet. Add one.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.classes, key = { it.id }) { schoolClass -> ClassCard(schoolClass) }
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

    if (showAddDialog) {
        AddClassDialog(
            isSaving = state.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { grade, section, academicYear ->
                viewModel.addClass(grade, section, academicYear)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ClassCard(schoolClass: SchoolClass) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(schoolClass.name, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun AddClassDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (grade: String, section: String, academicYear: String) -> Unit
) {
    var grade by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Class") },
        text = {
            Column {
                SimpleDropdown(
                    label = "Grade",
                    options = GRADES,
                    optionLabel = { "Grade $it" },
                    idOf = { it },
                    selectedId = grade,
                    onSelect = { grade = it }
                )
                OutlinedTextField(
                    value = section,
                    onValueChange = { section = it },
                    label = { Text("Section (e.g. A)") },
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = academicYear,
                    onValueChange = { academicYear = it },
                    label = { Text("Academic year (optional)") },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = grade.isNotBlank() && !isSaving, onClick = { onConfirm(grade, section, academicYear) }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
