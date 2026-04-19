package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.CourseAssignmentDto
import com.tuapp.libreta.data.remote.dto.SchoolDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.School
import com.tuapp.libreta.domain.repository.SchoolRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseSchoolRepository(private val supabase: SupabaseClient) : SchoolRepository {

    override fun getByTeacher(teacherId: UuidString): Flow<List<School>> = flow {
        val assignments = supabase.from("course_assignments")
            .select { filter { eq("teacher_id", teacherId.value) } }
            .decodeList<CourseAssignmentDto>()

        val schoolIds = assignments.map { it.schoolId }.distinct()
        if (schoolIds.isEmpty()) { emit(emptyList()); return@flow }

        val schools = supabase.from("schools")
            .select { filter { isIn("id", schoolIds) } }
            .decodeList<SchoolDto>()

        emit(schools.map { it.toDomain() })
    }
}
