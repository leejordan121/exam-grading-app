package com.examgrading.app.domain.models

// Named SchoolClass (not "Class") to avoid colliding with kotlin.reflect.Class.
data class SchoolClass(
    val id: String,
    val name: String,
    val grade: String?,
    val section: String?
)
