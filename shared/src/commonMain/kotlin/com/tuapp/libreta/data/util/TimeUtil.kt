package com.tuapp.libreta.data.util

import kotlinx.datetime.Instant

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

expect fun currentEpochMs(): Long
expect fun monotonicTimeMs(): Long

/** Converts epoch milliseconds to an ISO-8601 string that Supabase TIMESTAMPTZ accepts. */
fun epochMsToIso(ms: Long): String = Instant.fromEpochMilliseconds(ms).toString()

fun formatIsoToTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        // Supabase often returns strings like 2024-04-26 22:08:26.123+00
        // kotlinx.datetime.Instant.parse expects ISO format (with T)
        val normalized = iso.replace(" ", "T")
        val instant = Instant.parse(normalized)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        ""
    }
}
