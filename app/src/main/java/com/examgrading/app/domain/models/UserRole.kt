package com.examgrading.app.domain.models

enum class UserRole {
    ADMIN,
    TEACHER,
    STUDENT;

    companion object {
        fun fromDb(value: String): UserRole = when (value) {
            "admin" -> ADMIN
            "teacher" -> TEACHER
            "student" -> STUDENT
            else -> throw IllegalArgumentException("Unknown role: $value")
        }
    }
}
