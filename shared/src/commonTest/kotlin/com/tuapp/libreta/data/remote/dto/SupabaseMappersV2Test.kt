package com.tuapp.libreta.data.remote.dto

import kotlin.test.Test
import kotlin.test.Ignore

/**
 * FASE 5 — Tests de mappers DTO V2 ↔ Domain.
 *
 * Esqueleto. Cuando se cree SupabaseMappersV2.kt en FASE 1, rellenar
 * un test por cada `.toDomain()` y `.toDto()`.
 *
 * Cobertura objetivo: 100% branches en mappers.
 *
 * TODO[FASE-5]:
 *   - StudentDto.toDomain / Student.toDtoV2: nombre vacío, UUID inválido,
 *     studentRut null vs string, courseId inválido.
 *   - AttendanceDto: status unknown → ABSENT, fecha mal formada.
 *   - MessageDtoV2: message_text vacío, receiverId null.
 *   - JustificationDtoV2: status case-insensitive, documentUrl null.
 *   - InvitationCodeDtoV2: code case-insensitive (claim() lo uppercase-a).
 */
class SupabaseMappersV2Test {

    @Test
    @Ignore // FASE-5: rellenar cuando se cree SupabaseMappersV2.kt
    fun `StudentDto toDomain handles missing rut`() {
        // val dto = StudentDto(id = "00000000-0000-0000-0000-000000000001", fullName = "Juan Pérez", courseId = "...", parentId = "...")
        // val domain = dto.toDomain()
        // assertEquals("Juan Pérez", domain.fullName)
        // assertNull(domain.studentRut)
    }

    @Test
    @Ignore
    fun `AttendanceDto toDomain maps unknown status to ABSENT`() {
        // FASE-5: implementar
    }

    @Test
    @Ignore
    fun `MessageDtoV2 toDomain preserves message_text in content`() {
        // FASE-5: implementar
    }

    @Test
    @Ignore
    fun `JustificationDtoV2 toDomain handles case-insensitive status`() {
        // FASE-5: implementar
    }
}
