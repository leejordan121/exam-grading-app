package com.examgrading.app.data.admin

import com.examgrading.app.core.network.ensureValidSession
import com.examgrading.app.data.exams.ClassDto
import com.examgrading.app.data.exams.SubjectDto
import com.examgrading.app.data.exams.toDomain
import com.examgrading.app.domain.models.CreatedUserResult
import com.examgrading.app.domain.models.SchoolClass
import com.examgrading.app.domain.models.StaffSummary
import com.examgrading.app.domain.models.Subject
import com.examgrading.app.domain.repositories.AdminRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.call.body
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AdminRepository {

    override suspend fun getSubjects(schoolId: String): Result<List<Subject>> = runCatching {
        supabase.postgrest.from("subjects")
            .select(columns = Columns.list("id", "name", "code")) {
                filter { eq("school_id", schoolId) }
                order("name", Order.ASCENDING)
            }
            .decodeList<SubjectDto>()
            .map { it.toDomain() }
    }

    override suspend fun createSubject(schoolId: String, name: String, code: String?): Result<Unit> = runCatching {
        supabase.postgrest.from("subjects").insert(SubjectInsertDto(schoolId, name, code))
        Unit
    }

    override suspend fun getClasses(schoolId: String): Result<List<SchoolClass>> = runCatching {
        supabase.postgrest.from("classes")
            .select(columns = Columns.list("id", "name", "grade", "section")) {
                filter { eq("school_id", schoolId) }
                order("grade", Order.ASCENDING)
                order("name", Order.ASCENDING)
            }
            .decodeList<ClassDto>()
            .map { it.toDomain() }
    }

    override suspend fun createClass(
        schoolId: String,
        name: String,
        grade: String?,
        section: String?,
        academicYear: String?
    ): Result<Unit> = runCatching {
        supabase.postgrest.from("classes").insert(ClassInsertDto(schoolId, name, grade, section, academicYear))
        Unit
    }

    override suspend fun getStaff(schoolId: String, role: String): Result<List<StaffSummary>> = runCatching {
        supabase.postgrest.from("profiles")
            .select(columns = Columns.list("id", "full_name", "email")) {
                filter {
                    eq("school_id", schoolId)
                    eq("role", role)
                }
                order("full_name", Order.ASCENDING)
            }
            .decodeList<StaffRow>()
            .map { StaffSummary(id = it.id, fullName = it.fullName, email = it.email) }
    }

    override suspend fun createUser(
        email: String,
        fullName: String,
        role: String,
        classId: String?
    ): Result<CreatedUserResult> = runCatching {
        supabase.ensureValidSession()
        val response = supabase.functions.invoke(
            "create-user",
            body = CreateUserRequestDto(email = email, fullName = fullName, role = role, classId = classId)
        )
        val decoded = response.body<CreateUserResponseDto>()
        if (decoded.userId == null || decoded.tempPassword == null) {
            error(decoded.error ?: "Failed to create user")
        }
        CreatedUserResult(userId = decoded.userId, tempPassword = decoded.tempPassword, warning = decoded.warning)
    }
}
