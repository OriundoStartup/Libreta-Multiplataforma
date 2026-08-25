package org.oriundo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import com.tuapp.libreta.initKoin
import com.tuapp.libreta.data.util.AppLogger
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
    
    AppLogger.d("Main", "Starting Boot Sequence...")
    
    try {
        // 1. Validación de Configuración
        if (SupabaseConfig.URL.isBlank() || SupabaseConfig.ANON_KEY.isBlank()) {
            throw IllegalStateException("Supabase URL/Key no configuradas")
        }
        
        // 2. Inicialización de Koin
        AppLogger.d("Main", "Initializing Koin...")
        try {
            initKoin()
            AppLogger.d("Main", "Koin initialized successfully.")
        } catch (e: Throwable) {
            AppLogger.e("Main", "KOIN ERROR -> ${e.message}")
            throw e 
        }

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

                AppLogger.d("Main", "Web App: Successfully started.")
            }
        } else {
            throw IllegalStateException("No se encontró el contenedor #app-root en el HTML")
        }

    } catch (e: Throwable) {
        AppLogger.e("Main", "FATAL ERROR during startup", e)
        
        val causeMessage = e.cause?.message ?: "Sin causa detallada"
        val causeTrace = e.cause?.let { 
            // Intentar obtener algo útil del stacktrace de la causa
            it.toString() 
        } ?: ""

        // Mostrar el error visualmente para salir de la carga infinita
        if (loadingScreen != null) {
            loadingScreen.innerHTML = """
                <div style="background: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); max-width: 80%; color: #d32f2f; font-family: sans-serif;">
                    <h2 style="margin-top: 0;">Error de Arranque</h2>
                    <p style="color: #444;">La aplicación no pudo iniciarse correctamente.</p>
                    <div style="background: #f5f5f5; padding: 10px; font-size: 12px; overflow: auto; max-height: 300px; border: 1px solid #ddd; text-align: left;">
                        <strong>Mensaje:</strong> ${e.message}<br><br>
                        <strong>Causa:</strong> $causeMessage<br>
                        <pre style="margin-top: 10px; border-top: 1px solid #eee; pt: 10px;">$causeTrace</pre>
                    </div>
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
    // No logueamos la URL completa para evitar filtrar el ?code= en logs de producción
    val authCode = extractQueryParam("code") ?: extractHashParam("code")
    
    if (authCode == null) {
        val error = extractQueryParam("error") ?: extractHashParam("error")
        if (error != null) {
            val desc = extractQueryParam("error_description") ?: extractHashParam("error_description")
            AppLogger.e("OAuth", "OAuth Error Detected -> $error: $desc")
        } else {
            AppLogger.d("OAuth", "No auth code found in URL. Standard boot.")
        }
        return
    }

    AppLogger.d("OAuth", "OAuth Code Found! Exchanging for session...")

    val supabase = GlobalContext.get().get<SupabaseClient>()
    try {
        supabase.auth.exchangeCodeForSession(authCode)
        AppLogger.d("OAuth", "Session established SUCCESSFULLY.")
        
        val targetUrl = window.location.origin + window.location.pathname
        window.history.replaceState(null, "LibretApp", targetUrl)
    } catch (e: Exception) {
        AppLogger.e("OAuth", "FATAL ERROR during code exchange: ${e.message}", e)
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
