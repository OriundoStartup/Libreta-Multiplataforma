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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import org.w3c.dom.Worker

// Guardián de inicialización para evitar "no such table" en Wasm
actual val dbReady = CompletableDeferred<Unit>()

actual val platformModule = module {
    single<SqlDriver> {
        val driver = try {
            WebWorkerDriver(Worker("sqldelight-worker.js"))
        } catch (e: Throwable) {
            println("Wasm DB: Worker failed to start. ${e.message}")
            throw RuntimeException("Fallo al cargar la base de datos (Worker no encontrado). Revisa si sqldelight-worker.js existe.")
        }
        
        CoroutineScope(Dispatchers.Default).launch {
            runCatching { 
                LibretaAppDatabase.Schema.create(driver).await() 
                println("Wasm DB: Esquema creado exitosamente.")
                dbReady.complete(Unit)
            }.onFailure { 
                println("Wasm DB: error creando esquema: ${it.message}")
                // Si ya existe, también marcamos como listo
                if (it.message?.contains("already exists") == true) {
                    dbReady.complete(Unit)
                }
            }
        }
        driver
    }

    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }

    single {
        val baseUrl = SupabaseConfig.URL.removeSuffix("/")
        println("Supabase Client: Inicializando con URL=$baseUrl")
        
        createSupabaseClient(
            supabaseUrl = baseUrl,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                flowType = FlowType.PKCE
                
                // Sanitización del REDIRECT_URL para evitar errores de construcción de URL
                val cleanRedirect = SupabaseConfig.REDIRECT_URL.removeSuffix("/")
                if (cleanRedirect.isNotBlank()) {
                    host = cleanRedirect.removePrefix("https://").removePrefix("http://").split("/").firstOrNull()
                    scheme = if (cleanRedirect.startsWith("https")) "https" else "http"
                }
                
                AppLogger.d("SupabaseAuth", "Configurado PKCE con host=$host, scheme=$scheme")
            }
            install(Realtime)
            install(Storage)
        }
    }
}
