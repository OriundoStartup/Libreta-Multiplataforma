package com.tuapp.libreta.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UuidStringTest {

    private val VALID_UUID = "550e8400-e29b-41d4-a716-446655440000"
    private val INVALID_UUID = "not-a-uuid"

    @Test
    fun `UuidString should accept valid UUID`() {
        val uuid = UuidString(VALID_UUID)
        assertEquals(VALID_UUID, uuid.value)
    }

    @Test
    fun `UuidString should throw IllegalArgumentException for invalid UUID`() {
        assertFailsWith<IllegalArgumentException> {
            UuidString(INVALID_UUID)
        }
    }

    @Test
    fun `UuidString should throw IllegalArgumentException for empty string`() {
        assertFailsWith<IllegalArgumentException> {
            UuidString("")
        }
    }

    @Test
    fun `nullIfInvalid should return UuidString for valid input`() {
        val result = VALID_UUID.nullIfInvalid()
        assertEquals(VALID_UUID, result?.value)
    }

    @Test
    fun `nullIfInvalid should return null for invalid input`() {
        assertNull(INVALID_UUID.nullIfInvalid())
    }

    @Test
    fun `nullIfInvalid should return null for empty string`() {
        assertNull("".nullIfInvalid())
    }

    @Test
    fun `toUuidOrNull should return UuidString for valid input`() {
        val result = VALID_UUID.toUuidOrNull()
        assertEquals(VALID_UUID, result?.value)
    }

    @Test
    fun `toUuidOrNull should return null for null input`() {
        val input: String? = null
        assertNull(input.toUuidOrNull())
    }
}
