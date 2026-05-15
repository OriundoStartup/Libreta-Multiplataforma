package org.oriundo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import com.tuapp.libreta.initKoin
import com.tuapp.libreta.data.remote.SupabaseConfig
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
