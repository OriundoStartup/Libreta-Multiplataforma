package com.tuapp.libreta.data.util

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object AppContextHolder {
    private lateinit var application: Application

    fun init(app: Application) {
        application = app
    }

    val appContext: Context get() = application.applicationContext
}

actual object ClipboardHelper {
    actual fun copyToClipboard(text: String) {
        val ctx = AppContextHolder.appContext
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("invite_code", text))
    }
}
