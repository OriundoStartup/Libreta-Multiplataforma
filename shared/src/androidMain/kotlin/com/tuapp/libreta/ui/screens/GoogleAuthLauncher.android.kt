package com.tuapp.libreta.ui.screens

import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseConfig
import org.koin.compose.koinInject

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? {
    val context = LocalContext.current
    val authService = koinInject<SupabaseAuthService>()
    return remember(context) {
        suspend {
            val url = authService.getGoogleOAuthUrl(redirectTo = SupabaseConfig.ANDROID_REDIRECT_URL)
            CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
        }
    }
}
