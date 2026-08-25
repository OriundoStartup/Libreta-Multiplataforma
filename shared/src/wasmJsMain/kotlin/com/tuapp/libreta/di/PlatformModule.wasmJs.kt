package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlPreparedStatement
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
import kotlinx.coroutines.delay
import org.koin.dsl.module
import org.w3c.dom.Worker

// Guardián de inicialización para evitar "no such table" en Wasm
actual val dbReady = CompletableDeferred<Unit>()
private var isInitializing = false

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
        
        if (!isInitializing) {
            isInitializing = true
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // 1. Interceptar y ejecutar creación
                    val results = mutableListOf<QueryResult<*>>()
                    val interceptor = object : SqlDriver by driver {
                        override fun execute(
                            identifier: Int?,
                            sql: String,
                            parameters: Int,
                            binders: (SqlPreparedStatement.() -> Unit)?
                        ): QueryResult<Long> {
                            val r = driver.execute(identifier, sql, parameters, binders)
                            results.add(r)
                            return r
                        }
                    }

                    LibretaAppDatabase.Schema.create(interceptor)
                    
                    // 2. Esperar confirmaciones
                    results.forEach { it.await() }
                    
                    delay(200)

                    // 3. Verificación final (Log mínimo)
                    driver.executeQuery(null, "SELECT name FROM sqlite_master WHERE type='table'", { cursor ->
                        QueryResult.AsyncValue {
                            val tables = mutableListOf<String>()
                            while (cursor.next().await()) {
                                val name = cursor.getString(0) ?: ""
                                if (!name.startsWith("sqlite_")) tables.add(name)
                            }
                            if (tables.isEmpty()) throw IllegalStateException("DB Schema creation failed")
                            AppLogger.d("WasmDB", "Schema initialized with ${tables.size} tables.")
                        }
                    }, 0).await()

                    dbReady.complete(Unit)
                } catch (e: Throwable) {
                    AppLogger.e("WasmDB", "Critical initialization error: ${e.message}")
                    dbReady.completeExceptionally(e)
                }
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
        createSupabaseClient(supabaseUrl = baseUrl, supabaseKey = SupabaseConfig.ANON_KEY) {
            install(Postgrest)
            install(Auth) { flowType = FlowType.PKCE }
            install(Realtime)
            install(Storage)
        }
    }
}
