package com.tuapp.libreta

import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.presentation.RoleSelectionScreenModel
import com.tuapp.libreta.presentation.RoleSelectionUiState
import com.tuapp.libreta.util.TestDataFactory
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Test de Integración: Valida el flujo completo de Onboarding (Registro -> Rol -> Dashboard).
 * Este test verifica la "Regla de Oro" de navegación centralizada y persistencia atómica.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    
    // Mocks de dependencias
    private val authService = mockk<SupabaseAuthService>(relaxed = true)
    private val coursesRepo = mockk<CoursesRepository>(relaxed = true)
    private val studentRepo = mockk<StudentRepository>(relaxed = true)
    private val syncManager = mockk<com.tuapp.libreta.data.sync.SyncManager>(relaxed = true)

    private lateinit var model: RoleSelectionScreenModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        model = RoleSelectionScreenModel(authService, coursesRepo, studentRepo, syncManager)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `full parent onboarding flow - Success`() = runTest {
        // GIVEN: Un usuario autenticado pero sin rol (recién registrado)
        val userId = TestDataFactory.randomUuid()
        val course = TestDataFactory.makeCourse(inviteCode = "PARENT123")
        
        coEvery { authService.currentUserId() } returns userId
        coEvery { coursesRepo.getCourseByInviteCode("PARENT123") } returns Result.success(course)
        coEvery { coursesRepo.enrollStudent(any(), any(), any()) } returns Result.success(Unit)

        // WHEN: El usuario confirma el rol de apoderado e inscribe a su hijo
        model.confirmRole(UserRole.PARENT, "PARENT123", "Juanito Perez")
        advanceUntilIdle()

        // THEN: Se verifican las 3 acciones atómicas del Match Backend
        coVerify(exactly = 1) { authService.updateRole(UserRole.PARENT) }
        coVerify(exactly = 1) { coursesRepo.enrollStudent(course.id, "Juanito Perez") }
        coVerify(exactly = 1) { syncManager.syncAll() }

        // THEN: El estado de la UI debe ser Success (Notificando al Guardian de App.kt)
        val state = model.state.value
        assertTrue(state is RoleSelectionUiState.Success)
        assertEquals(UserRole.PARENT, (state as RoleSelectionUiState.Success).role)
    }

    @Test
    fun `parent onboarding flow - Failure - invalid code`() = runTest {
        // GIVEN: Código inválido
        coEvery { authService.currentUserId() } returns TestDataFactory.randomUuid()
        coEvery { coursesRepo.getCourseByInviteCode("BAD_CODE") } returns Result.success(null)

        // WHEN: Intenta confirmar
        model.confirmRole(UserRole.PARENT, "BAD_CODE", "Juanito")
        advanceUntilIdle()

        // THEN: No se actualiza el rol y se muestra error
        coVerify(exactly = 0) { authService.updateRole(any()) }
        assertTrue(model.state.value is RoleSelectionUiState.Error)
    }
}
