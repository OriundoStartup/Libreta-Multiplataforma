package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.EnrollmentSupabaseDto
import com.tuapp.libreta.data.remote.dto.JustificationSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.JustificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import com.tuapp.libreta.data.util.currentEpochMs
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseJustificationRepository(private val supabase: SupabaseClient) : JustificationRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Justification>> = flow {
        val response = supabase.from("justifications")
            .select { filter { eq("student_id", studentId.value) } }
            .decodeList<JustificationSupabaseDto>()
        emit(response.map { it.toDomain() })
    }

    override fun getPendingByTeacher(teacherId: UuidString): Flow<List<Justification>> = flow {
        try {
            // 1. Obtener cursos del profesor
            val coursesRaw = supabase.from("courses")
                .select { filter { eq("teacher_id", teacherId.value) } }
                .decodeList<com.tuapp.libreta.data.remote.dtos.CourseDto>()
            
            val courseNameMap = coursesRaw.associate { (it.id ?: "") to it.name }
            val courseIds = coursesRaw.map { it.id ?: "" }

            if (courseIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            // 2. Obtener IDs de alumnos (enrollments) de esos cursos
            val enrollments = supabase.from("enrollments")
                .select { filter { isIn("course_id", courseIds) } }
                .decodeList<EnrollmentSupabaseDto>()
            
            if (enrollments.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val enrollmentMap = enrollments.associateBy { it.id ?: "" }

            // 3. Obtener justificaciones pendientes de esos alumnos
            val studentIds = enrollments.map { it.id ?: "" }
            val justifications = supabase.from("justifications")
                .select { filter { 
                    isIn("student_id", studentIds)
                    eq("status", "PENDING")
                } }
                .decodeList<JustificationSupabaseDto>()
            
            emit(justifications.map { 
                val enrollment = enrollmentMap[it.studentId]
                val studentName = enrollment?.studentName
                val courseName = courseNameMap[enrollment?.courseId ?: ""]
                it.toDomain().copy(
                    studentName = studentName,
                    courseName = courseName
                )
            })
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun save(justification: Justification) {
        supabase.from("justifications").upsert(
            JustificationSupabaseDto(
                id        = justification.id?.value,
                studentId = justification.studentId.value,
                date      = justification.date.toString(),
                reason    = justification.reason,
                status    = justification.status.name
            )
        )
    }

    override suspend fun saveWithAttachment(
        justification: Justification,
        fileBytes: ByteArray?,
        fileName: String?
    ): Result<Unit> = runCatching {
        var documentUrl: String? = null
        
        if (fileBytes != null && fileName != null) {
            val path = "certificados/${justification.studentId.value}/${currentEpochMs()}_$fileName"
            val bucket = supabase.storage.from("justifications")
            bucket.upload(path, fileBytes) { upsert = true }
            documentUrl = bucket.publicUrl(path)
        }

        supabase.from("justifications").insert(
            JustificationSupabaseDto(
                studentId   = justification.studentId.value,
                date        = justification.date.toString(),
                reason      = justification.reason,
                status      = justification.status.name,
                documentUrl = documentUrl
            )
        )
    }

    override suspend fun delete(id: UuidString) {
        supabase.from("justifications").delete { filter { eq("id", id.value) } }
    }
}
