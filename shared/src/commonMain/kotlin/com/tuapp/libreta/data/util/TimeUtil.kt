package com.tuapp.libreta.data.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.TimeSource

private val startMark = TimeSource.Monotonic.markNow()

fun currentEpochMs(): Long = Clock.System.now().toEpochMilliseconds()
fun monotonicTimeMs(): Long = startMark.elapsedNow().inWholeMilliseconds

/** Converts epoch milliseconds to an ISO-8601 string that Supabase TIMESTAMPTZ accepts. */
fun epochMsToIso(ms: Long): String = Instant.fromEpochMilliseconds(ms).toString()

/**
 * Converts epoch milliseconds to a `YYYY-MM-DD` string for Postgres `DATE` columns
 * (attendance.date, justifications.date). Inserting epoch-ms strings into a DATE
 * column fails with a 400, so all writes to those columns must go through here.
 */
fun epochMsToSqlDate(ms: Long): String {
    val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC)
    val mm = dt.monthNumber.toString().padStart(2, '0')
    val dd = dt.dayOfMonth.toString().padStart(2, '0')
    return "${dt.year}-$mm-$dd"
}

/**
 * Parses a Postgres `DATE`/`TIMESTAMPTZ` string (e.g. "2026-06-20") back to epoch ms.
 * Tolerates legacy rows that stored epoch-ms as a string, and full ISO timestamps.
 */
fun sqlDateToEpochMs(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    value.toLongOrNull()?.let { return it } // legacy: epoch-ms persisted as string
    return try {
        val normalized = if (value.contains("T")) value.replace(" ", "T") else "${value}T00:00:00Z"
        Instant.parse(normalized).toEpochMilliseconds()
    } catch (_: Exception) {
        0L
    }
}

fun formatIsoToTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        // Supabase often returns strings like 2024-04-26 22:08:26.123+00
        // kotlinx.datetime.Instant.parse expects ISO format (with T)
        val normalized = iso.replace(" ", "T")
        val instant = Instant.parse(normalized)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (_: Exception) {
        ""
    }
}

fun formatEpochToDate(ms: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(ms)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.dayOfMonth.toString().padStart(2, '0')}/${localDateTime.monthNumber.toString().padStart(2, '0')}/${localDateTime.year}"
    } catch (_: Exception) {
        "Fecha inválida"
    }
}
