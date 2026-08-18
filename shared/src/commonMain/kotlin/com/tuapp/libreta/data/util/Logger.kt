package com.tuapp.libreta.data.util

enum class AuditOrigin {
    AUTH, DATA, UNKNOWN
}

object AppLogger {
    private val logCounts = mutableMapOf<String, Int>()
    private val lastLogTime = mutableMapOf<String, Long>()
    private val lastLogContext = mutableMapOf<String, LogContext>()
    private const val RATE_LIMIT_MS = 5000L

    private data class LogContext(val value: String?, val origin: AuditOrigin)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = currentEpochMs()
        println("[ERROR] [$timestamp] [$tag] $message")
        throwable?.printStackTrace()
    }

    fun w(tag: String, message: String) {
        val timestamp = currentEpochMs()
        println("[WARN] [$timestamp] [$tag] $message")
    }

    fun d(tag: String, message: String) {
        val timestamp = currentEpochMs()
        println("[DEBUG] [$timestamp] [$tag] $message")
    }

    fun uuid(flow: String, field: String, value: String?, result: String, origin: AuditOrigin = AuditOrigin.UNKNOWN) {
        val key = "$flow-$field-$result"
        val now = currentEpochMs()
        
        lastLogContext[key] = LogContext(value, origin)

        val lastTime = lastLogTime[key] ?: 0L
        if (now - lastTime < RATE_LIMIT_MS) {
            logCounts[key] = (logCounts[key] ?: 0) + 1
            return
        }

        val count = logCounts[key] ?: 0
        
        // Track the current (first of window) occurrence
        AppMetrics.trackUuid(flow, field, origin, result, 1)

        val countSuffix = if (count > 0) " (Repeated $count times with different values)" else ""
        logCounts[key] = 0
        lastLogTime[key] = now

        // Truncate for privacy and log clarity
        val safeValue = value?.let { 
            if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else it 
        } ?: "null"
        
        val message = "Flow: $flow | Field: $field | SampleValue: $safeValue | Result: $result | Origin: $origin$countSuffix"
        d("UUID_TRACE", message)
    }
    
    fun auditInvalidUuid(flow: String, field: String, value: String?, origin: AuditOrigin) {
        uuid(flow, field, value, "INVALID", origin)
    }
}
