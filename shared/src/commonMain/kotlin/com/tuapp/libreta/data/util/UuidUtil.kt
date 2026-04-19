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
        if (!value.isValidUUID()) {
            AppLogger.e("UuidString", "Invalid UUID format attempt: '$value'")
            throw IllegalArgumentException("Invalid UUID format: '$value'")
        }
    }

    override fun toString(): String = value
    fun toUuidOrNull(): UuidString {
        TODO("Provide the return value")
    }

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
