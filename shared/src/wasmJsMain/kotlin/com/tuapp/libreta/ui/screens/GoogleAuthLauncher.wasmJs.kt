package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.util.AppLogger
import kotlinx.browser.window
import org.koin.compose.koinInject

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val authService = koinInject<SupabaseAuthService>()
    
    return remember(authService) {
        suspend {
            try {
                println("Wasm Launcher: Start redirection flow")
                
                val currentOrigin = window.location.origin
                // En Web, ignoramos el esquema de Android (si existiera) y usamos siempre el origen actual
                val redirectUrl = "$currentOrigin/auth-callback.html"
                
                println("Wasm Launcher: Origin is $currentOrigin")
                println("Wasm Launcher: Redirecting to $redirectUrl")
                
                val url = authService.getGoogleOAuthUrl(redirectTo = redirectUrl)
                println("Wasm Launcher: Generated OAuth URL: $url")
                
                if (url.isNotBlank()) {
                    AppLogger.d("Wasm Launcher", "Navigating to: $url")
                    window.location.assign(url)
                } else {
                    AppLogger.e("Wasm Launcher", "ERROR - Generated URL is empty")
                }
            } catch (e: Exception) {
                println("Wasm Launcher: EXCEPTION -> ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
