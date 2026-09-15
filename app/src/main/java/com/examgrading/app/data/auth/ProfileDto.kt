package com.examgrading.app.data.auth

import com.examgrading.app.domain.models.UserProfile
import com.examgrading.app.domain.models.UserRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("school_id") val schoolId: String,
    val role: String,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

fun ProfileDto.toDomain() = UserProfile(
    id = id,
    schoolId = schoolId,
    role = UserRole.fromDb(role),
    fullName = fullName,
    email = email,
    avatarUrl = avatarUrl
)
