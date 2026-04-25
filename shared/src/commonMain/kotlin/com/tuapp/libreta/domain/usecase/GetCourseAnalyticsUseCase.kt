package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class StudentAbsences(val student: Student, val absenceCount: Int)

data class DailyAttendance(val label: String, val presentCount: Int, val totalCount: Int) {
    val percent: Float get() = if (totalCount == 0) 0f else presentCount / totalCount.toFloat()
}

data class CourseAnalytics(
    val totalStudents: Int,
    val overallAttendancePercent: Float,
    val presentCount: Int,
    val absentCount: Int,
    val justifiedCount: Int,
    val topAbsentees: List<StudentAbsences>,
    val atRisk: List<StudentAbsences>,
    val last5Days: List<DailyAttendance>
)

class GetCourseAnalyticsUseCase(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository
) {
    operator fun invoke(classId: UuidString): Flow<CourseAnalytics> =
        studentRepo.getStudentsByClass(classId).map { students ->
            // Carga la asistencia de todos los alumnos en paralelo (evita N+1 secuencial)
            val allAttendance = coroutineScope {
                students.map { student ->
                    async { attendanceRepo.getByStudent(student.id).first() }
                }.flatMap { it.await() }
            }

            val presentCount   = allAttendance.count { it.status == AttendanceStatus.PRESENT }
            val absentCount    = allAttendance.count { it.status == AttendanceStatus.ABSENT }
            val justifiedCount = allAttendance.count { it.status == AttendanceStatus.LATE }
            val total          = allAttendance.size.coerceAtLeast(1)

            val absencesByStudent = students.map { student ->
                val absences = allAttendance.count {
                    it.studentId == student.id && it.status == AttendanceStatus.ABSENT
                }
                StudentAbsences(student, absences)
            }

            val atRisk = absencesByStudent.filter { sa ->
                val studentTotal   = allAttendance.count { it.studentId == sa.student.id }.coerceAtLeast(1)
                val studentPresent = allAttendance.count {
                    it.studentId == sa.student.id && it.status == AttendanceStatus.PRESENT
                }
                (studentPresent / studentTotal.toFloat()) < 0.75f
            }

            // Últimos 5 días con datos REALES agrupados por fecha
            val last5Days = allAttendance
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .take(5)
                .map { (date, records) ->
                    val dayPresent = records.count { it.status == AttendanceStatus.PRESENT }
                    val dayTotal   = records.size.coerceAtLeast(1)
                    val label      = date.takeLast(5)   // muestra MM-DD del ISO date
                    DailyAttendance(label, dayPresent, dayTotal)
                }
                .reversed()   // orden cronológico ascendente para el gráfico

            CourseAnalytics(
                totalStudents            = students.size,
                overallAttendancePercent = presentCount / total.toFloat(),
                presentCount             = presentCount,
                absentCount              = absentCount,
                justifiedCount           = justifiedCount,
                topAbsentees             = absencesByStudent.sortedByDescending { it.absenceCount }.take(3),
                atRisk                   = atRisk,
                last5Days                = last5Days
            )
        }
}
