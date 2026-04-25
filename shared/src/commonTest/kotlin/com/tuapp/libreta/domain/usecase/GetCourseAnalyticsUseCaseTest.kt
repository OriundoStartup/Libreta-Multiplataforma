package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.test.FakeAttendanceRepository
import com.tuapp.libreta.test.FakeStudentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCourseAnalyticsUseCaseTest {

    private val studentRepo    = FakeStudentRepository()
    private val attendanceRepo = FakeAttendanceRepository()
    private val useCase        = GetCourseAnalyticsUseCase(studentRepo, attendanceRepo)

    private fun student(id: String) = Student(id, "rut-$id", "Nombre", "Apellido", "p1", "c1")

    private fun attendance(id: String, studentId: String, status: AttendanceStatus) =
        Attendance(id, studentId, 0L, status)

    @Test
    fun `attendance percent is calculated correctly`() = runTest {
        // 10 students, 8 present, 2 absent → 80%
        val students = (1..10).map { student("s$it") }
        studentRepo.students.value = students

        val records = (1..8).map { attendance("a$it", "s$it", AttendanceStatus.PRESENT) } +
                      (9..10).map { attendance("a$it", "s$it", AttendanceStatus.ABSENT) }
        attendanceRepo.records.value = records

        val analytics = useCase("c1").first()

        assertEquals(8, analytics.presentCount)
        assertEquals(2, analytics.absentCount)
        assertTrue(analytics.overallAttendancePercent in 0.79f..0.81f)
    }

    @Test
    fun `students with less than 75 percent attendance are at risk`() = runTest {
        val students = listOf(student("s1"), student("s2"))
        studentRepo.students.value = students

        // s1: 4/4 = 100% → safe
        // s2: 2/4 = 50%  → at risk
        attendanceRepo.records.value = listOf(
            attendance("a1", "s1", AttendanceStatus.PRESENT),
            attendance("a2", "s1", AttendanceStatus.PRESENT),
            attendance("a3", "s1", AttendanceStatus.PRESENT),
            attendance("a4", "s1", AttendanceStatus.PRESENT),
            attendance("a5", "s2", AttendanceStatus.PRESENT),
            attendance("a6", "s2", AttendanceStatus.PRESENT),
            attendance("a7", "s2", AttendanceStatus.ABSENT),
            attendance("a8", "s2", AttendanceStatus.ABSENT)
        )

        val analytics = useCase("c1").first()

        assertEquals(1, analytics.atRisk.size)
        assertEquals("s2", analytics.atRisk.first().student.id)
    }

    @Test
    fun `empty class returns zero analytics`() = runTest {
        studentRepo.students.value = emptyList()

        val analytics = useCase("c1").first()

        assertEquals(0, analytics.totalStudents)
        assertEquals(0, analytics.presentCount)
        assertTrue(analytics.atRisk.isEmpty())
        assertTrue(analytics.topAbsentees.isEmpty())
    }

    @Test
    fun `top absentees are sorted descending by absence count`() = runTest {
        val students = listOf(student("s1"), student("s2"), student("s3"))
        studentRepo.students.value = students

        // s1: 1 absence, s2: 3 absences, s3: 2 absences → order: s2, s3, s1
        attendanceRepo.records.value = listOf(
            attendance("a1", "s1", AttendanceStatus.ABSENT),
            attendance("a2", "s2", AttendanceStatus.ABSENT),
            attendance("a3", "s2", AttendanceStatus.ABSENT),
            attendance("a4", "s2", AttendanceStatus.ABSENT),
            attendance("a5", "s3", AttendanceStatus.ABSENT),
            attendance("a6", "s3", AttendanceStatus.ABSENT)
        )

        val analytics = useCase("c1").first()
        val ids = analytics.topAbsentees.map { it.student.id }

        assertEquals("s2", ids[0])
        assertEquals("s3", ids[1])
        assertEquals("s1", ids[2])
    }
}
