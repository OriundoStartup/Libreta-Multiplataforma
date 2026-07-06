package org.oriundo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import com.tuapp.libreta.initKoin
import com.tuapp.libreta.data.remote.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val loadingScreen = document.getElementById("loading-screen") as? HTMLElement
    
    println("Web App: Starting Boot Sequence...")
    
    try {
        // 1. Validación de Configuración
        if (SupabaseConfig.URL.isBlank() || SupabaseConfig.ANON_KEY.isBlank()) {
            throw IllegalStateException("Supabase URL/Key no configuradas en local.properties")
        }
        
        // 2. Inicialización de Koin
        println("Web App: Initializing Koin...")
        initKoin()

        // 2.5. Procesar callback OAuth (PKCE).
        // Tras el login con Google, auth-callback.html redirige a /?code=xxx.
        // En wasmJs la SDK NO intercambia ese code automáticamente (a diferencia de
        // Android/iOS que usan handleDeeplinks), así que lo hacemos manualmente aquí.
        // Sin esto, la sesión nunca se crea y la app rebota de vuelta al login.
        handleWebOAuthCallback()

        // 3. Montar App en el DOM
        val root = document.getElementById("app-root") as? HTMLElement
        if (root != null) {
            ComposeViewport(root) {
                App()
            }
            
            // 4. Ocultar pantalla de carga una vez Compose tome el control
            window.setTimeout({
                loadingScreen?.style?.opacity = "0"
                window.setTimeout({
                    loadingScreen?.style?.display = "none"
                    null
                }, 500)
                null
            }, 500)
            
            println("Web App: Successfully started.")
        } else {
            throw IllegalStateException("No se encontró el contenedor #app-root en el HTML")
        }

    } catch (e: Throwable) {
        println("Web App: FATAL ERROR during startup")
        e.printStackTrace()
        
        // Mostrar el error visualmente para salir de la carga infinita
        if (loadingScreen != null) {
            loadingScreen.innerHTML = """
                <div style="background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); max-width: 80%; color: #d32f2f; font-family: sans-serif;">
                    <h2 style="margin-top: 0;">Error de Arranque</h2>
                    <p style="color: #444;">La aplicación no pudo iniciarse correctamente.</p>
                    <pre style="background: #f5f5f5; padding: 10px; font-size: 12px; overflow: auto; max-height: 200px; border: 1px solid #ddd;">${e.message}\n$e</pre>
                    <button onclick="location.reload()" style="background: #6750a4; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; margin-top: 10px;">Reintentar</button>
                </div>
            """.trimIndent()
        }
    }
}

/**
 * Detecta el parámetro `?code=` que Supabase deja en la URL tras el login OAuth
 * (flujo PKCE) y lo canjea por una sesión. Al tener éxito, limpia la URL para que
 * un refresh no reintente canjear un code ya consumido (los codes son de un solo uso).
 */
private fun handleWebOAuthCallback() {
    // Diagnóstico: deja ver en consola con qué URL exacta volvemos de Google.
    println("Web App: URL de arranque -> ${window.location.href}")

    // Si Supabase/Google rechazó el redirect, vuelve con ?error=...&error_description=...
    val error = extractQueryParam("error")
    if (error != null) {
        val desc = extractQueryParam("error_description")
        println("Web App: OAuth devolvió un error -> $error ($desc)")
        return
    }

    val authCode = extractQueryParam("code") ?: run {
        println("Web App: sin ?code= en la URL (carga normal, no es un callback OAuth)")
        return
    }
    println("Web App: OAuth code detectado, canjeando por sesión...")

    val supabase = GlobalContext.get().get<SupabaseClient>()
    CoroutineScope(Dispatchers.Default).launch {
        runCatching { supabase.auth.exchangeCodeForSession(authCode) }
            .onSuccess {
                println("Web App: Sesión establecida correctamente.")
                // Limpiar URL
                window.history.replaceState(null, "LibretApp", window.location.pathname)
            }
            .onFailure {
                println("Web App: ERROR CRÍTICO al canjear code: ${it.message}")
                it.printStackTrace()
                // También limpiamos la URL en caso de fallo para evitar loops de recarga
                window.history.replaceState(null, "LibretApp", window.location.pathname)
            }
    }
}

/** Extrae un parámetro del query string (?a=1&b=2) sin depender de URLSearchParams. */
private fun extractQueryParam(name: String): String? {
    val search = window.location.search
    if (search.isBlank()) return null
    return search.removePrefix("?")
        .split("&")
        .firstNotNullOfOrNull { part ->
            val idx = part.indexOf('=')
            if (idx > 0 && part.substring(0, idx) == name) part.substring(idx + 1) else null
        }
}
