package com.tuapp.libreta.data.util

actual fun currentEpochMs(): Long = System.currentTimeMillis()
actual fun monotonicTimeMs(): Long = android.os.SystemClock.elapsedRealtime()
