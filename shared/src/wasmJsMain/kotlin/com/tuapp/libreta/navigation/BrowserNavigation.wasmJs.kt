package com.tuapp.libreta.navigation

import kotlinx.browser.window

actual fun updateBrowserHistory(path: String) {
    // IMPORTANTE: Preservamos los parámetros de búsqueda (?code=...) durante la navegación inicial.
    // Si los borramos antes de que Supabase los lea, el login fallará (común en Incógnito).
    val currentSearch = window.location.search
    
    // Usamos Hash Navigation para evitar errores 404 al refrescar (Cannot GET /path)
    val hashPath = if (path.startsWith("/")) "#$path" else "#/$path"
    
    // Combinamos el hash nuevo con los parámetros actuales si existen
    val finalUrl = if (currentSearch.isNotBlank()) "$currentSearch$hashPath" else hashPath
    
    window.history.pushState(null, "", finalUrl)
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
