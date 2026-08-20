package com.tuapp.libreta.data.util

import com.tuapp.libreta.data.util.DataSeeder.Companion.REMOTE_SEEDING_ENABLED
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.SyncStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CourseInsert(val id: String = "", val name: String, @SerialName("teacher_id") val teacherId: String = "")

@Serializable
private data class StudentInsert(
    val id: String = "",
    @SerialName("full_name") val fullName: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String
)

class DataSeeder(private val queries: LibretaAppQueries) {

    private val now = currentEpochMs()

    companion object {
        private const val DEMO_TEACHER_ID = "de10de10-0000-0000-0000-000000000001"
        private const val DEMO_PARENT_ID  = "de10de10-0000-0000-0000-000000000002"
        private const val DEMO_COURSE_ID  = "de10de10-0000-0000-0000-000000000003"
        private const val REMOTE_SEEDING_ENABLED = false
    }

    suspend fun seedIfEmpty() {
        queries.transaction {
            val count = queries.getStudentsByCourse(DEMO_COURSE_ID).executeAsList().size
            if (count > 0) return@transaction
            
            seedLocalProfiles()
            seedLocalCourse()
            seedLocalStudents()
            seedLocalAttendance()
        }
    }

    suspend fun seedSupabaseIfEmpty(supabase: SupabaseClient, teacherId: String) {
        if (!REMOTE_SEEDING_ENABLED) return

        val existing = runCatching {
            supabase.from("courses").select { filter { eq("teacher_id", teacherId) } }
                .decodeList<CourseInsert>()
        }.getOrElse { emptyList() }

        val courseId = if (existing.isNotEmpty()) {
            existing.first().id
        } else {
            supabase.from("courses")
                .insert(mapOf("name" to "4° Básico A (Demo)", "teacher_id" to teacherId)) { select() }
                .decodeSingle<CourseInsert>().id
        }

        val studentsExist = runCatching {
            supabase.from("students").select { filter { eq("course_id", courseId) } }
                .decodeList<StudentInsert>().isNotEmpty()
        }.getOrElse { false }
        if (studentsExist) return

        val students = studentData.map { (_, name) ->
            mapOf("full_name" to name, "course_id" to courseId, "parent_id" to teacherId)
        }
        supabase.from("students").insert(students)

        val insertedStudents = supabase.from("students")
            .select { filter { eq("course_id", courseId) } }
            .decodeList<StudentInsert>()

        val dayMs = 86_400_000L
        val attendance = insertedStudents.flatMapIndexed { idx, student ->
            val pat = pattern.values.toList().getOrElse(idx) { List(5) { true } }
            pat.mapIndexed { i, present ->
                mapOf(
                    "student_id" to student.id,
                    "date"       to epochMsToIso(now - (4 - i) * dayMs).take(10),
                    "status"     to if (present) AttendanceStatus.PRESENT.name else AttendanceStatus.ABSENT.name
                )
            }
        }
        if (attendance.isNotEmpty()) supabase.from("attendance").insert(attendance)
    }

    private fun seedLocalProfiles() {
        queries.insertOrReplaceProfile(
            id = DEMO_TEACHER_ID,
            full_name = "Carlos Fuentes (Demo)",
            role = "TEACHER",
            server_version = 1,
            is_deleted = 0,
            sync_status = SyncStatus.SYNCED.name,
            created_at = now,
            updated_at = now
        )
        queries.insertOrReplaceProfile(
            id = DEMO_PARENT_ID,
            full_name = "Ana Martínez (Demo)",
            role = "PARENT",
            server_version = 1,
            is_deleted = 0,
            sync_status = SyncStatus.SYNCED.name,
            created_at = now,
            updated_at = now
        )
    }

    private fun seedLocalCourse() =
        queries.insertOrReplaceCourse(
            id = DEMO_COURSE_ID,
            name = "4° Básico A (Demo)",
            description = null,
            subject = null,
            grade = null,
            section = null,
            teacher_id = DEMO_TEACHER_ID,
            school_id = null,
            invite_code = null,
            is_active = 1,
            server_version = 1,
            is_deleted = 0,
            sync_status = SyncStatus.SYNCED.name,
            created_at = now,
            updated_at = now
        )

    private val studentData = listOf(
        "de10de10-0000-0000-0000-000000000101" to "Sofía Martínez",
        "de10de10-0000-0000-0000-000000000102" to "Mateo González",
        "de10de10-0000-0000-0000-000000000103" to "Valentina López",
        "de10de10-0000-0000-0000-000000000104" to "Sebastián Rodríguez",
        "de10de10-0000-0000-0000-000000000105" to "Isabella Pérez",
        "de10de10-0000-0000-0000-000000000106" to "Benjamín Sánchez",
        "de10de10-0000-0000-0000-000000000107" to "Camila Ramírez",
        "de10de10-0000-0000-0000-000000000108" to "Lucas Torres",
        "de10de10-0000-0000-0000-000000000109" to "Martina Flores",
        "de10de10-0000-0000-0000-000000000110" to "Diego Vargas"
    )

    private fun seedLocalStudents() {
        studentData.forEach { (id, name) ->
            queries.insertOrReplaceStudent(
                id = id,
                full_name = name,
                student_rut = null,
                course_id = DEMO_COURSE_ID,
                parent_id = DEMO_PARENT_ID,
                server_version = 1,
                is_deleted = 0,
                sync_status = SyncStatus.SYNCED.name,
                created_at = now,
                updated_at = now
            )
        }
    }

    private val pattern = mapOf(
        "de10de10-0000-0000-0000-000000000101" to listOf(true,true,true,true,true),
        "de10de10-0000-0000-0000-000000000102" to listOf(true,true,false,true,true),
        "de10de10-0000-0000-0000-000000000103" to listOf(true,true,true,false,true),
        "de10de10-0000-0000-0000-000000000104" to listOf(false,true,true,true,false),
        "de10de10-0000-0000-0000-000000000105" to listOf(true,true,true,true,true),
        "de10de10-0000-0000-0000-000000000106" to listOf(true,false,false,true,true),
        "de10de10-0000-0000-0000-000000000107" to listOf(true,true,true,true,false),
        "de10de10-0000-0000-0000-000000000108" to listOf(false,false,true,true,true),
        "de10de10-0000-0000-0000-000000000109" to listOf(true,true,true,true,true),
        "de10de10-0000-0000-0000-000000000110" to listOf(true,true,false,false,true)
    )

    private fun seedLocalAttendance() {
        val dayMs = 86_400_000L
        studentData.forEach { (studentId, _) ->
            (pattern[studentId] ?: List(5) { true }).forEachIndexed { i, present ->
                val dateStr = epochMsToIso(now - (4 - i) * dayMs).take(10)
                val attendanceId = studentId.substring(0, 31) + "a" + i + studentId.takeLast(3)
                queries.insertOrReplaceAttendance(
                    id = attendanceId,
                    student_id = studentId,
                    date = dateStr,
                    status = if (present) AttendanceStatus.PRESENT.name else AttendanceStatus.ABSENT.name,
                    server_version = 1,
                    is_deleted = 0,
                    sync_status = SyncStatus.SYNCED.name,
                    created_at = now,
                    updated_at = now
                )
            }
        }
    }
}