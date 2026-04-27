package com.tuapp.libreta.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UuidUtilTest {

    @Test
    fun `valid UUID v4 returns true`() {
        val result = "550e8400-e29b-41d4-a716-446655440000".isValidUUID()
        assertTrue(result, "UUID v4 válido debería retornar true")
    }

    @Test
    fun `UUID with uppercase letters returns true`() {
        val result = "550E8400-E29B-41D4-A716-446655440000".isValidUUID()
        assertTrue(result, "UUID en mayúsculas debería retornar true")
    }

    @Test
    fun `UUID with mixed case returns true`() {
        val result = "550e8400-E29B-41d4-A716-446655440000".isValidUUID()
        assertTrue(result, "UUID con caso mixto debería retornar true")
    }

    @Test
    fun `invalid string returns false`() {
        val result = "not-a-uuid".isValidUUID()
        assertFalse(result, "String inválido debería retornar false")
    }

    @Test
    fun `null string returns false`() {
        val nullString: String? = null
        val result = nullString.isValidUUID()
        assertFalse(result, "String null debería retornar false")
    }

    @Test
    fun `empty string returns false`() {
        val result = "".isValidUUID()
        assertFalse(result, "String vacío debería retornar false")
    }

    @Test
    fun `UUID without dashes returns false`() {
        val result = "550e8400e29b41d4a716446655440000".isValidUUID()
        assertFalse(result, "UUID sin guiones debería retornar false")
    }

    @Test
    fun `UUID with any version returns true`() {
        val result = "550e8400-e29b-61d4-a716-446655440000".isValidUUID()
        assertTrue(result, "UUID con versión 6 debería retornar true con el regex relajado")
    }

    @Test
    fun `UUID with any variant returns true`() {
        val result = "550e8400-e29b-41d4-c716-446655440000".isValidUUID()
        assertTrue(result, "UUID con variante c debería retornar true con el regex relajado")
    }

    @Test
    fun `UUID too short returns false`() {
        val result = "550e8400-e29b-41d4-a716".isValidUUID()
        assertFalse(result, "UUID incompleto debería retornar false")
    }

    @Test
    fun `UUID with invalid characters returns false`() {
        val result = "550e8400-e29b-41d4-a716-44665544000g".isValidUUID()
        assertFalse(result, "UUID con caracteres inválidos (g) debería retornar false")
    }

    @Test
    fun `randomUuidString generates valid UUID`() {
        val uuid = randomUuidString()
        assertTrue(uuid.isValidUUID(), "crypto.randomUUID() debería generar UUID válido: $uuid")
    }

    @Test
    fun `randomUuidString generates different UUIDs`() {
        val uuid1 = randomUuidString()
        val uuid2 = randomUuidString()
        assertTrue(uuid1 != uuid2, "Dos llamadas deberían generar UUIDs diferentes")
    }
}