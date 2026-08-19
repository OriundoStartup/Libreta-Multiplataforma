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
    println("Web App: Forcing deployment sync - August 19 2026")
    
    try {
        // 1. Validación de Configuración
        if (SupabaseConfig.URL.isBlank() || SupabaseConfig.ANON_KEY.isBlank()) {
            throw IllegalStateException("Supabase URL/Key no configuradas en local.properties")
        }
        
        // 2. Inicialización de Koin
        println("Web App: Initializing Koin...")
        initKoin()

        // 3. Montar App en el DOM (dentro de una corrutina para manejar el canje de código OAuth)
        val root = document.getElementById("app-root") as? HTMLElement
        if (root != null) {
            CoroutineScope(Dispatchers.Main).launch {
                // 2.5. Procesar callback OAuth (PKCE) de forma síncrona/secuencial
                handleWebOAuthCallback()

                // INFORMACIÓN OFICIAL: Configurar mapeo de recursos para web
                // Evita problemas de 404 en subrutas y despliegues edge (Vercel)
                org.jetbrains.compose.resources.configureWebResources {
                    resourcePathMapping { path -> "./$path" }
                }

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
            }
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
 * Detecta el parámetro `?code=` o `#code=` que Supabase deja en la URL tras el login OAuth
 * (flujo PKCE) y lo canjea por una sesión.
 */
private suspend fun handleWebOAuthCallback() {
    val fullUrl = window.location.href
    println("Web App: Boot URL -> $fullUrl")

    // Supabase puede devolver el código en el query (?) o en el fragmento (#) dependiendo de la config.
    val authCode = extractQueryParam("code") ?: extractHashParam("code")
    
    if (authCode == null) {
        val error = extractQueryParam("error") ?: extractHashParam("error")
        if (error != null) {
            val desc = extractQueryParam("error_description") ?: extractHashParam("error_description")
            println("Web App: OAuth Error Detected -> $error: $desc")
        } else {
            println("Web App: No auth code found in URL. Standard boot.")
        }
        return
    }

    println("Web App: OAuth Code Found! Exchanging for session... (Code: ${authCode.take(5)}...)")

    val supabase = GlobalContext.get().get<SupabaseClient>()
    try {
        println("Web App: Calling exchangeCodeForSession...")
        supabase.auth.exchangeCodeForSession(authCode)
        println("Web App: Session established SUCCESSFULLY.")
        
        // Limpiar URL de forma inteligente: mantener la ruta actual pero quitar el ?code=
        val targetUrl = window.location.origin + window.location.pathname
        println("Web App: Cleaning URL to: $targetUrl")
        window.history.replaceState(null, "LibretApp", targetUrl)
    } catch (e: Exception) {
        println("Web App: FATAL ERROR during code exchange: ${e.message}")
        e.printStackTrace()
        val targetUrl = window.location.origin + window.location.pathname
        window.history.replaceState(null, "LibretApp", targetUrl)
    }
}

/** Extrae un parámetro del query string (?a=1&b=2) */
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

/** Extrae un parámetro del fragmento (#a=1&b=2) - Útil si se usa Implicit Flow o Hash Routing */
private fun extractHashParam(name: String): String? {
    val hash = window.location.hash
    if (hash.isBlank()) return null
    return hash.removePrefix("#")
        .removePrefix("/") // Por si es #/code=...
        .split("&")
        .firstNotNullOfOrNull { part ->
            val idx = part.indexOf('=')
            if (idx > 0 && part.substring(0, idx) == name) part.substring(idx + 1) else null
        }
}
