package com.examgrading.app.ui.admin.people

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
import com.examgrading.app.domain.models.StaffSummary
import com.examgrading.app.ui.common.SimpleDropdown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    viewModel: PeopleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val titleLabel = if (state.role == "teacher") "Teachers" else "Students"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add $titleLabel")
            }
        }
    ) { padding: PaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.people.isEmpty() -> Text(
                    "No $titleLabel yet. Add one.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(state.people, key = { it.id }) { person -> PersonCard(person) }
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
        AddPersonDialog(
            role = state.role,
            classes = state.classes,
            isSaving = state.isSaving,
            onDismiss = { showAddDialog = false },
            onConfirm = { email, fullName, classId ->
                viewModel.addPerson(email, fullName, classId)
                showAddDialog = false
            }
        )
    }

    val created = state.lastCreated
    if (created != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCreatedResult,
            title = { Text("Account created") },
            text = {
                Column {
                    Text("Share these sign-in details with them:")
                    Text(
                        "Temporary password: ${created.tempPassword}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (!created.warning.isNullOrBlank()) {
                        Text(
                            created.warning,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissCreatedResult) { Text("Done") }
            }
        )
    }
}

@Composable
private fun PersonCard(person: StaffSummary) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(person.fullName, style = MaterialTheme.typography.titleLarge)
            Text(person.email, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AddPersonDialog(
    role: String,
    classes: List<com.examgrading.app.domain.models.SchoolClass>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (email: String, fullName: String, classId: String?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var classId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (role == "teacher") "Add Teacher" else "Add Student") },
        text = {
            Column {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full name") })
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (role == "student") {
                    SimpleDropdown(
                        label = "Class",
                        options = classes,
                        optionLabel = { it.name },
                        idOf = { it.id },
                        selectedId = classId,
                        onSelect = { classId = it.id },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = email.isNotBlank() && fullName.isNotBlank() && !isSaving,
                onClick = { onConfirm(email, fullName, classId) }
            ) {
                Text("Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
