package com.examgrading.app.domain.models

data class StaffSummary(
    val id: String,
    val fullName: String,
    val email: String
)

data class CreatedUserResult(
    val userId: String,
    val tempPassword: String,
    val warning: String?
)
