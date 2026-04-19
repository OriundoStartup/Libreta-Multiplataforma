package com.tuapp.libreta.data.util

import android.content.Intent

actual object ShareHelper {
    actual fun shareText(text: String) {
        val ctx = AndroidContextHolder.context ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ctx.startActivity(Intent.createChooser(intent, "Compartir código").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
