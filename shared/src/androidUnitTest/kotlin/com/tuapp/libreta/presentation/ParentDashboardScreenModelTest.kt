package com.tuapp.libreta.presentation

import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import io.github.jan.supabase.auth.user.UserInfo
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ParentDashboardScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocking minimal dependencies manually
    private lateinit var studentRepo: StudentRepository
    private lateinit var attendanceRepo: AttendanceRepository
    private lateinit var messageRepo: MessageRepository
    private lateinit var coursesRepo: CoursesRepository
    private lateinit var authService: SupabaseAuthService

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        studentRepo = mockk(relaxed = true)
        attendanceRepo = mockk(relaxed = true)
        messageRepo = mockk(relaxed = true)
        coursesRepo = mockk(relaxed = true)
        authService = mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `load - No Students - shows NoStudents state`() = runTest {
        // GIVEN: Usuario autenticado pero sin hijos vinculados
        val userId = com.tuapp.libreta.util.TestDataFactory.randomUuid()
        coEvery { authService.currentUserId() } returns userId
        coEvery { studentRepo.getStudentsByParent(userId) } returns flowOf(emptyList())

        val model = ParentDashboardScreenModel(studentRepo, attendanceRepo, messageRepo, coursesRepo, authService)
        advanceUntilIdle()

        // THEN: Se muestra el estado de NoStudents
        assertTrue(model.state.value.uiState is ParentDashboardUiState.NoStudents)
    }

    @Test
    fun `load - Success - shows students list from Factory`() = runTest {
        // GIVEN: 2 alumnos generados dinámicamente
        val userId = com.tuapp.libreta.util.TestDataFactory.randomUuid()
        val students = listOf(
            com.tuapp.libreta.util.TestDataFactory.makeStudent(parentId = userId),
            com.tuapp.libreta.util.TestDataFactory.makeStudent(parentId = userId)
        )
        
        coEvery { authService.currentUserId() } returns userId
        coEvery { studentRepo.getStudentsByParent(userId) } returns flowOf(students)

        val model = ParentDashboardScreenModel(studentRepo, attendanceRepo, messageRepo, coursesRepo, authService)
        advanceUntilIdle()

        // THEN: El estado de éxito debe contener a los 2 alumnos
        val state = model.state.value.uiState
        assertTrue(state is ParentDashboardUiState.Success)
        assertEquals(2, state.students.size)
    }
}
