package com.examgrading.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Read-only date field backed by a Material3 calendar picker; past dates are disabled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    isoDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val todayStartMillis = remember {
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    OutlinedTextField(
        value = isoDate,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick date")
            }
        },
        modifier = modifier.fillMaxWidth()
    )

    if (showPicker) {
        val initialMillis = isoDate.toEpochMillisOrNull() ?: todayStartMillis
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayStartMillis
            }
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millisToIsoDate(millis))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun String.toEpochMillisOrNull(): Long? = runCatching {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private fun millisToIsoDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
