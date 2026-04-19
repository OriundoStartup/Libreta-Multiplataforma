package com.tuapp.libreta.data.util

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
    @SerialName("parent_id") val parentId: String,
    @SerialName("attendance_percentage") val attendancePercentage: Double = 0.0
)

/**
 * DataSeeder proporciona datos de demostración tanto para el modo local como remoto.
 *
 * ESTRATEGIA APLICADA:
 * 1. IDs de Desarrollo: Se han reemplazado IDs genéricos ("s01", "clase-demo") por UUIDs válidos 
 *    (formato de10de10-...) para evitar errores 22P02 en PostgreSQL/Supabase.
 * 2. Aislamiento Local vs Remoto: El seeding en Supabase está desactivado por defecto mediante 
 *    [REMOTE_SEEDING_ENABLED]. Solo debe activarse explícitamente en desarrollo.
 * 3. Seguridad en Producción: El seeding local es seguro ya que solo afecta a la caché SQLite 
 *    del dispositivo del desarrollador/usuario demo.
 */
class DataSeeder(private val queries: LibretaAppQueries) {

    private val now = currentEpochMs()

    companion object {
        // IDs estáticos válidos para mantener consistencia y evitar errores de sintaxis UUID
        private const val DEMO_TEACHER_ID = "de10de10-0000-0000-0000-000000000001"
        private const val DEMO_PARENT_ID  = "de10de10-0000-0000-0000-000000000002"
        private const val DEMO_COURSE_ID  = "de10de10-0000-0000-0000-000000000003"

        // FLAG DE SEGURIDAD: Cambiar a 'true' solo si se desea poblar una instancia de Supabase nueva
        private const val REMOTE_SEEDING_ENABLED = false
    }

    /**
     * Pobla la base de datos local si está vacía. 
     * Ideal para pruebas offline y demostraciones rápidas.
     */
    fun seedIfEmpty() {
        queries.transaction {
            val count = queries.getStudentsByClass(DEMO_COURSE_ID).executeAsList().size
            if (count > 0) return@transaction
            
            seedLocalProfiles()
            seedLocalClass()
            seedLocalStudents()
            seedLocalAttendance()
        }
    }

    /**
     * Pobla Supabase con datos de prueba. 
     * Requiere que [REMOTE_SEEDING_ENABLED] sea true.
     */
    suspend fun seedSupabaseIfEmpty(supabase: SupabaseClient, teacherId: String) {
        if (!REMOTE_SEEDING_ENABLED) return

        // Obtener o crear el curso demo vinculado al profesor real
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

        // Evitar duplicados
        val studentsExist = runCatching {
            supabase.from("students").select { filter { eq("course_id", courseId) } }
                .decodeList<StudentInsert>().isNotEmpty()
        }.getOrElse { false }
        if (studentsExist) return

        // Insertar estudiantes (Supabase generará sus UUIDs reales)
        val students = studentData.map { (_, name) ->
            mapOf("full_name" to name, "course_id" to courseId, "parent_id" to teacherId)
        }
        supabase.from("students").insert(students)

        // Insertar asistencia vinculada a los nuevos UUIDs generados por Supabase
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
        queries.insertOrReplaceProfile(DEMO_TEACHER_ID, "TEACHER", "Carlos Fuentes (Demo)",
            SyncStatus.SYNCED.name, now, now)
        queries.insertOrReplaceProfile(DEMO_PARENT_ID, "PARENT", "Ana Martínez (Demo)",
            SyncStatus.SYNCED.name, now, now)
    }

    private fun seedLocalClass() =
        queries.insertOrReplaceClass(DEMO_COURSE_ID, "1BA", "4° Básico A (Demo)", DEMO_TEACHER_ID,
            SyncStatus.SYNCED.name, now, now)

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
            queries.insertOrReplaceStudent(id, name, DEMO_COURSE_ID, DEMO_PARENT_ID,
                SyncStatus.SYNCED.name, now, now)
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
                // Generar un ID de asistencia único y válido (36 caracteres)
                val attendanceId = studentId.substring(0, 31) + "a" + i + studentId.takeLast(3)
                queries.insertOrReplaceAttendance(attendanceId, studentId, dateStr,
                    if (present) AttendanceStatus.PRESENT.name else AttendanceStatus.ABSENT.name,
                    null, SyncStatus.SYNCED.name, now, now)
            }
        }
    }
}
