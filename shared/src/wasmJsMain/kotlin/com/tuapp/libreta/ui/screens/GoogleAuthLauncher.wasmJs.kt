package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.AppLogger
import kotlinx.browser.window
import org.koin.compose.koinInject

// C:/Users/USUARIO/Downloads/LibretaMultiplataformAws/LibretaMultiplataformAws/shared/src/wasmJsMain/kotlin/com/tuapp/libreta/ui/screens/GoogleAuthLauncher.wasmJs.kt

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val authService = koinInject<SupabaseAuthService>()

    return remember(authService) {
        suspend {
            try {
                // DETECCIÓN DINÁMICA DE ORIGEN
                // window.location.origin devuelve el dominio sin la barra final (ej: https://tudominio.vercel.app)
                val currentOrigin = window.location.origin
                
                // Forzamos que la URL de redirección coincida EXACTAMENTE con lo configurado en Supabase.
                // Si estamos en localhost, usamos el puerto 8080. Si no, el dominio actual.
                val redirectUrl = if (currentOrigin.contains("localhost")) {
                    "http://localhost:8080"
                } else {
                    currentOrigin // Esto enviará "https://libretappestudiantil.vercel.app"
                }

                println("Wasm Launcher: Redirecting dynamically to: $redirectUrl")
                authService.signInWithGoogle(redirectUrl = redirectUrl)

            } catch (e: Exception) {
                AppLogger.e("GoogleLauncher", "Error al iniciar sesión: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
