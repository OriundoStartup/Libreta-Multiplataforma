package com.tuapp.libreta.data.util

/**
 * FASE 0 — Métricas internas.
 *
 * Logger.kt ya hace referencia a `AppMetrics.trackUuid(...)`, este archivo
 * provee la implementación stub para que compile y deja un hook claro para
 * mandar métricas a un backend (PostHog / Plausible / tabla Supabase).
 *
 * TODO[FASE-0]:
 *   - Batch en memoria + flush cada 30 s
 *   - Implementar destino remoto (RPC `track_event` en Supabase)
 *   - Respetar privacidad: nunca enviar PII (truncar UUIDs como hace AppLogger.uuid)
 */
object AppMetrics {
    fun trackUuid(flow: String, field: String, origin: AuditOrigin, result: String, count: Int) {
        // TODO[FASE-0]: bufferizar y enviar a backend de métricas
    }

    fun trackEvent(name: String, props: Map<String, String> = emptyMap()) {
        // TODO[FASE-0]: idem
    }
}
