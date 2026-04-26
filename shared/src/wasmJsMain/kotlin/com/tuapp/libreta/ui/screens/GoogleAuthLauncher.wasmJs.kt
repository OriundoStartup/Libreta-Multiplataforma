package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.AppLogger
import kotlinx.browser.window
import org.koin.compose.koinInject

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val authService = koinInject<SupabaseAuthService>()
    return remember {
        suspend {
            try {
                val url = authService.getGoogleOAuthUrl()
                if (url.startsWith("https://")) {
                    window.location.href = url
                } else {
                    AppLogger.e("GoogleAuthLauncher", "OAuth URL no es HTTPS: $url")
                }
            } catch (e: Exception) {
                AppLogger.e("GoogleAuthLauncher", "Error al obtener URL OAuth: ${e.message}")
            }
        }
    }
}
