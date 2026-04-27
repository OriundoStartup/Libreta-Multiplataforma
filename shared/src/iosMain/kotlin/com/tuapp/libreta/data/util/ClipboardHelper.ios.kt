package com.tuapp.libreta.data.util

import platform.UIKit.UIPasteboard

actual object ClipboardHelper {
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
