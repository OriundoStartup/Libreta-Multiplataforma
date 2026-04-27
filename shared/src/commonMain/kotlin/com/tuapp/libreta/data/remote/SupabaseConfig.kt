package com.tuapp.libreta.data.remote

import com.tuapp.libreta.BuildKonfig

object SupabaseConfig {
    val URL          get() = BuildKonfig.SUPABASE_URL
    val ANON_KEY     get() = BuildKonfig.SUPABASE_KEY
    val REDIRECT_URL get() = BuildKonfig.SUPABASE_REDIRECT_URL
}
