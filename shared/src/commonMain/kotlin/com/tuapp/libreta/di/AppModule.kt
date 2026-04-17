package com.tuapp.libreta.di

import com.tuapp.libreta.data.repository.*
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.db.LibretaAppDatabase
import com.tuapp.libreta.domain.repository.*
import com.tuapp.libreta.domain.usecase.*
import com.tuapp.libreta.presentation.*
import org.koin.dsl.module

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────
    single { LibretaAppDatabase(get()) }
    single { get<LibretaAppDatabase>().libretaAppQueries }
    single { DataSeeder(get()) }

    // ── Repositories ──────────────────────────────────────────────────────────
    single<ProfileRepository>       { ProfileRepositoryImpl(get()) }
    single<ClassRoomRepository>     { ClassRoomRepositoryImpl(get()) }
    single<StudentRepository>       { StudentRepositoryImpl(get()) }
    single<AttendanceRepository>    { AttendanceRepositoryImpl(get()) }
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
