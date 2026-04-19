package com.tuapp.libreta.data.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

// Necesitamos una forma de obtener el contexto. 
// Normalmente en KMP se puede pasar o usar un static holder.
object AndroidContextHolder {
    var context: Context? = null
}

actual object ClipboardHelper {
    actual fun copyToClipboard(text: String) {
        val ctx = AndroidContextHolder.context ?: return
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("invite_code", text)
        clipboard.setPrimaryClip(clip)
    }
}
