package com.tuapp.libreta.di

import com.tuapp.libreta.data.remote.*
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
    single { SupabaseAuthService(get()) }

    // ── Supabase repositories ─────────────────────────────────────────────────
    single<AttendanceRepository>       { SupabaseAttendanceDataSource(get()) }
    single<InvitationCodeRepository>   { SupabaseInvitationRepository(get()) }
    single<SchoolRepository>           { SupabaseSchoolRepository(get()) }
    single<CommunicationRepository>    { SupabaseCommunicationRepository(get()) }
    single<CourseAssignmentRepository> { SupabaseCourseAssignmentRepository(get()) }
    single<CoursesRepository>          { SupabaseCoursesRepository(get()) }

    // ── Local repositories (SQLDelight) ───────────────────────────────────────
    single<ProfileRepository>       { ProfileRepositoryImpl(get()) }
    single<ClassRoomRepository>     { ClassRoomRepositoryImpl(get()) }
    single<StudentRepository>       { SupabaseStudentRepository(get()) }
    single<JustificationRepository> { SupabaseJustificationRepository(get()) }
    single<MessageRepository>       { SupabaseMessageRepository(get()) }

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
    factory { GenerateInvitationCodeUseCase(get()) }
    factory { ClaimInvitationCodeUseCase(get()) }
    factory { GetTeacherInvitationsUseCase(get()) }

    // ── ScreenModels ──────────────────────────────────────────────────────────
    factory { LoginScreenModel(get()) }
    factory { RoleSelectionScreenModel(get(), get()) }
    factory { TeacherDashboardScreenModel(get(), get(), get(), get()) }
    factory { StudentListScreenModel(get(), get(), get()) }
    factory { ParentDashboardScreenModel(get(), get(), get(), get(), get()) }
    factory { ProfileScreenModel(get(), get(), get(), get()) }
    factory { MessageScreenModel(get(), get(), get(), get()) }
    factory { JustificationScreenModel(get(), get(), get(), get()) }
    factory { NoticeScreenModel(get(), get(), get()) }
    factory { StatsScreenModel(get()) }
}
