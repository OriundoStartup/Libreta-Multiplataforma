package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DtoSerializationTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun testAttendanceDtoSerialization_OmitNullsAndNoEmptyStrings() {
        val dto = AttendanceDto(
            id = null,
            studentId = "550e8400-e29b-41d4-a716-446655440000",
            date = "2023-10-10",
            status = "PRESENT",
            justificationId = null
        )

        val jsonString = Json.encodeToString(AttendanceDto.serializer(), dto)

        // Verificar que no se incluyen los campos nulos (debido a @EncodeDefault(NEVER))
        assertFalse(jsonString.contains("\"id\""), "El campo 'id' no debe serializarse si es null")
        assertFalse(jsonString.contains("\"justification_id\""), "El campo 'justification_id' no debe serializarse si es null")
        assertFalse(jsonString.contains("\"created_at\""), "El campo 'created_at' no debe serializarse si es null")
        
        // Verificar que no hay strings vacíos representados
        assertFalse(jsonString.contains(":\"\""), "No debe haber valores de String vacío en la serialización")
        
        // Verificar campos obligatorios
        assertTrue(jsonString.contains("\"student_id\""), "Debe contener student_id")
    }

    @Test
    fun testCommunicationDtoSerialization_OmitNullsAndNoEmptyStrings() {
        val dto = CommunicationDto(
            senderId = "sender-uuid",
            courseId = "course-uuid",
            messageText = "Hola Mundo",
            id = null,
            receiverId = null,
            studentId = null
        )

        val jsonString = Json.encodeToString(CommunicationDto.serializer(), dto)

        // Verificar omisión de opcionales nulos
        assertFalse(jsonString.contains("\"id\""))
        assertFalse(jsonString.contains("\"receiver_id\""))
        assertFalse(jsonString.contains("\"student_id\""))
        assertFalse(jsonString.contains("\"created_at\""))

        // Verificar que los obligatorios y valores por defecto SI están
        assertTrue(jsonString.contains("\"sender_id\""))
        assertTrue(jsonString.contains("\"course_id\""))
        assertTrue(jsonString.contains("\"message_text\""))
        assertTrue(jsonString.contains("\"category\":\"AVISO_GENERAL\""))
        
        // Verificar ausencia de ""
        assertFalse(jsonString.contains(":\"\""))
    }

    @Test
    fun testJustificationDtoSerialization_OmitNullsAndNoEmptyStrings() {
        val dto = JustificationSupabaseDto(
            id = null,
            studentId = "stu-id",
            parentId = null,
            date = 123456789L,
            reason = "Enfermedad"
        )

        val jsonString = Json.encodeToString(JustificationSupabaseDto.serializer(), dto)

        assertFalse(jsonString.contains("\"id\""))
        assertFalse(jsonString.contains("\"parent_id\""))
        assertFalse(jsonString.contains("\"created_at\""))
        
        assertTrue(jsonString.contains("\"student_id\""))
        assertTrue(jsonString.contains("\"reason\""))
        
        assertFalse(jsonString.contains(":\"\""))
    }
}
