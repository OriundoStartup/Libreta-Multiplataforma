package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.EnrollmentSupabaseDto
import com.tuapp.libreta.data.remote.dto.StudentSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.StudentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException

class SupabaseStudentRepository(private val supabase: SupabaseClient) : StudentRepository {

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> = flow {
        try {
            val result = supabase.from("enrollments")
                .select { filter { eq("course_id", classId.value) } }
                .decodeList<EnrollmentSupabaseDto>()
            emit(result.map { enrollment ->
                // Generar un ID lo más único posible para evitar crashes en LazyColumn de Android
                val safeId = enrollment.id 
                    ?: "${enrollment.courseId}-${enrollment.parentId}-${enrollment.studentName.replace(" ", "")}"
                
                Student(
                    id = UuidString(safeId),
                    fullName = enrollment.studentName,
                    courseId = classId,
                    parentId = UuidString(enrollment.parentId)
                )
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("ERROR getStudentsByClass: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> = flow {
        val currentUser = supabase.auth.currentUserOrNull()
        println("DEBUG getStudents: currentUser=${currentUser?.id}")

        if (currentUser == null) {
            println("DEBUG getStudents: NO HAY SESIÓN ACTIVA")
            emit(emptyList())
            return@flow
        }

        try {
            val list = supabase.from("enrollments")
                .select {
                    filter { eq("parent_id", currentUser.id) }
                }
                .decodeList<EnrollmentSupabaseDto>()
            
            AppLogger.d("StudentRepository", "Decoded ${list.size} students for parent ${currentUser.id}")
            
            emit(list.map { it.toStudentDomain() })

        } catch (e: CancellationException) {
            // Relanzar para que .first() funcione correctamente sin violar transparencia
            throw e
        } catch (e: Exception) {
            println("ERROR getStudents: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    private fun EnrollmentSupabaseDto.toStudentDomain(): Student {
        val safeId = id ?: "$courseId-$parentId-${studentName.replace(" ", "")}"
        return Student(
            id = UuidString(safeId),
            fullName = studentName,
            courseId = UuidString(courseId),
            parentId = UuidString(parentId)
        )
    }

    override suspend fun saveStudent(student: Student) {
        supabase.from("students").upsert(
            StudentSupabaseDto(
                id = student.id.value,
                fullName = student.fullName,
                studentRut = student.studentRut,
                courseId = student.courseId.value,
                parentId = student.parentId.value
            )
        )
    }

    override suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit> = runCatching {
        supabase.from("enrollments").update({
            set("student_name", name)
            set("student_rut", rut)
        }) { filter { eq("id", id.value) } }
        Unit
    }

    override suspend fun deleteStudent(id: UuidString) {
        supabase.from("enrollments").delete { filter { eq("id", id.value) } }
    }
}
