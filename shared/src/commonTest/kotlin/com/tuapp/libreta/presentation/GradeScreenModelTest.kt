package com.tuapp.libreta.presentation

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.repository.GradeRepository
import com.tuapp.libreta.domain.usecase.DeleteGradeUseCase
import com.tuapp.libreta.domain.usecase.GetStudentGradesUseCase
import com.tuapp.libreta.domain.usecase.SaveGradeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GradeScreenModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val studentId = "00000000-0000-0000-0000-000000000001"
    private val courseId = "00000000-0000-0000-0000-0000000000c1"

    private var lastSavedGrade: Grade? = null
    private var lastDeletedId: UuidString? = null
    private var flowToReturn: Flow<List<Grade>> = flowOf(emptyList())

    private val fakeGradeRepo = object : GradeRepository {
        override fun getByStudent(studentId: UuidString): Flow<List<Grade>> = flowToReturn
        override fun getByCourse(courseId: UuidString): Flow<List<Grade>> = flowOf(emptyList())
        override suspend fun save(grade: Grade) {
            lastSavedGrade = grade
        }
        override suspend fun delete(id: UuidString) {
            lastDeletedId = id
        }
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        lastSavedGrade = null
        lastDeletedId = null
        flowToReturn = flowOf(emptyList())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadUpdatesStateToSuccess() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        model.load(studentId)
        
        // En UnconfinedTestDispatcher, las emisiones deberían ser casi instantáneas
        // pero vamos a forzar la ejecución
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Success, "Expected Success but was $state")
    }

    @Test
    fun testAddGradeWithInvalidDataUpdatesStateToError() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        // Forzamos un estado previo
        model.load(studentId) 
        advanceUntilIdle()
        
        // Add invalid grade (score > 7)
        model.addGrade(studentId, courseId, "Test", 8.0, "Math")
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Error, "Expected Error but was $state")
        assertEquals("La nota debe estar entre 1.0 y 7.0", (state as GradeUiState.Error).message)
    }

    @Test
    fun testLoadFailureUpdatesStateToError() = runTest {
        val errorMessage = "Network Error"
        flowToReturn = flow { throw Exception(errorMessage) }
        
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        model.load(studentId)
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Error, "Expected Error but was $state")
        assertEquals(errorMessage, (state as GradeUiState.Error).message)
    }

    @Test
    fun testAddGradeWithEmptyTitleUpdatesStateToError() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        model.addGrade(studentId, courseId, "", 5.0, "Math")
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Error, "Expected Error but was $state")
        assertEquals("El título de la evaluación no puede estar vacío", (state as GradeUiState.Error).message)
    }

    @Test
    fun testAddGradeWithNegativeWeightUpdatesStateToError() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        model.addGrade(studentId, courseId, "Test", 5.0, "Math", weight = -1.0)
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Error, "Expected Error but was $state")
        assertEquals("El peso de la evaluación debe ser mayor a 0", (state as GradeUiState.Error).message)
    }

    @Test
    fun testAddGradeValidCallsRepository() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        val testTitle = "Final Exam"
        val testScore = 6.5
        model.addGrade(studentId, courseId, testTitle, testScore, "History")
        advanceUntilIdle()
        
        assertNotNull(lastSavedGrade)
        assertEquals(testTitle, lastSavedGrade?.title)
        assertEquals(testScore, lastSavedGrade?.score)
    }

    @Test
    fun testDeleteGradeCallsRepository() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        val idToDelete = UuidString("00000000-0000-0000-0000-000000000009")
        model.deleteGrade(idToDelete)
        advanceUntilIdle()
        
        assertEquals(idToDelete, lastDeletedId)
    }

    @Test
    fun testUpdateGradeValidCallsRepository() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        val gradeToUpdate = Grade(
            id = UuidString("00000000-0000-0000-0000-000000000002"),
            studentId = UuidString(studentId),
            courseId = UuidString(courseId),
            title = "Updated Title",
            score = 7.0,
            subject = "Math",
            weight = 1.0,
            date = 123456789L
        )
        
        model.updateGrade(gradeToUpdate)
        advanceUntilIdle()
        
        assertEquals(gradeToUpdate, lastSavedGrade)
    }

    @Test
    fun testUpdateGradeInvalidUpdatesStateToError() = runTest {
        val model = GradeScreenModel(
            GetStudentGradesUseCase(fakeGradeRepo),
            SaveGradeUseCase(fakeGradeRepo),
            DeleteGradeUseCase(fakeGradeRepo),
            this
        )
        
        val invalidGrade = Grade(
            studentId = UuidString(studentId),
            courseId = UuidString(courseId),
            title = "Invalid",
            score = 10.0, // Invalid score > 7.0
            subject = "Math",
            weight = 1.0,
            date = 123456789L
        )
        
        model.updateGrade(invalidGrade)
        advanceUntilIdle()
        
        val state = model.state.value
        assertTrue(state is GradeUiState.Error, "Expected Error but was $state")
        assertEquals("La nota debe estar entre 1.0 y 7.0", (state as GradeUiState.Error).message)
    }
}
