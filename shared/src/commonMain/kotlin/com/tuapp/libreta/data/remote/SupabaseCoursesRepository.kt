package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dtos.CourseDto
import com.tuapp.libreta.data.remote.dtos.EnrollmentDto
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.domain.model.Course
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.from

interface CoursesRepository {
    suspend fun createCourse(
        name: String,
        description: String?,
        subject: String?,
        grade: String?,
        schoolName: String?
    ): Result<Course>
    suspend fun getTeacherCourses(): Result<List<Course>>
    suspend fun getCourseByInviteCode(code: String): Result<Course?>
    suspend fun enrollStudent(courseId: String, studentName: String, studentRut: String? = null): Result<Unit>
    suspend fun updateCourse(
        courseId: String,
        name: String,
        description: String?,
        subject: String?,
        grade: String?
    ): Result<Course>
    suspend fun deleteCourse(courseId: String): Result<Unit>
}

class SupabaseCoursesRepository(private val supabase: SupabaseClient) : CoursesRepository {

    override suspend fun deleteCourse(courseId: String): Result<Unit> = runCatching {
        supabase.postgrest["courses"].delete { filter { eq("id", courseId) } }
        Unit
    }

    override suspend fun createCourse(
        name: String,
        description: String?,
        subject: String?,
        grade: String?,
        schoolName: String?
    ): Result<Course> = runCatching {
        println("DEBUG: Iniciando createCourse()")
        val user = supabase.auth.currentUserOrNull()
        println("DEBUG: currentUser = ${user?.id}")
        if (user == null) throw Exception("No hay sesión activa")
        
        // 1. Generar código único en el servidor para evitar duplicados
        println("DEBUG: Llamando RPC generate_invite_code")
        val inviteCode = supabase.postgrest.rpc("generate_invite_code").decodeAs<String>()
        println("DEBUG: inviteCode recibido = $inviteCode")
        
        val dto = CourseDto(
            teacherId = user.id,
            name = name,
            description = description,
            subject = subject,
            grade = grade,
            schoolName = schoolName,
            inviteCode = inviteCode
        )
        println("DEBUG: dto a insertar = $dto")
        
        val response = supabase.postgrest["courses"]
            .insert(dto) { select() }
            .decodeSingle<CourseDto>()
        println("DEBUG: curso insertado = ${response.id}")
            
        response.toDomain()
    }.onFailure { error ->
        println("ERROR en createCourse: ${error.message}")
        error.printStackTrace()
    }

    override suspend fun getTeacherCourses(): Result<List<Course>> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("No hay sesión activa")
        supabase.postgrest["courses"]
            .select { filter { eq("teacher_id", user.id) } }
            .decodeList<CourseDto>()
            .map { it.toDomain() }
    }

    override suspend fun getCourseByInviteCode(code: String): Result<Course?> = runCatching {
        supabase.postgrest["courses"]
            .select { filter { eq("invite_code", code.uppercase()) } }
            .decodeSingleOrNull<CourseDto>()
            ?.toDomain()
    }

    override suspend fun updateCourse(
        courseId: String,
        name: String,
        description: String?,
        subject: String?,
        grade: String?
    ): Result<Course> = runCatching {
        supabase.postgrest["courses"].update({
            set("name", name)
            set("description", description)
            set("subject", subject)
            set("grade", grade)
        }) { filter { eq("id", courseId) } }
        getCourseById(courseId).getOrThrow()
    }

    private suspend fun getCourseById(courseId: String): Result<Course> = runCatching {
        supabase.postgrest["courses"]
            .select { filter { eq("id", courseId) } }
            .decodeSingle<CourseDto>()
            .toDomain()
    }

    override suspend fun enrollStudent(courseId: String, studentName: String, studentRut: String?): Result<Unit> = runCatching {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("No hay sesión activa")
        
        val normalizedName = studentName
            .trim()                          // elimina espacios inicio/fin
            .replace(Regex("\\s+"), " ")     // colapsa espacios múltiples internos

        val dto = EnrollmentDto(
            courseId = courseId,
            studentName = normalizedName,
            parentId = user.id,
            studentRut = studentRut
        )
        
        AppLogger.d("EnrollStudent", "Auth UID: ${user.id} | Payload DTO: $dto")
        
        // Usamos .from() y aseguramos la ejecución
        val response = supabase.from("enrollments").insert(dto)
        Unit
    }.onFailure { error ->
        // LOG CRÍTICO PARA IDENTIFICAR LA CONSTRAINT REAL
        AppLogger.e("EnrollStudent", "RAW ERROR FROM POSTGRES: ${error.message}")
        
        val message = if (error.message?.contains("duplicate") == true ||
            error.message?.contains("unique") == true) {
            "Este alumno ya está registrado en el curso"
        } else {
            error.message ?: "Error al registrar alumno"
        }
        throw Exception(message)
    }

    private fun CourseDto.toDomain() = Course(
        id = id ?: "demo-course-${name.lowercase().replace(" ", "-")}",
        teacherId = teacherId ?: "",
        name = name,
        description = description,
        subject = subject,
        grade = grade,
        schoolName = schoolName,
        inviteCode = inviteCode ?: "",
        isActive = isActive,
        createdAt = createdAt ?: ""
    )
}
