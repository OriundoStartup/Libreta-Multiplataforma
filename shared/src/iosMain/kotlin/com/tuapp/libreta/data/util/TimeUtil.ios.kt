package com.tuapp.libreta.data.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

actual fun currentEpochMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
actual fun monotonicTimeMs(): Long = (CFAbsoluteTimeGetCurrent() * 1000).toLong()
