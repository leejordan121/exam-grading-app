package com.examgrading.app.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class PickedFile(val name: String, val bytes: ByteArray)

fun readPickedFile(context: Context, uri: Uri): PickedFile? {
    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: uri.lastPathSegment ?: "file"

    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedFile(name = name, bytes = bytes)
}
