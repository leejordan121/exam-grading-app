package com.examgrading.app.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminHomeScreen(
    onOpenExams: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenTeachers: () -> Unit,
    onOpenStudents: () -> Unit
) {
    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "Admin",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            AdminSectionCard("Exams", "View and delete exams created by any teacher", onOpenExams)
            AdminSectionCard("Subjects", "Manage the subjects taught at your school", onOpenSubjects)
            AdminSectionCard("Classes", "Manage grade levels and sections", onOpenClasses)
            AdminSectionCard("Teachers", "Add and view teacher accounts", onOpenTeachers)
            AdminSectionCard("Students", "Add and view student accounts", onOpenStudents)
        }
    }
}

@Composable
private fun AdminSectionCard(title: String, description: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
