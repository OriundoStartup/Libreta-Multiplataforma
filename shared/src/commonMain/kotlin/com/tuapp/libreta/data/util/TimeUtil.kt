package com.tuapp.libreta.data.util

import kotlinx.datetime.Instant

expect fun currentEpochMs(): Long
expect fun monotonicTimeMs(): Long

/** Converts epoch milliseconds to an ISO-8601 string that Supabase TIMESTAMPTZ accepts. */
fun epochMsToIso(ms: Long): String = Instant.fromEpochMilliseconds(ms).toString()
