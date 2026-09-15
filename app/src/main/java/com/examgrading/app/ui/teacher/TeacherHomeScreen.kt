package com.examgrading.app.ui.teacher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder — next up: exam creation wizard + submissions dashboard (spec section 77, 79).
@Composable
fun TeacherHomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Teacher dashboard", style = MaterialTheme.typography.titleLarge)
    }
}
