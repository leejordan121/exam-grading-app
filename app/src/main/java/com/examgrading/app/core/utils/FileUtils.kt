package com.examgrading.app.core.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

data class PickedFile(val name: String, val bytes: ByteArray)

fun readPickedFile(context: Context, uri: Uri): PickedFile? {
    val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    } ?: uri.lastPathSegment ?: "file"

    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedFile(name = name, bytes = bytes)
}

/** Creates a fresh file under cacheDir/captures/ and returns a content:// Uri the camera app can write to. */
fun createCaptureUri(context: Context, fileName: String): Uri {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun readBytesFromUri(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
