package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.SettingsSessionStore
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import org.koin.dsl.module

actual val platformModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(LibretaAppDatabase.Schema.synchronous(), get(), "libreta_v2.db")
    }

    single<Settings> {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "libreta_secure_prefs",
            masterKeyAlias,
            get(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        SharedPreferencesSettings(sharedPreferences)
    }

    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                flowType                  = FlowType.PKCE
                scheme                    = "org.oriundo"
                host                      = "login-callback"
                defaultExternalAuthAction = ExternalAuthAction.ExternalBrowser
                sessionStore              = SettingsSessionStore(get())
            }
            install(Realtime)
        }
    }
    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }
}
