package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseConfig
import org.koin.compose.koinInject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val authService = koinInject<SupabaseAuthService>()
    return remember(authService) {
        suspend {
            // Construye la URL OAuth con el redirect del scheme registrado y la abre en Safari.
            // Tras el login, Safari redirige a org.oriundo://login-callback y iOSApp.swift
            // (.onOpenURL) entrega el callback a handleDeepLink().
            val url = authService.getGoogleOAuthUrl(redirectTo = SupabaseConfig.IOS_REDIRECT_URL)
            NSURL.URLWithString(url)?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(nsUrl)
            }
        }
    }
}
