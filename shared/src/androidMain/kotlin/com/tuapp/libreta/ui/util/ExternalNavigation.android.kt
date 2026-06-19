package com.tuapp.libreta.ui.util

import android.content.Intent
import android.net.Uri
import com.tuapp.libreta.data.util.AppContextHolder
import com.tuapp.libreta.data.util.AppLogger

actual fun openUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Necesario porque se lanza desde el applicationContext (no una Activity)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        AppContextHolder.appContext.startActivity(intent)
    }.onFailure { e ->
        AppLogger.e("ExternalNavigation", "No se pudo abrir la URL: $url", e)
    }
}
