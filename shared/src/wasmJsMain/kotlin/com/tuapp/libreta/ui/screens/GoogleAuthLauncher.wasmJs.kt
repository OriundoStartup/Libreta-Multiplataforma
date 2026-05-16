package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseConfig
import kotlinx.browser.window
import org.koin.compose.koinInject

// C:/Users/USUARIO/Downloads/LibretaMultiplataformAws/LibretaMultiplataformAws/shared/src/wasmJsMain/kotlin/com/tuapp/libreta/ui/screens/GoogleAuthLauncher.wasmJs.kt

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val authService = koinInject<SupabaseAuthService>()

    return remember(authService) {
        suspend {
            try {
                // USAR LA URL DE CONFIGURACIÓN QUE COINCIDE CON EL DASHBOARD
                val redirectUrl = SupabaseConfig.REDIRECT_URL

                println("Wasm Launcher: Redirecting to $redirectUrl")

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
