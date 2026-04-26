package com.tuapp.libreta.data.util

@JsFun("(text) => { navigator.clipboard?.writeText(text).catch(() => {}); }")
private external fun jsWriteClipboard(text: String)

actual object ClipboardHelper {
    actual fun copyToClipboard(text: String) {
        jsWriteClipboard(text)
    }
}