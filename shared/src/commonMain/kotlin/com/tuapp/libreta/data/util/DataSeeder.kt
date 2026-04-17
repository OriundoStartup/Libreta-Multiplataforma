package com.tuapp.libreta.data.util

import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.SyncStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class DataSeeder(private val queries: LibretaAppQueries) {

    private val tz  = TimeZone.currentSystemDefault()
    private val now = Clock.System.now().toEpochMilliseconds()

    fun seedIfEmpty() {
        queries.transaction {
            val count = queries.getStudentsByClass("clase-demo").executeAsList().size
            if (count > 0) return@transaction

            seedProfiles()
            seedClass()
            val students = seedStudents()
            seedAttendance(students)
        }
    }

    private fun seedProfiles() {
        queries.insertOrReplaceProfile("teacher-demo", "TEACHER", "Carlos", "Fuentes",
            "profesor@libreta.cl", SyncStatus.SYNCED.name, now, now)
        queries.insertOrReplaceProfile("parent-demo", "PARENT", "Ana", "Martínez",
            "apoderado@libreta.cl", SyncStatus.SYNCED.name, now, now)
    }

    private fun seedClass() {
        queries.insertOrReplaceClass("clase-demo", "1BA", "4° Básico A", "teacher-demo",
            SyncStatus.SYNCED.name, now, now)
    }

    private val studentData = listOf(
        Triple("s01", "Sofía",     "Martínez"),
        Triple("s02", "Mateo",     "González"),
        Triple("s03", "Valentina", "López"),
        Triple("s04", "Sebastián", "Rodríguez"),
        Triple("s05", "Isabella",  "Pérez"),
        Triple("s06", "Benjamín",  "Sánchez"),
        Triple("s07", "Camila",    "Ramírez"),
        Triple("s08", "Lucas",     "Torres"),
        Triple("s09", "Martina",   "Flores"),
        Triple("s10", "Diego",     "Vargas")
    )

    private fun seedStudents(): List<String> {
        studentData.forEach { (id, first, last) ->
            queries.insertOrReplaceStudent(id, "1${id.drop(1)}.000.000-${id.last()}",
                first, last, "parent-demo", "clase-demo", SyncStatus.SYNCED.name, now, now)
        }
        return studentData.map { it.first }
    }

    private val attendancePattern = mapOf(
        "s01" to listOf(true,  true,  true,  true,  true),
        "s02" to listOf(true,  true,  false, true,  true),
        "s03" to listOf(true,  true,  true,  false, true),
        "s04" to listOf(false, true,  true,  true,  false),
        "s05" to listOf(true,  true,  true,  true,  true),
        "s06" to listOf(true,  false, false, true,  true),
        "s07" to listOf(true,  true,  true,  true,  false),
        "s08" to listOf(false, false, true,  true,  true),
        "s09" to listOf(true,  true,  true,  true,  true),
        "s10" to listOf(true,  true,  false, false, true)
    )

    private fun seedAttendance(studentIds: List<String>) {
        val today = Clock.System.now().toLocalDateTime(tz).date
        studentIds.forEach { studentId ->
            val pattern = attendancePattern[studentId] ?: List(5) { true }
            pattern.forEachIndexed { dayOffset, isPresent ->
                val date    = today.minus(4 - dayOffset, DateTimeUnit.DAY)
                val epochMs = date.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds()
                val status  = if (isPresent) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                queries.insertOrReplaceAttendance("att-$studentId-$dayOffset", studentId,
                    epochMs, status.name, SyncStatus.SYNCED.name, now, now)
            }
        }
    }
}
