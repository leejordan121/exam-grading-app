package com.examgrading.app.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * ACTION_OPEN_DOCUMENT with no MIME restriction. EXTRA_MIME_TYPES filtering
 * isn't honored consistently across every picker source (some cloud/gallery
 * providers hide files entirely instead of just greying them out), so this
 * shows everything and the caller validates the extension after picking —
 * PDF/JPG/PNG/DOC(X) per spec section 11.
 */
class DocumentPickerContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}
