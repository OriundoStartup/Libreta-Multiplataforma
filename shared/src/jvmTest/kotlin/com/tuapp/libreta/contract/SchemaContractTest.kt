package com.tuapp.libreta.contract

import kotlin.test.Test
import kotlin.test.fail

/**
 * FASE 1 — Test de contrato DTO ↔ Schema autoritativo.
 *
 * Compara los `@SerialName` de cada DTO contra una lista de columnas
 * declaradas en `supabase/CLAUDE.md` (o en un schema.json generado a partir
 * de `pg_dump --schema-only` parseado).
 *
 * Falla el build si:
 *   - Un DTO envía un campo que no existe en BD.
 *   - Un DTO omite un campo NOT NULL sin default en BD.
 *   - Un DTO usa un nombre distinto al que tiene la columna.
 *
 * TODO[FASE-1]:
 *   1. Generar `shared/src/jvmTest/resources/schema_canonical.json` a partir de
 *      la migración 002 + 003 (o vía CLI de Supabase: `supabase db dump --schema-only`).
 *   2. Implementar parser de schema_canonical.json.
 *   3. Reflejar cada DTO con kotlin.reflect o usar kotlinx.serialization SerialDescriptor.
 *   4. Reportar diff en JUnit-friendly assertion.
 */
class SchemaContractTest {

    @Test
    fun `all V2 DTOs must match canonical schema`() {
        // TODO[FASE-1]: implementar carga del schema canonical
        val schema: Map<String, Set<String>> = loadCanonicalSchema()
        val drift = mutableListOf<String>()

        val tablesToVerify = listOf(
            "students" to listOf("id", "full_name", "student_rut", "course_id", "parent_id", "created_at"),
            "attendance" to listOf("id", "student_id", "date", "status", "created_at"),
            "courses" to listOf("id", "name", "description", "subject", "grade", "section", "class_code", "school_id", "teacher_id", "invite_code", "is_active", "created_at"),
            "profiles" to listOf("id", "full_name", "email", "role", "created_at"),
            "messages" to listOf("id", "sender_id", "receiver_id", "message_text", "read_at", "created_at"),
            "communications" to listOf("id", "sender_id", "course_id", "message_text", "category", "created_at"),
            "enrollments" to listOf("id", "course_id", "parent_id", "student_id", "student_name", "student_rut", "enrolled_at"),
            "invitation_codes" to listOf("code", "student_id", "teacher_id", "claimed_by", "expires_at", "created_at"),
            "course_assignments" to listOf("id", "teacher_id", "course_id", "school_id", "is_head_teacher", "created_at"),
            "justifications" to listOf("id", "student_id", "date", "reason", "status", "created_at"),
            "schools" to listOf("id", "name", "address", "created_at")
        )

        for ((table, expectedCols) in tablesToVerify) {
            val actualCols = schema[table] ?: run {
                drift += "Tabla `$table` ausente en schema canonical"
                continue
            }
            val missing = expectedCols.filterNot { it in actualCols }
            val extra = actualCols.filterNot { it in expectedCols }
            if (missing.isNotEmpty()) drift += "$table: faltan columnas $missing en BD"
            if (extra.isNotEmpty())   drift += "$table: BD tiene columnas extra $extra no contempladas por DTO"
        }

        if (drift.isNotEmpty()) fail("Schema drift detectado:\n${drift.joinToString("\n")}")
    }

    private fun loadCanonicalSchema(): Map<String, Set<String>> {
        // TODO[FASE-1]: parsear schema_canonical.json desde resources
        // Por ahora retorna vacío y el test queda en estado "rojo intencional"
        // hasta que se implemente la carga real.
        return emptyMap()
    }
}
