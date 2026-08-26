package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.CourseAssignmentSupabaseDto
import com.tuapp.libreta.data.remote.dto.InvitationCodeSupabaseDto
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
            .decodeList<CourseAssignmentSupabaseDto>().map { it.toDomain() })
    }

    override suspend fun assignByCode(code: String, teacherId: UuidString): Result<Unit> = runCatching {
        // 1. Buscar código válido
        val invite = supabase.from("invitation_codes")
            .select { filter { 
                eq("code", code.uppercase())
            } }
            .decodeSingleOrNull<InvitationCodeSupabaseDto>() ?: throw Exception("Código inválido o expirado")

        // El esquema actual no tiene course_id en invitation_codes. 
        // Usamos student_id como fallback o arrojamos error si no es un flujo soportado.
        val studentId = invite.studentId ?: throw Exception("Código no tiene alumno/curso asociado")

        // 2. Asignar profesor al curso (vía el curso del alumno del código)
        // Por ahora lanzamos error ya que el flujo de invitar colegas no está alineado con el esquema 3NF
        throw Exception("El flujo de invitación para profesores no está habilitado en este esquema.")
    }

    override suspend fun assign(assignment: CourseAssignment) {
        AppLogger.uuid("assign", "courseId", assignment.courseId.value, "VALID")
        val schoolId = assignment.schoolId

        val dto = CourseAssignmentSupabaseDto(
            id            = assignment.id?.value,
            teacherId     = assignment.teacherId.value,
            courseId      = assignment.courseId.value,
            schoolId      = schoolId.value
        )
        supabase.from("course_assignments").upsert(dto)
    }

    override suspend fun generateColleagueInvite(courseId: UuidString, schoolId: UuidString, issuedByTeacherId: UuidString): String {
        // Fallback: Este flujo requiere columnas nuevas en DB.
        throw Exception("El flujo de invitación para colegas requiere actualización de esquema en la tabla invitation_codes.")
    }
}
