package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.first

data class AttendanceReport(
    val students: List<String>,
    val dates: List<String>,
    val matrix: Map<String, Map<String, AttendanceStatus?>> // StudentName -> (Date -> Status)
)

class GetConsolidatedReportUseCase(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository
) {
    suspend fun execute(courseId: UuidString): AttendanceReport {
        val students = studentRepo.getStudentsByClass(courseId).first().sortedBy { it.fullName }
        val attendance = attendanceRepo.getByCourse(courseId).first()
        
        val dates = attendance.map { it.date }.distinct().sortedDescending().take(31).sorted()
        
        val matrix = students.associate { student ->
            student.fullName to dates.associateWith { date ->
                attendance.find { it.studentId == student.id && it.date == date }?.status
            }
        }
        
        return AttendanceReport(
            students = students.map { it.fullName },
            dates = dates,
            matrix = matrix
        )
    }
}
