package com.tuapp.libreta.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
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
import org.koin.dsl.module

actual val platformModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(LibretaAppDatabase.Schema, "libreta.db")
    }
    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) { flowType = FlowType.PKCE }
            install(Realtime)
        }
    }
    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }
    single<ProfileRepository>   { ProfileRepositoryImpl(get()) }
    single<ClassRoomRepository> { ClassRoomRepositoryImpl(get()) }
}
