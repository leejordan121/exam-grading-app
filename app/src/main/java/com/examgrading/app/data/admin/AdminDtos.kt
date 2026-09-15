package com.examgrading.app.data.admin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectInsertDto(
    @SerialName("school_id") val schoolId: String,
    val name: String,
    val code: String? = null
)

@Serializable
data class ClassInsertDto(
    @SerialName("school_id") val schoolId: String,
    val name: String,
    val grade: String? = null,
    val section: String? = null,
    @SerialName("academic_year") val academicYear: String? = null
)

@Serializable
data class StaffRow(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val email: String
)

@Serializable
data class CreateUserRequestDto(
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("class_id") val classId: String? = null
)

@Serializable
data class CreateUserResponseDto(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("temp_password") val tempPassword: String? = null,
    val warning: String? = null,
    val error: String? = null
)
