package com.examgrading.app.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Submissions dashboard (spec section 77) is the next piece to add here.
@Composable
fun TeacherHomeScreen(onCreateExam: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Teacher dashboard", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onCreateExam, modifier = Modifier.padding(top = 24.dp)) {
            Text("Create Exam")
        }
    }
}
