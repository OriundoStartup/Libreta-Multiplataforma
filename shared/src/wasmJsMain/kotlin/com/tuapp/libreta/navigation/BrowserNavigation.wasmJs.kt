package com.tuapp.libreta.navigation

import kotlinx.browser.window

actual fun updateBrowserHistory(path: String) {
    // Usamos Hash Navigation para evitar errores 404 al refrescar (Cannot GET /path)
    val hashPath = if (path.startsWith("/")) "#$path" else "#/$path"
    window.history.pushState(null, "", hashPath)
}

actual fun getInitialPath(): String {
    // Extraemos la ruta desde el hash si existe (ej: #/students/123 -> /students/123)
    val hash = window.location.hash
    return if (hash.startsWith("#")) {
        hash.substring(1)
    } else {
        "/"
    }
}
