package com.examgrading.app.ui.student.submission

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: SubmissionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCaptureResult(context, success)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) captureLauncher.launch(viewModel.prepareCapture(context))
    }

    LaunchedEffect(state.submitted) {
        if (state.submitted) onSubmitted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Exam") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            Text(
                "Place the complete paper inside the frame for each page. ${state.pageNumbers.size} page(s) captured.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                items(state.pageNumbers) { pageNumber ->
                    Box(modifier = Modifier.padding(4.dp)) {
                        val thumb = state.pageThumbnails[pageNumber]
                        if (thumb != null) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = "Page $pageNumber",
                                modifier = Modifier.aspectRatio(0.7f).fillMaxWidth()
                            )
                        } else {
                            Box(
                                modifier = Modifier.aspectRatio(0.7f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Page $pageNumber")
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deletePage(pageNumber) },
                            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete page", tint = Color.Red)
                        }
                    }
                }
            }

            if (state.isUploading) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Text("  Scan Page", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = { showConfirm = true },
                    enabled = state.pageNumbers.isNotEmpty() && !state.isLoading,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text("Submit Exam")
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Submit exam?") },
            text = { Text("You are about to submit ${state.pageNumbers.size} page(s). You may not be able to upload another submission afterward.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.submit()
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
