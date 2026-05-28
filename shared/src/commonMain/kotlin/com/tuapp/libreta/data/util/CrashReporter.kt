package com.tuapp.libreta.data.util

/**
 * FASE 0 — Observabilidad
 *
 * Stub de un reporter remoto (Sentry / GlitchTip / etc.).
 * Inyectado en AppLogger.e() para que los errores en repos remotos
 * salgan automáticamente a un backend de crash reporting en builds release.
 *
 * TODO[FASE-0]:
 *   1. Elegir backend (Sentry self-hosted vs GlitchTip vs Supabase tabla `crash_logs`).
 *   2. Implementar `actual` por plataforma (Ktor POST a endpoint).
 *   3. Inyectar via Koin en `AppModule` con flag BuildKonfig.ENABLE_CRASH_REPORTING.
 *   4. Llamarlo desde AppLogger.e() solo si flag activo y throwable != null.
 */
interface CrashReporter {
    fun report(throwable: Throwable, tag: String, message: String, extra: Map<String, String> = emptyMap())
    fun breadcrumb(category: String, message: String)
    fun setUser(userId: String?, role: String?)
}

/** Default no-op para que el resto del código pueda depender de la interfaz sin nullability. */
object NoOpCrashReporter : CrashReporter {
    override fun report(throwable: Throwable, tag: String, message: String, extra: Map<String, String>) = Unit
    override fun breadcrumb(category: String, message: String) = Unit
    override fun setUser(userId: String?, role: String?) = Unit
}
