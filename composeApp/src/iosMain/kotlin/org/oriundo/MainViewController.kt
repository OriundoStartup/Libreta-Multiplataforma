package org.oriundo

import androidx.compose.ui.window.ComposeUIViewController
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.core.context.GlobalContext
import platform.Foundation.NSURL

fun MainViewController() = ComposeUIViewController(
    configure = {
        com.tuapp.libreta.initKoin()
    }
) { App() }

/**
 * Puente llamado desde iOSApp.swift (.onOpenURL) cuando Safari devuelve el control
 * tras el login OAuth con la URL org.oriundo://login-callback#...
 * Entrega la URL a Supabase para que complete el flujo PKCE y guarde la sesión.
 */
fun handleDeepLink(url: String) {
    val nsUrl = NSURL(string = url)
    val supabase = GlobalContext.get().get<SupabaseClient>()
    supabase.handleDeeplinks(nsUrl)
}
