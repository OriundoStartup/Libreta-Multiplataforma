package com.tuapp.libreta.data.remote

import com.tuapp.libreta.BuildKonfig

object SupabaseConfig {
    // IMPORTANTE: En Web/Wasm, estas claves son visibles en el cliente.
    // Usa siempre la 'anon public key', nunca la 'service_role key'.
    val URL          get() = BuildKonfig.SUPABASE_URL
    val ANON_KEY     get() = BuildKonfig.SUPABASE_KEY
    val REDIRECT_URL get() = BuildKonfig.SUPABASE_REDIRECT_URL
    
    const val ANDROID_REDIRECT_URL = "org.oriundo://login-callback"
}
