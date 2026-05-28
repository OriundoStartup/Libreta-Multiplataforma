package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.util.AppLogger

/**
 * FASE 6 — Validación de configuración inicial.
 *
 * Hoy solo el target Web valida que SUPABASE_URL/KEY estén presentes
 * (composeApp/wasmJsMain/main.kt:19). Si BuildKonfig falla silenciosamente
 * en Android o iOS, el crash es opaco.
 *
 * Este helper centraliza la validación y debe invocarse en:
 *   - LibretaApplication.onCreate() (Android)
 *   - MainViewController() (iOS)
 *   - main() (Wasm — ya lo tiene, ahora delegar a este helper)
 *
 * TODO[FASE-6]:
 *   1. Reemplazar el check inline de main.kt:19 por SupabaseConfig.validate().
 *   2. Añadir invocación en LibretaApplication.onCreate().
 *   3. Añadir invocación en MainViewController().
 *   4. En CI: smoke test que verifica que BuildKonfig generated tiene los 3 campos.
 */
fun SupabaseConfig.validate(): ValidationResult {
    val errors = mutableListOf<String>()
    if (URL.isBlank()) errors += "SUPABASE_URL vacío. Revisar local.properties o env vars de Vercel."
    if (ANON_KEY.isBlank()) errors += "SUPABASE_KEY vacío. Revisar local.properties o env vars."
    if (REDIRECT_URL.isBlank()) errors += "SUPABASE_REDIRECT_URL vacío. Requerido para OAuth callback."

    if (URL.isNotBlank() && !URL.startsWith("https://")) {
        errors += "SUPABASE_URL debe ser https:// — actual: $URL"
    }

    return if (errors.isEmpty()) {
        AppLogger.d("SupabaseConfig", "Configuración válida: host=${URL.substringAfter("https://")}")
        ValidationResult.Ok
    } else {
        AppLogger.e("SupabaseConfig", "Configuración inválida: ${errors.joinToString("; ")}")
        ValidationResult.Invalid(errors)
    }
}

sealed interface ValidationResult {
    data object Ok : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

/** Lanza si la config no es válida — útil para parar el arranque temprano en lugar de fallar diferido. */
fun SupabaseConfig.requireValid() {
    when (val r = validate()) {
        ValidationResult.Ok -> Unit
        is ValidationResult.Invalid -> error(
            "Supabase mal configurado:\n - ${r.errors.joinToString("\n - ")}"
        )
    }
}
