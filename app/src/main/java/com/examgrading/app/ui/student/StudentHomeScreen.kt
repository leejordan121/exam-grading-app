package com.examgrading.app.ui.student

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Placeholder — next up: Upcoming/Available/Submitted/Results tabs (spec section 17).
@Composable
fun StudentHomeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Student dashboard", style = MaterialTheme.typography.titleLarge)
    }
}
