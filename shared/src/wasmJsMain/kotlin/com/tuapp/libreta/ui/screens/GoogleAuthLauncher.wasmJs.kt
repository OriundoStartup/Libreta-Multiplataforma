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
                // DETECCIÓN DINÁMICA: Usamos el origen actual del navegador (ej: https://tudominio.vercel.app)
                // Esto evita que en producción intente redirigir a 'localhost'.
                val currentOrigin = window.location.origin
                
                // ELIMINAR EL INTERMEDIARIO: Redirigimos directamente a la raíz (/)
                // Esto asegura que el PKCE verifier se encuentre en el mismo contexto de URL.
                val redirectUrl = "$currentOrigin/"

                println("Wasm Launcher: Initiating PKCE flow directly to: $redirectUrl")

                // CRÍTICO: Usar signInWith para que la SDK gestione el PKCE Verifier
                // Esto guarda el code_verifier en el LocalStorage antes de redirigir.
                authService.signInWithGoogle(redirectUrl = redirectUrl)

            } catch (e: Exception) {
                AppLogger.e("GoogleLauncher", "Error al iniciar sesión: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
