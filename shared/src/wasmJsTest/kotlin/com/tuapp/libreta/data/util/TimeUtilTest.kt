package com.tuapp.libreta.data.util

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TimeUtilTest {

    @Test
    fun `currentEpochMs returns positive value`() {
        val result = currentEpochMs()
        assertTrue(result > 0, "currentEpochMs() debería ser mayor a 0, pero fue: $result")
    }

    @Test
    fun `monotonicTimeMs returns positive value`() {
        val result = monotonicTimeMs()
        assertTrue(result >= 0, "monotonicTimeMs() debería ser mayor o igual a 0, pero fue: $result")
    }

    @Test
    fun `monotonicTimeMs is monotonic - each call returns greater or equal value`() {
        val first = monotonicTimeMs()
        val second = monotonicTimeMs()
        assertTrue(second >= first, "performance.now() debería ser monotónico: first=$first, second=$second")
    }

    @Test
    fun `currentEpochMs increases over time`() {
        val before = currentEpochMs()
        val after = currentEpochMs()
        assertTrue(after >= before, "currentEpochMs debería incrementarse: before=$before, after=$after")
    }

    @Test
    fun `both time functions return Long type`() {
        val epochMs = currentEpochMs()
        val perfMs = monotonicTimeMs()
        assertTrue(epochMs is Long, "currentEpochMs debería retornar Long")
        assertTrue(perfMs is Long, "monotonicTimeMs debería retornar Long")
    }

    @Test
    fun `monotonic time difference is non-negative`() {
        val times = List(10) { monotonicTimeMs() }
        for (i in 1 until times.size) {
            assertTrue(
                times[i] >= times[i - 1],
                "Tiempo monotónico debería ser no-decreciente en iteración $i"
            )
        }
    }
}