package com.tuapp.libreta.data.sync

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.db.AttendanceEntity
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.SyncStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.PostgrestBuilder
import io.github.jan.supabase.postgrest.postgrest
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private val queries = mockk<LibretaAppQueries>(relaxed = true)
    private val supabase = mockk<SupabaseClient>(relaxed = true)
    private val postgrest = mockk<Postgrest>(relaxed = true)
    private val builder = mockk<PostgrestBuilder>(relaxed = true)

    private lateinit var syncManager: SyncManager

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)
        syncManager = SyncManager(queries, supabase)

        // Configuración de Mocks para Supabase
        every { supabase.postgrest } returns postgrest
        every { postgrest[any()] } returns builder
        
        // Mock de extensiones estáticas de SQLDelight
        mockkStatic("app.cash.sqldelight.coroutines.FlowQueryExtensionsKt")
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `syncAttendance - Success - realiza bulk upsert y marca como sincronizado`() = runTest {
        // GIVEN: Registros dinámicos usando el Factory (evita hardcoding)
        val pendingEntities = listOf(
            com.tuapp.libreta.util.TestDataFactory.makeAttendanceEntity(status = "PRESENT"),
            com.tuapp.libreta.util.TestDataFactory.makeAttendanceEntity(status = "LATE")
        )
        
        setupMockQuery(queries.getUnsyncedAttendanceEntities(), pendingEntities)

        // WHEN
        syncManager.syncAll()

        // THEN: Se llamó a upsert una sola vez con la lista de DTOs
        coVerify(exactly = 1) { 
            builder.upsert(match<List<Map<String, Any>>> { it.size == 2 }, any(), any(), any(), any()) 
        }
        
        // THEN: Se marcó localmente como SYNCED para cada registro
        verify(exactly = 2) { 
            queries.insertOrReplaceAttendance(any(), any(), any(), any(), any(), any(), SyncStatus.SYNCED.name, any(), any()) 
        }
    }

    @Test
    fun `syncAttendance - Network Failure - no marca como sincronizado localmente`() = runTest {
        // GIVEN: Registros pendientes
        val pendingEntities = listOf(
            AttendanceEntity("id-1", "student-1", "2024-03-20", "PRESENT", 1, 0, SyncStatus.PENDING_INSERT.name, 1000, 1000)
        )
        setupMockQuery(queries.getUnsyncedAttendanceEntities(), pendingEntities)

        // GIVEN: Supabase falla
        coEvery { builder.upsert(any<List<Map<String, Any>>>(), any(), any(), any(), any()) } throws Exception("Network Error")

        // WHEN
        syncManager.syncAll()

        // THEN: NO se llama a marcar como SYNCED
        verify(exactly = 0) { 
            queries.insertOrReplaceAttendance(any(), any(), any(), any(), any(), any(), SyncStatus.SYNCED.name, any(), any()) 
        }
    }

    @Test
    fun `syncAttendance - Empty Queue - no realiza llamadas de red`() = runTest {
        // GIVEN: Lista vacía
        setupMockQuery(queries.getUnsyncedAttendanceEntities(), emptyList())

        // WHEN
        syncManager.syncAll()

        // THEN: No hay interacción con Supabase para Attendance
        coVerify(exactly = 0) { postgrest["attendance"] }
    }

    /**
     * Helper para mockear la cadena queries.method().asFlow().mapToList().first()
     */
    private fun <T : Any> setupMockQuery(queryCall: Query<T>, results: List<T>) {
        val mockQuery = mockk<Query<T>>()
        every { queryCall } returns mockQuery
        // Mocking the chain: asFlow().mapToList()
        // Nota: mapToList es una extensión que requiere un dispatcher, MockK la manejará si mockkStatic funciona
        every { mockQuery.asFlow().mapToList(any()) } returns flowOf(results)
    }
}
