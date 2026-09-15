package com.examgrading.app.ui.common

// Maps the raw submissions.status db value (spec section 27) to what a
// student should actually read on screen.
fun submissionStatusLabel(status: String): String = when (status) {
    "draft" -> "Not submitted"
    "uploading" -> "Uploading..."
    "uploaded" -> "Submitted — waiting to be graded"
    "processing" -> "Processing..."
    "ocr_processing" -> "Reading your answers..."
    "grading" -> "Grading in progress..."
    "needs_review" -> "Under teacher review"
    "graded" -> "Graded"
    "failed" -> "Grading failed — please contact your teacher"
    else -> status
}
