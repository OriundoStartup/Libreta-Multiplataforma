package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradeUseCasesTest {

    private val studentId = UuidString("00000000-0000-0000-0000-000000000001")
    private val courseId = UuidString("00000000-0000-0000-0000-0000000000c1")

    private val fakeGradeRepo = object : GradeRepository {
        var lastSavedGrade: Grade? = null
        override fun getByStudent(studentId: UuidString): Flow<List<Grade>> = flowOf(
            listOf(
                Grade(studentId = studentId, courseId = courseId, title = "Test 1", score = 6.0, weight = 0.5, subject = "Math", date = 1000L),
                Grade(studentId = studentId, courseId = courseId, title = "Test 2", score = 4.0, weight = 0.5, subject = "Math", date = 2000L),
                Grade(studentId = studentId, courseId = courseId, title = "Test 3", score = 7.0, weight = 1.0, subject = "History", date = 3000L)
            )
        )
        override fun getByCourse(courseId: UuidString): Flow<List<Grade>> = flowOf(emptyList())
        override suspend fun save(grade: Grade) { lastSavedGrade = grade }
        override suspend fun delete(id: UuidString) {}
    }

    @Test
    fun testGetStudentGradesCalculatesWeightedAverage() = runTest {
        val useCase = GetStudentGradesUseCase(fakeGradeRepo)
        val result = useCase(studentId).first()

        assertEquals(2, result.size)
        
        val math = result.find { it.subject == "Math" }!!
        // (6.0 * 0.5 + 4.0 * 0.5) / 1.0 = 5.0
        assertEquals(5.0, math.average)
        assertEquals(2, math.grades.size)

        val history = result.find { it.subject == "History" }!!
        assertEquals(7.0, history.average)
    }

    @Test
    fun testSaveGradeValidatesScoreRange() = runTest {
        val useCase = SaveGradeUseCase(fakeGradeRepo)
        
        // Test high boundary
        val tooHigh = Grade(studentId = studentId, courseId = courseId, title = "Fail", score = 7.1, subject = "Math")
        assertTrue(useCase(tooHigh).isFailure)

        // Test low boundary
        val tooLow = Grade(studentId = studentId, courseId = courseId, title = "Fail", score = 0.9, subject = "Math")
        assertTrue(useCase(tooLow).isFailure)

        // Test valid
        val valid = Grade(studentId = studentId, courseId = courseId, title = "Pass", score = 4.0, subject = "Math")
        assertTrue(useCase(valid).isSuccess)
    }

    @Test
    fun testSaveGradeValidatesPositiveWeight() = runTest {
        val useCase = SaveGradeUseCase(fakeGradeRepo)
        
        val invalidWeight = Grade(studentId = studentId, courseId = courseId, title = "Fail", score = 5.0, weight = 0.0, subject = "Math")
        val result = useCase(invalidWeight)
        
        assertTrue(result.isFailure)
        assertEquals("El peso de la evaluación debe ser mayor a 0", result.exceptionOrNull()?.message)
    }

    @Test
    fun testSaveGradeValidatesNonEmptyTitle() = runTest {
        val useCase = SaveGradeUseCase(fakeGradeRepo)
        
        val emptyTitle = Grade(studentId = studentId, courseId = courseId, title = "", score = 5.0, subject = "Math")
        val result = useCase(emptyTitle)
        
        assertTrue(result.isFailure)
        assertEquals("El título de la evaluación no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun testDeleteGradeCallsRepository() = runTest {
        val useCase = DeleteGradeUseCase(fakeGradeRepo)
        val id = UuidString.random()
        useCase(id)
        // Success if no exception
    }
}
