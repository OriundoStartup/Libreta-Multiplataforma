package com.tuapp.libreta.data.util

import kotlin.jvm.JvmInline

/**
 * FASE 2 — Sellado real de UuidString.
 *
 * Coexiste temporalmente con la `UuidString` de [UuidUtil.kt] (que solo loguea
 * advertencia sin lanzar). Este V2 SÍ valida y SÍ lanza excepción si recibe
 * un string que no es UUID v4 válido.
 *
 * Plan:
 *   1. Migrar gradualmente call-sites a UuidV2.of(s) / UuidV2.random().
 *   2. Una vez migrado, renombrar UuidV2 → UuidString y borrar la versión laxa.
 *   3. Mantener `String.toUuidOrNull()` retornando null en vez de excepción.
 *
 * TODO[FASE-2]:
 *   - Ajustar UuidSafetyLintTest.kt para que valide construcciones con UuidV2.
 *   - Reemplazar el fallback de SupabaseStudentRepository:27 por UuidV2.random()
 *     persistiendo el ID generado de vuelta en la fila enrollment.student_id.
 */
@JvmInline
value class UuidV2 private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        private val UUID_REGEX = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )

        /** Construye con validación estricta. Lanza si el formato no es UUID. */
        fun of(value: String): UuidV2 {
            require(UUID_REGEX.matches(value)) { "Invalid UUID format: $value" }
            return UuidV2(value)
        }

        /** Construye sin validar (uso interno o tests). Marcado para que sea fácil de auditar. */
        @InternalUuidApi
        fun unsafe(value: String): UuidV2 = UuidV2(value)

        /** Genera un UUID v4 nuevo usando el [randomUuidString] expect/actual. */
        fun random(): UuidV2 = UuidV2(randomUuidString())

        /** Conversión nullable — null si no es UUID válido. */
        fun ofOrNull(value: String?): UuidV2? =
            if (value != null && UUID_REGEX.matches(value)) UuidV2(value) else null
    }
}

@RequiresOptIn(message = "Construcción de UuidV2 sin validación. Solo para tests / migraciones.")
annotation class InternalUuidApi
