package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetConsolidatedReportUseCaseTest {

    private val fakeStudentRepo = object : StudentRepository {
        override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> = flowOf(
            listOf(
                Student(id = UuidString("00000000-0000-0000-0000-000000000001"), fullName = "Alice", courseId = classId, parentId = UuidString("00000000-0000-0000-0000-000000000011")),
                Student(id = UuidString("00000000-0000-0000-0000-000000000002"), fullName = "Bob", courseId = classId, parentId = UuidString("00000000-0000-0000-0000-000000000011"))
            )
        )
        override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> = flowOf(emptyList())
        override suspend fun saveStudent(student: Student) {}
        override suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit> = Result.success(Unit)
        override suspend fun deleteStudent(id: UuidString) {}
    }

    private val fakeAttendanceRepo = object : AttendanceRepository {
        override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> = flowOf(emptyList())
        override fun getByCourse(courseId: UuidString): Flow<List<Attendance>> = flowOf(
            listOf(
                Attendance(id = UuidString("00000000-0000-0000-0000-0000000000a1"), studentId = UuidString("00000000-0000-0000-0000-000000000001"), date = "2024-01-01", status = AttendanceStatus.PRESENT),
                Attendance(id = UuidString("00000000-0000-0000-0000-0000000000a2"), studentId = UuidString("00000000-0000-0000-0000-000000000002"), date = "2024-01-01", status = AttendanceStatus.ABSENT)
            )
        )
        override suspend fun save(attendance: Attendance) {}
        override suspend fun delete(id: UuidString) {}
    }

    private val useCase = GetConsolidatedReportUseCase(fakeStudentRepo, fakeAttendanceRepo)

    @Test
    fun `execute returns correct matrix`() = runTest {
        val courseId = UuidString("00000000-0000-0000-0000-0000000000c1")
        val report = useCase.execute(courseId)

        assertEquals(2, report.students.size)
        assertEquals(1, report.dates.size)
        assertEquals("2024-01-01", report.dates[0])
        
        assertEquals(AttendanceStatus.PRESENT, report.matrix["Alice"]?.get("2024-01-01"))
        assertEquals(AttendanceStatus.ABSENT, report.matrix["Bob"]?.get("2024-01-01"))
    }
}
