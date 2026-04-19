package com.tuapp.libreta.data.util

import platform.Foundation.NSUUID

actual fun String?.isValidUUID(): Boolean {
    if (this == null) return false
    return runCatching { NSUUID(uUIDString = this) }.isSuccess
}

actual fun randomUuidString(): String = NSUUID().UUIDString
