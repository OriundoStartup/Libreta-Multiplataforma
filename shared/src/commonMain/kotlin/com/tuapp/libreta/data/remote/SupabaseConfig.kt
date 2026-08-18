package com.tuapp.libreta.data.remote

import com.tuapp.libreta.BuildKonfig

object SupabaseConfig {
    // Sanitización proactiva para evitar 404 por trailing slashes en el intercambio PKCE
    val URL          get() = BuildKonfig.SUPABASE_URL.removeSuffix("/")
    val ANON_KEY     get() = BuildKonfig.SUPABASE_KEY
    val REDIRECT_URL get() = BuildKonfig.SUPABASE_REDIRECT_URL
    
    const val ANDROID_REDIRECT_URL = "org.oriundo://login-callback"
    const val IOS_REDIRECT_URL = "org.oriundo://login-callback"
}
