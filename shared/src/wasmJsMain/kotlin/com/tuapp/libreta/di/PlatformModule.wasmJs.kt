package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.repository.ClassRoomRepositoryImpl
import com.tuapp.libreta.data.repository.ProfileRepositoryImpl
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.repository.ProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module
import org.w3c.dom.Worker

actual val platformModule = module {
    single<SqlDriver> {
        try {
            // Intentar usar Worker para persistencia real
            WebWorkerDriver(Worker("sqldelight-worker.js"))
        } catch (e: Throwable) {
            println("Wasm DB: Worker failed to start, falling back to dummy/error. ${e.message}")
            // Si el worker falla en local, lanzamos el error para verlo en el HTML que configuramos
            throw RuntimeException("Fallo al cargar la base de datos (Worker no encontrado). Revisa si sqldelight-worker.js existe.")
        }
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
                // En Wasm/JS, el SessionManager se instala solo y usa LocalStorage por defecto
                // pero nos aseguramos de que el esquema de redirección sea el de producción
                val redirectUrl = SupabaseConfig.REDIRECT_URL
                if (redirectUrl.startsWith("http")) {
                   // Supabase kt lo parsea automáticamente si le pasas la URL base
                }
            }
            install(Realtime)
            install(Storage)
        }
    }
}
