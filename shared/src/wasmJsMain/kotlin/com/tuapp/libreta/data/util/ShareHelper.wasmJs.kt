package com.tuapp.libreta.data.util

@JsFun("(text) => { if (navigator.share) { navigator.share({ text: text }).catch(() => {}); } else { navigator.clipboard?.writeText(text).catch(() => {}); } }")
private external fun jsShareText(text: String)

actual object ShareHelper {
    actual fun shareText(text: String) {
        jsShareText(text)
    }
}