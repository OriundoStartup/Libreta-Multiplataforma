package com.tuapp.libreta.di

import com.tuapp.libreta.BuildKonfig
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.repository.InMemoryClassRoomRepository
import com.tuapp.libreta.data.repository.InMemoryProfileRepository
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.repository.ProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.browser.window
import org.koin.dsl.module

actual val platformModule = module {
    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                flowType = FlowType.PKCE
            }
            install(Realtime)
        }
    }
    single<ProfileRepository>   { InMemoryProfileRepository() }
    single<ClassRoomRepository> { InMemoryClassRoomRepository() }
}