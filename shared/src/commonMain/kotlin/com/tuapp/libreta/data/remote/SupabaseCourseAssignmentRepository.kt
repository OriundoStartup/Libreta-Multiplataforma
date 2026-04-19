package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.CourseAssignmentDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.domain.model.CourseAssignment
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

@Serializable
private data class SchoolInsert(val name: String, val address: String = "")
@Serializable
private data class SchoolId(val id: String)

class SupabaseCourseAssignmentRepository(private val supabase: SupabaseClient) : CourseAssignmentRepository {

    override fun getByTeacher(teacherId: UuidString): Flow<List<CourseAssignment>> = flow {
        emit(supabase.from("course_assignments")
            .select { filter { eq("teacher_id", teacherId.value) } }
            .decodeList<CourseAssignmentDto>().map { it.toDomain() })
    }

    override suspend fun assign(assignment: CourseAssignment) {
        AppLogger.uuid("assign", "courseId", assignment.courseId.value, "VALID")
        val schoolId = assignment.schoolId

        val dto = CourseAssignmentDto(
            id            = assignment.id?.value,
            teacherId     = assignment.teacherId.value,
            courseId      = assignment.courseId.value,
            schoolId      = schoolId.value,
            isHeadTeacher = assignment.isHeadTeacher
        )
        supabase.from("course_assignments").upsert(dto) {
            onConflict = "teacher_id,course_id,school_id"
        }
    }

    override suspend fun generateColleagueInvite(courseId: UuidString, schoolId: UuidString, issuedByTeacherId: UuidString): String {
        val code = (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        supabase.from("invitation_codes").insert(mapOf(
            "code"       to code,
            "teacher_id" to issuedByTeacherId.value,
            "expires_at" to epochMsToIso(currentEpochMs() + 48 * 3600 * 1000L)
        ))
        return code
    }
}
