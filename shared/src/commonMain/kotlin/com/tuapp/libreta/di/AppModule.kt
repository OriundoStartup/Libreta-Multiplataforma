package com.tuapp.libreta.di

import com.tuapp.libreta.data.remote.SupabaseAttendanceDataSource
import com.tuapp.libreta.data.remote.SupabaseConfig
import com.tuapp.libreta.data.repository.*
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import com.tuapp.libreta.domain.repository.*
import com.tuapp.libreta.domain.usecase.*
import com.tuapp.libreta.presentation.*
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.koin.dsl.module

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────
    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }

    // ── Supabase ──────────────────────────────────────────────────────────────
    single {
        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.ANON_KEY
        ) {
            install(Postgrest)
            install(Auth)
        }
    }
    // Remote data source — intercambiable con el local sin tocar UseCases ni tests
    single<AttendanceRepository> { SupabaseAttendanceDataSource(get()) }

    // ── Repositories (local SQLDelight) ──────────────────────────────────────
    single<ProfileRepository>       { ProfileRepositoryImpl(get()) }
    single<ClassRoomRepository>     { ClassRoomRepositoryImpl(get()) }
    single<StudentRepository>       { StudentRepositoryImpl(get()) }
    // AttendanceRepository → bound above to SupabaseAttendanceDataSource
    single<JustificationRepository> { JustificationRepositoryImpl(get()) }
    single<MessageRepository>       { MessageRepositoryImpl(get()) }

    // ── UseCases ──────────────────────────────────────────────────────────────
    factory { GetStudentsByClassUseCase(get()) }
    factory { DeleteStudentUseCase(get()) }
    factory { GetCourseAnalyticsUseCase(get(), get()) }

    factory { SubmitJustificationUseCase(get()) }
    factory { GetPendingJustificationsUseCase(get()) }
    factory { ReviewJustificationUseCase(get(), get()) }

    factory { GetInboxUseCase(get()) }
    factory { GetConversationUseCase(get()) }
    factory { SendMessageUseCase(get()) }

    // ── ScreenModels ──────────────────────────────────────────────────────────
    factory { StudentListScreenModel(get(), get()) }
    factory { MessageScreenModel(get(), get(), get()) }
    factory { JustificationScreenModel(get(), get(), get()) }
    factory { ParentDashboardScreenModel(get(), get(), get()) }
    factory { StatsScreenModel(get()) }
}
