package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
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
                val redirectUrl = "$currentOrigin/auth-callback.html"

                println("Wasm Launcher: Redirecting back to $redirectUrl")

                val url = authService.getGoogleOAuthUrl(
                    redirectTo = redirectUrl,
                    prompt = "select_account"
                )

                if (url.isNotBlank()) {
                    window.location.assign(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
