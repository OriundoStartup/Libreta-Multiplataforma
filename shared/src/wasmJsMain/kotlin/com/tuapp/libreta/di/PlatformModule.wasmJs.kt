package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.w3c.dom.Worker

actual val platformModule = module {
    single<SqlDriver> {
        val driver = try {
            WebWorkerDriver(Worker("sqldelight-worker.js"))
        } catch (e: Throwable) {
            println("Wasm DB: Worker failed to start. ${e.message}")
            // Si el worker falla en local, lanzamos el error para verlo en el HTML que configuramos
            throw RuntimeException("Fallo al cargar la base de datos (Worker no encontrado). Revisa si sqldelight-worker.js existe.")
        }
        // A diferencia de los drivers nativos, el WebWorkerDriver NO crea el esquema
        // automáticamente. Hay que crearlo (operación asíncrona del driver web).
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { LibretaAppDatabase.Schema.create(driver).await() }
                .onFailure { println("Wasm DB: error creando esquema: ${it.message}") }
        }
        driver
    }

    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }

    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                flowType = FlowType.PKCE
                // IMPORTANTE: En Web/Wasm, la SDK maneja la persistencia en LocalStorage.
                // Si el modo incógnito bloquea LocalStorage, la sesión no persistirá tras refrescar.
                // Sin embargo, el login inicial debería funcionar si el intercambio de code tiene éxito.
                
                // Aseguramos que la URL de redirección base sea la correcta de producción si está definida
                host = SupabaseConfig.REDIRECT_URL.removePrefix("https://").removePrefix("http://").split("/").firstOrNull()
                scheme = if (SupabaseConfig.REDIRECT_URL.startsWith("https")) "https" else "http"
            }
            install(Realtime)
            install(Storage)
        }
    }
}
