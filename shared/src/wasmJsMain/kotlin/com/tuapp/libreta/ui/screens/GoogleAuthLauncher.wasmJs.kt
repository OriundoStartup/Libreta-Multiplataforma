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
                val redirectUrl = if (SupabaseConfig.REDIRECT_URL.isNotBlank()) {
                    SupabaseConfig.REDIRECT_URL
                } else {
                    "$currentOrigin/auth-callback.html"
                }
                
                println("Wasm Launcher: Origin is $currentOrigin")
                println("Wasm Launcher: Redirecting to $redirectUrl")
                
                val url = authService.getGoogleOAuthUrl(redirectTo = redirectUrl)
                println("Wasm Launcher: Generated OAuth URL: $url")
                
                if (url.isNotBlank()) {
                    println("Wasm Launcher: Navigating now...")
                    window.location.href = url
                } else {
                    println("Wasm Launcher: ERROR - Generated URL is empty")
                }
            } catch (e: Exception) {
                println("Wasm Launcher: EXCEPTION -> ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
