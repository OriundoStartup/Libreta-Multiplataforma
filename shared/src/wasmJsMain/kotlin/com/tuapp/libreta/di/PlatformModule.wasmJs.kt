package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.postgrest.Postgrest
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
            val errorMsg = "Wasm DB: Worker failed to start. ${e.message}"
            println(errorMsg)
            dbReady.completeExceptionally(RuntimeException(errorMsg))
            throw RuntimeException(errorMsg)
        }
        
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Secuenciación Real: Esperamos a que el Worker confirme la creación del esquema.
                LibretaAppDatabase.Schema.create(driver).await() 
                println("Wasm DB: Esquema verificado/creado exitosamente.")

                // Log de depuración para listar tablas reales creadas
                driver.executeQuery(null, "SELECT name FROM sqlite_master WHERE type='table'", { cursor ->
                    app.cash.sqldelight.db.QueryResult.AsyncValue {
                        val tables = mutableListOf<String>()
                        while (cursor.next().await()) {
                            tables.add(cursor.getString(0) ?: "")
                        }
                        println("Wasm DB: Tablas detectadas -> ${tables.joinToString(", ")}")
                    }
                }, 0).await()

                dbReady.complete(Unit)
            } catch (e: Throwable) {
                println("Wasm DB: Error crítico inicializando tablas: ${e.message}")
                // Evitamos carga infinita: los consumidores de dbReady recibirán el fallo.
                dbReady.completeExceptionally(e)
            }
        }
        driver
    }

    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { get<LibretaAppDatabase>().syncMetadataQueries }
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
            }
            install(Realtime)
            install(Storage)
        }
    }
}
