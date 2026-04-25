package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SupabaseRepositorySafetyTest {

    private val VALID_UUID = "550e8400-e29b-41d4-a716-446655440000"

    @Test
    fun `save attendance should fail if studentId is invalid through UuidString`() {
        assertFailsWith<IllegalArgumentException> {
            Attendance(
                id = null,
                studentId = UuidString("not-a-uuid"), // Esto fallará en el constructor de UuidString
                date = "2023-10-10",
                status = AttendanceStatus.PRESENT,
                justificationId = null
            )
        }
    }

    @Test
    fun `repository delete should not accept empty string due to UuidString parameter`() {
        // El compilador de Kotlin no permitirá pasar "" a una función que espera UuidString.
        // Este test valida que no se puede "engañar" al sistema mediante casting si intentamos crear uno inválido.
        assertFailsWith<IllegalArgumentException> {
            UuidString("")
        }
    }
}
