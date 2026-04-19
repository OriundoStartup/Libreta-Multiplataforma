package com.tuapp.libreta.data.util

enum class IntegrityAlertLevel {
    LOW, MEDIUM, CRITICAL, EXTREME
}

data class IntegrityAlert(
    val level: IntegrityAlertLevel,
    val message: String,
    val context: Map<String, String>
)

interface MetricsEvent {
    val name: String
    val params: Map<String, String>
}

data class UuidIntegrityEvent(
    val flow: String,
    val field: String,
    val origin: AuditOrigin,
    val result: String,
    val count: Int = 1
) : MetricsEvent {
    override val name: String = "uuid_integrity_check"
    override val params: Map<String, String> = mapOf(
        "flow" to flow,
        "field" to field,
        "origin" to origin.name,
        "result" to result,
        "count" to count.toString()
    )
}

object AppMetrics {
    private var provider: ((List<MetricsEvent>) -> Unit)? = null
    private var alertHandler: ((IntegrityAlert) -> Unit)? = null
    private val eventQueue = mutableListOf<MetricsEvent>()
    private var lastFlushTime = 0L

    // Estructura híbrida: Map para búsqueda O(1) + Queue para limpieza O(1)
    private data class MetricBucket(var total: Int = 0, var invalid: Int = 0)
    
    private class WindowState {
        val bucketsMap = mutableMapOf<Long, MetricBucket>() // Búsqueda segura por tiempo exacto
        val bucketQueue = ArrayDeque<Long>()               // Orden de expiración
        var windowTotal: Int = 0
        var windowInvalid: Int = 0
    }

    private val contextWindows = mutableMapOf<String, WindowState>()
    /**
     * Bucketización Temporal:
     * Los eventos se agrupan en bloques de 5 segundos para optimizar memoria y CPU.
     * Se utiliza división entera (Long) para alinear los timestamps al inicio del bucket.
     *
     * Definición de Intervalos: [inicio, inicio + BUCKET_SIZE_MS)
     * Los límites son inclusivos por la izquierda y exclusivos por la derecha.
     *
     * Ejemplo (BUCKET_SIZE_MS = 5000):
     * - 12300ms -> (12300 / 5000) * 5000 = 10000ms (Bucket 10s)
     * - 14999ms -> (14999 / 5000) * 5000 = 10000ms (Bucket 10s)
     * - 15000ms -> (15000 / 5000) * 5000 = 15000ms (Bucket 15s) -> Punto de cambio exacto
     * - 15001ms -> (15001 / 5000) * 5000 = 15000ms (Bucket 15s)
     */
    private const val BUCKET_SIZE_MS = 5000L 
    private const val WINDOW_DURATION_MS = 600000L 
    
    // Debounce de alertas
    private val lastAlertTime = mutableMapOf<String, Long>()
    private val lastAlertLevel = mutableMapOf<String, IntegrityAlertLevel>()
    private const val ALERT_DEBOUNCE_MS = 300000L // 5 minutos

    private const val BATCH_SIZE_LIMIT = 10
    private const val BATCH_TIME_LIMIT_MS = 30000L

    // Umbrales de Tasa de Error (%)
    private const val THRESHOLD_MEDIUM_RATE = 10.0 // 10%
    private const val THRESHOLD_CRITICAL_RATE = 30.0 // 30%
    private const val THRESHOLD_EXTREME_RATE = 60.0 // 60%
    private const val MIN_SAMPLES_FOR_ALERT = 5
    
    // Persistencia para escalamiento progresivo
    private val persistenceCounters = mutableMapOf<String, Int>()
    private const val PERSISTENCE_FOR_CRITICAL = 3
    private const val PERSISTENCE_FOR_EXTREME = 5

    fun initialize(
        batchProvider: (List<MetricsEvent>) -> Unit,
        onAlert: ((IntegrityAlert) -> Unit)? = null
    ) {
        provider = batchProvider
        alertHandler = onAlert
        lastFlushTime = monotonicTimeMs()
    }

    fun track(event: MetricsEvent) {
        eventQueue.add(event)
        
        if (event is UuidIntegrityEvent) {
            updateStatsAndEvaluate(event)
        }
        
        checkFlush()
    }

    private fun updateStatsAndEvaluate(event: UuidIntegrityEvent) {
        val key = "${event.flow}:${event.field}:${event.origin}"
        val now = monotonicTimeMs()
        val currentBucketTime = (now / BUCKET_SIZE_MS) * BUCKET_SIZE_MS

        val state = contextWindows.getOrPut(key) { WindowState() }
        
        // 1. Búsqueda por tiempo exacto (O(1)) - Soporta eventos fuera de orden
        val bucket = state.bucketsMap.getOrPut(currentBucketTime) {
            state.bucketQueue.addLast(currentBucketTime)
            MetricBucket()
        }
        
        // Actualizar bucket y acumuladores globales
        bucket.total += event.count
        state.windowTotal += event.count
        
        if (event.result == "INVALID") {
            bucket.invalid += event.count
            state.windowInvalid += event.count
        }

        // 2. Limpieza robusta (O(1) amortizado)
        val expiryTime = now - WINDOW_DURATION_MS
        while (state.bucketQueue.isNotEmpty() && state.bucketQueue.first() < expiryTime) {
            val oldTime = state.bucketQueue.removeFirst()
            val oldBucket = state.bucketsMap.remove(oldTime)
            
            if (oldBucket != null) {
                state.windowTotal -= oldBucket.total
                state.windowInvalid -= oldBucket.invalid
            }
        }

        // 3. Evaluación O(1)
        if (state.windowTotal >= MIN_SAMPLES_FOR_ALERT) {
            val rate = (state.windowInvalid.toDouble() / state.windowTotal) * 100.0
            // ... resto de la lógica de alertas se mantiene ...
            
            val rawLevel = when {
                rate >= THRESHOLD_EXTREME_RATE -> IntegrityAlertLevel.EXTREME
                rate >= THRESHOLD_CRITICAL_RATE -> IntegrityAlertLevel.CRITICAL
                rate >= THRESHOLD_MEDIUM_RATE -> IntegrityAlertLevel.MEDIUM
                else -> null
            }

            if (rawLevel != null) {
                val finalLevel = escalateProgressively(key, rawLevel)
                
                if (shouldSendAlert(key, finalLevel)) {
                    lastAlertTime[key] = now
                    lastAlertLevel[key] = finalLevel
                    
                    alertHandler?.invoke(IntegrityAlert(
                        level = finalLevel,
                        message = "Integrity Alert ($finalLevel): $rate% failure rate in window (last 10m)",
                        context = mapOf(
                            "flow" to event.flow,
                            "field" to event.field,
                            "origin" to event.origin.name,
                            "rate" to "${rate.toInt()}%",
                            "total" to state.windowTotal.toString()
                        )
                    ))
                }
            } else {
                persistenceCounters[key] = 0 
            }
        }
    }

    private fun escalateProgressively(key: String, detectedLevel: IntegrityAlertLevel): IntegrityAlertLevel {
        val currentPersistence = (persistenceCounters[key] ?: 0) + 1
        persistenceCounters[key] = currentPersistence

        return when (detectedLevel) {
            IntegrityAlertLevel.EXTREME -> {
                if (currentPersistence >= PERSISTENCE_FOR_EXTREME) IntegrityAlertLevel.EXTREME 
                else IntegrityAlertLevel.CRITICAL
            }
            IntegrityAlertLevel.CRITICAL -> {
                if (currentPersistence >= PERSISTENCE_FOR_CRITICAL) IntegrityAlertLevel.CRITICAL 
                else IntegrityAlertLevel.MEDIUM
            }
            else -> IntegrityAlertLevel.MEDIUM
        }
    }

    private fun shouldSendAlert(key: String, newLevel: IntegrityAlertLevel): Boolean {
        val lastTime = lastAlertTime[key] ?: 0L
        val lastLevel = lastAlertLevel[key]
        val now = monotonicTimeMs()

        // Si es un nivel más grave, saltar debounce
        if (lastLevel != null && newLevel > lastLevel) return true

        // Si ha pasado el tiempo de enfriamiento
        return (now - lastTime) >= ALERT_DEBOUNCE_MS
    }

    fun flush() {
        if (eventQueue.isEmpty()) return
        val batch = eventQueue.toList()
        eventQueue.clear()
        lastFlushTime = monotonicTimeMs()
        provider?.invoke(batch)
    }

    private fun checkFlush() {
        val now = monotonicTimeMs()
        if (eventQueue.size >= BATCH_SIZE_LIMIT || (now - lastFlushTime) >= BATCH_TIME_LIMIT_MS) {
            flush()
        }
    }

    fun trackUuid(flow: String, field: String, origin: AuditOrigin, result: String, count: Int = 1) {
        track(UuidIntegrityEvent(flow, field, origin, result, count))
    }
}
