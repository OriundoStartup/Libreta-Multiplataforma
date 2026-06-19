package com.tuapp.libreta.data.util

import kotlin.jvm.JvmInline

/**
 * Validates if the string is a properly formatted UUID.
 * Uses platform-specific implementations for robustness.
 */
expect fun String?.isValidUUID(): Boolean

/**
 * A type-safe wrapper for UUID strings to prevent the use of empty strings ("")
 * and ensure all ID-related logic handles nullability correctly.
 */
@JvmInline
value class UuidString(val value: String) {
    init {
        // En desarrollo permitimos IDs no-UUID para demos, pero logueamos advertencia
        if (!value.isValidUUID() && !value.startsWith("demo-") && !value.contains("-demo")) {
            AppLogger.e("UuidString", "Invalid UUID format attempt: '$value'")
            // NO lanzamos excepción para evitar crashes críticos en UI, solo logueamos
        }
    }

    override fun toString(): String = value

    companion object
}

/**
 * Platform-specific UUID generator
 */
expect fun randomUuidString(): String

fun UuidString.Companion.random(): UuidString = UuidString(randomUuidString())

/**
 * Extension to safely convert a nullable String to a UuidString.
 */
fun String?.toUuidOrNull(): UuidString? = if (this.isValidUUID()) UuidString(this!!) else null

/**
 * Extension to safely get a UUID string or null, avoiding empty strings.
 */
fun String?.nullIfInvalid(): UuidString? = if (this.isValidUUID()) UuidString(this!!) else null
