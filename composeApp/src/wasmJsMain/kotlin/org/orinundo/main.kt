package org.orinundo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import com.tuapp.libreta.initKoin
import com.tuapp.libreta.data.remote.SupabaseConfig
import org.w3c.dom.HTMLElement

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val loadingScreen = document.getElementById("loading-screen") as? HTMLElement
    
    println("Web App: Starting...")
    println("Web App: Supabase URL: ${SupabaseConfig.URL}")

    try {
        // Intentar inicializar Koin
        println("Web App: Initializing Koin...")
        initKoin()
        println("Web App: Koin OK.")

        val root = document.getElementById("app-root") as? HTMLElement
        if (root != null) {
            ComposeViewport(root) {
                App()
            }
            
            // Ocultar pantalla de carga con un pequeño delay para asegurar el primer frame
            kotlinx.browser.window.setTimeout({
                loadingScreen?.style?.opacity = "0"
                kotlinx.browser.window.setTimeout({
                    loadingScreen?.style?.display = "none"
                    null
                }, 500)
                null
            }, 1000)
        } else {
            throw IllegalStateException("No se encontró el elemento #app-root")
        }

    } catch (e: Throwable) {
        println("Web App: FATAL ERROR -> ${e.message}")
        e.printStackTrace()
        
        if (loadingScreen != null) {
            loadingScreen.innerHTML = """
                <div style="color: #d32f2f; text-align: center; padding: 20px;">
                    <h3>Error de Inicialización</h3>
                    <p style="font-family: monospace; font-size: 13px;">${e.message}</p>
                    <button onclick="location.reload()" style="padding: 10px 20px;">Reintentar</button>
                </div>
            """.trimIndent()
        }
    }
}
