package com.examgrading.app.domain.repositories

import com.examgrading.app.domain.models.AdminExamSummary
import com.examgrading.app.domain.models.CreatedUserResult
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.StaffSummary
import com.examgrading.app.domain.models.Subject

interface AdminRepository {
    /** All exams in the school, across every teacher, for the admin exams list. */
    suspend fun getAllExams(schoolId: String): Result<List<AdminExamSummary>>
    suspend fun getSubjects(schoolId: String): Result<List<Subject>>
    suspend fun createSubject(schoolId: String, name: String, code: String?): Result<Unit>

    suspend fun getClasses(schoolId: String): Result<List<SchoolClass>>
    suspend fun createClass(
        schoolId: String,
        name: String,
        grade: String?,
        section: String?,
        academicYear: String?
    ): Result<Unit>

    /** role is "teacher" or "student". */
    suspend fun getStaff(schoolId: String, role: String): Result<List<StaffSummary>>

    /** Provisions a new auth user + profile via the create-user Edge Function. */
    suspend fun createUser(
        email: String,
        fullName: String,
        role: String,
        classId: String?
    ): Result<CreatedUserResult>
}
