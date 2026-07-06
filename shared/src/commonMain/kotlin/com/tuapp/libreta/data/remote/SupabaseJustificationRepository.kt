package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.JustificationSupabaseDto
import com.tuapp.libreta.data.remote.dto.StudentSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.JustificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToSqlDate
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration.Companion.hours

class SupabaseJustificationRepository(private val supabase: SupabaseClient) : JustificationRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Justification>> = flow {
        try {
            val response = supabase.from("justifications")
                .select { filter { eq("student_id", studentId.value) } }
                .decodeList<JustificationSupabaseDto>()
            emit(response.map { it.toDomain() })
        } catch (e: Exception) {
            AppLogger.e("JustificationRepository", "Error cargando justificaciones: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getPendingByTeacher(teacherId: UuidString): Flow<List<Justification>> = flow {
        try {
            // 1. Obtener cursos del profesor
            val coursesRaw = supabase.from("courses")
                .select { filter { eq("teacher_id", teacherId.value) } }
                .decodeList<com.tuapp.libreta.data.remote.dtos.CourseDto>()
            
            val courseNameMap = coursesRaw.associate { (it.id ?: "") to it.name }
            val courseIds = coursesRaw.mapNotNull { it.id }

            if (courseIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            // 2. Obtener los alumnos REALES de esos cursos desde la tabla students.
            //    justifications.student_id referencia students.id (NO enrollments.id),
            //    por eso aquí se consulta students y no enrollments.
            val students = supabase.from("students")
                .select {
                    filter {
                        isIn("course_id", courseIds)
                    }
                }
                .decodeList<StudentSupabaseDto>()

            if (students.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val studentMap = students.associateBy { it.id ?: "" }
            val studentIds = students.mapNotNull { it.id }

            // 3. Obtener justificaciones pendientes de esos alumnos
            val justifications = supabase.from("justifications")
                .select {
                    filter {
                        isIn("student_id", studentIds)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<JustificationSupabaseDto>()

            emit(justifications.map {
                val student = studentMap[it.studentId]
                it.toDomain().copy(
                    studentName = student?.fullName,
                    courseName  = courseNameMap[student?.courseId ?: ""]
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
                date      = epochMsToSqlDate(justification.date),
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
        var documentPath: String? = null
        
        if (fileBytes != null && fileName != null) {
            // Guardamos con una estructura que permita RLS: {owner_id}/{filename}
            val userId = supabase.storage.from("justifications").supabaseClient.auth.currentUserOrNull()?.id ?: "unknown"
            documentPath = "$userId/${currentEpochMs()}_$fileName"
            
            val bucket = supabase.storage.from("justifications")
            bucket.upload(documentPath, fileBytes) { upsert = true }
        }

        supabase.from("justifications").insert(
            JustificationSupabaseDto(
                studentId   = justification.studentId.value,
                date        = epochMsToSqlDate(justification.date),
                reason      = justification.reason,
                status      = justification.status.name,
                documentUrl = documentPath // Guardamos el PATH en la columna document_url
            )
        )
    }

    override suspend fun getAttachmentUrl(path: String): String = runCatching {
        if (path.startsWith("http")) return path // Compatibilidad con datos antiguos
        
        val bucket = supabase.storage.from("justifications")
        // Generamos una URL firmada válida por 1 hora
        bucket.createSignedUrl(path, expiresIn = 1.hours)
    }.getOrElse { "" }

    override suspend fun delete(id: UuidString) {
        supabase.from("justifications").delete { filter { eq("id", id.value) } }
    }
}
