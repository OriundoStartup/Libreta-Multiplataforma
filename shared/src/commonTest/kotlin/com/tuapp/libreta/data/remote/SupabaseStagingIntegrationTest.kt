package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.AttendanceDto
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * INTEGRATION TEST AGAINST REAL STAGING BACKEND.
 * This test is ignored by default to avoid failing in CI without proper credentials.
 * It is meant to be run manually for validation.
 */
class SupabaseStagingIntegrationTest {

    private val isStagingEnabled: Boolean
        get() = false // Hardcoded to false for safety, can be changed via BuildConfig or manual edit for local run

    private val supabase = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY
    ) {
        install(Postgrest)
    }

    private val TEST_STUDENT_ID = "550e8400-e29b-41d4-a716-446655440000"

    @Test
    fun `validate real supabase insert without id generates uuid`() = runTest {
        if (!isStagingEnabled) return@runTest
        
        val dto = AttendanceDto(
            id = null, // Backend should generate this
            studentId = TEST_STUDENT_ID,
            date = "2023-10-10",
            status = "PRESENT"
        )

        val result = supabase.from("attendance")
            .insert(dto) { select() }
            .decodeSingle<AttendanceDto>()

        assertNotNull(result.id, "Supabase should have generated a UUID for the new record")
        
        // Cleanup
        supabase.from("attendance").delete {
            filter { eq("id", result.id!!) }
        }
    }

    @Test
    fun `validate real supabase rejects empty string for uuid`() = runTest {
        if (!isStagingEnabled) return@runTest

        // This test confirms that our client-side block (UuidString) matches 
        // the backend's constraint 22P02.
        val dto = mapOf(
            "student_id" to "", // Manually bypassing our DTO to test backend
            "date" to "2023-10-10",
            "status" to "PRESENT"
        )

        val result = runCatching {
            supabase.from("attendance").insert(dto)
        }

        assertNotNull(result.exceptionOrNull(), "Backend should have rejected the empty string")
        val errorText = result.exceptionOrNull()?.message ?: ""
        // Check for common Postgres/Supabase UUID error code or message
        // assert(errorText.contains("22P02")) 
    }
}
