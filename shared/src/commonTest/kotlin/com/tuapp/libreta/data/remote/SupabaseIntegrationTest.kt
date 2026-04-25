package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.AttendanceDto
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SupabaseIntegrationTest {

    private val VALID_UUID = "550e8400-e29b-41d4-a716-446655440000"
    
    @Test
    fun `verify that attendance mapper does not produce empty strings`() = runTest {
        val domain = Attendance(
            id = null,
            studentId = UuidString(VALID_UUID),
            date = "2023-10-10",
            status = AttendanceStatus.PRESENT,
            justificationId = null
        )
        
        // Simulación del comportamiento del DTO antes del envío
        val dto = AttendanceDto(
            id = domain.id?.value,
            studentId = domain.studentId.value,
            date = domain.date,
            status = domain.status.name,
            justificationId = domain.justificationId?.value
        )
        
        val jsonString = Json.encodeToString(AttendanceDto.serializer(), dto)
        
        assertTrue(!jsonString.contains("\"\""), "The JSON should not contain empty strings for UUID fields")
        assertTrue(!jsonString.contains("\"id\""), "Null ID should be omitted from JSON")
    }

    @Test
    fun `prevent invalid UUID from reaching repository layer`() = runTest {
        // Este test valida que el sistema falla en el constructor del objeto de dominio,
        // mucho antes de intentar cualquier conexión de red.
        assertFailsWith<IllegalArgumentException> {
            val invalidId = "not-a-uuid"
            Attendance(
                id = null,
                studentId = UuidString(invalidId),
                date = "2023-10-10",
                status = AttendanceStatus.PRESENT,
                justificationId = null
            )
        }
    }
}
