package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.StudentDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.remote.dto.toDto
import com.tuapp.libreta.data.remote.dtos.EnrollmentDto
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.StudentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseStudentRepository(private val supabase: SupabaseClient) : StudentRepository {

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> = flow {
        val result = supabase.from("students")
            .select { filter { eq("course_id", classId.value) } }
            .decodeList<StudentDto>()
        emit(result.map { it.toDomain() })
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
            val result = supabase.postgrest["enrollments"]
                .select {
                    filter { eq("parent_id", currentUser.id) }
                }
            println("DEBUG getStudents: respuesta raw = ${result.data}")

            val list = result.decodeList<EnrollmentDto>()
            println("DEBUG getStudents: registros decodificados = ${list.size}")
            
            emit(list.map { it.toStudentDomain() })

        } catch (e: Exception) {
            println("ERROR getStudents: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    private fun EnrollmentDto.toStudentDomain() = Student(
        id = UuidString(id ?: parentId),
        fullName = studentName,
        courseId = UuidString(courseId),
        parentId = UuidString(parentId)
    )

    override suspend fun saveStudent(student: Student) {
        supabase.from("students").upsert(student.toDto())
    }

    override suspend fun deleteStudent(id: UuidString) {
        supabase.from("students").delete { filter { eq("id", id.value) } }
    }
}
