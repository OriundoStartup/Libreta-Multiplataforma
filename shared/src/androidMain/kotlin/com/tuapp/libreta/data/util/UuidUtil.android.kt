package com.tuapp.libreta.data.util

import java.util.UUID

actual fun String?.isValidUUID(): Boolean {
    if (this == null) return false
    return try {
        UUID.fromString(this)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun randomUuidString(): String = UUID.randomUUID().toString()
