package com.tuapp.libreta.di

import com.tuapp.libreta.data.db.LocalDataBridge
import com.tuapp.libreta.data.remote.SupabaseStudentRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseCommunicationRepository
import com.tuapp.libreta.data.remote.SupabaseCourseAssignmentRepository
import com.tuapp.libreta.data.remote.SupabaseCoursesRepository
import com.tuapp.libreta.data.remote.SupabaseInvitationRepository
import com.tuapp.libreta.data.remote.SupabaseMessageRepository
import com.tuapp.libreta.data.remote.SupabaseSchoolRepository
import com.tuapp.libreta.data.repository.ClassRoomRepositoryImpl
import com.tuapp.libreta.data.repository.GradeRepositoryImpl
import com.tuapp.libreta.data.repository.JustificationRepositoryImpl
import com.tuapp.libreta.data.repository.ProfileRepositoryImpl
import com.tuapp.libreta.data.repository.StudentRepositoryImpl
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.sync.SymbioticAttendanceRepository
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.CommunicationRepository
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import com.tuapp.libreta.domain.repository.GradeRepository
import com.tuapp.libreta.domain.repository.InvitationCodeRepository
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.ProfileRepository
import com.tuapp.libreta.domain.repository.SchoolRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.usecase.*
import com.tuapp.libreta.presentation.*
import org.koin.dsl.module

val appModule = module {

    // ── Database Bridge ──────────────────────────────────────────────────────
    single { LocalDataBridge(get(), get(), get()) }

    // ── Sync ──────────────────────────────────────────────────────────────────
    single { SyncManager(get(), get(), get()) }

    // ── Auth ──────────────────────────────────────────────────────────────────
    single { SupabaseAuthService(get(), get()) }

    // ── Repositories (Symbiosis: Local + Remote) ─────────────────────────────
    single<AttendanceRepository>       { SymbioticAttendanceRepository(get(), get()) }
    single<StudentRepository>          { StudentRepositoryImpl(get(), get(), get()) }
    single<JustificationRepository>    { com.tuapp.libreta.data.remote.SupabaseJustificationRepository(get()) }
    single<ProfileRepository>          { ProfileRepositoryImpl(get(), get()) }
    single<ClassRoomRepository>        { ClassRoomRepositoryImpl(get(), get()) }
    single<GradeRepository>            { GradeRepositoryImpl(get(), get()) }

    // ── Supabase direct repositories ──────────────────────────────────────────
    single<InvitationCodeRepository>   { SupabaseInvitationRepository(get()) }
    single<SchoolRepository>           { SupabaseSchoolRepository(get()) }
    single<CommunicationRepository>    { SupabaseCommunicationRepository(get()) }
    single<CourseAssignmentRepository> { SupabaseCourseAssignmentRepository(get()) }
    single<com.tuapp.libreta.data.remote.CoursesRepository> { SupabaseCoursesRepository(get()) }
    single<MessageRepository>          { SupabaseMessageRepository(get()) }

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
    factory { MarkAsReadUseCase(get()) }
    factory { ObserveConversationUseCase(get()) }
    factory { GenerateInvitationCodeUseCase(get()) }
    factory { ClaimInvitationCodeUseCase(get()) }
    factory { GetTeacherInvitationsUseCase(get()) }
    factory { GetConsolidatedReportUseCase(get(), get()) }
    factory { GetGlobalStatsUseCase(get()) }
    factory { GetStudentGradesUseCase(get()) }
    factory { SaveGradeUseCase(get()) }
    factory { DeleteGradeUseCase(get()) }

    // ── ScreenModels ──────────────────────────────────────────────────────────
    factory { LoginScreenModel(get()) }
    factory { RoleSelectionScreenModel(get(), get(), get(), get()) }
    factory { TeacherDashboardScreenModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { StudentListScreenModel(get(), get(), get(), get(), get()) }
    factory { ParentDashboardScreenModel(get(), get(), get(), get(), get(), get()) }
    factory { ProfileScreenModel(get(), get(), get(), get()) }
    factory { MessageScreenModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { JustificationScreenModel(get(), get(), get(), get()) }
    factory { ParentStudentDetailScreenModel(get()) }
    factory { JustificationListScreenModel(get()) }
    factory { NotificationScreenModel(get(), get(), get()) }
    factory { GlobalJustificationScreenModel(get(), get(), get()) }
    factory { NoticeScreenModel(get(), get(), get(), get(), get(), get()) }
    factory { NoticeListScreenModel(get()) }
    factory { StatsScreenModel(get()) }
    factory { GradeScreenModel(get(), get(), get()) }
    factory { params -> AttendanceScreenModel(get(), get(), params.get(), params.get()) }
    factory { params -> AttendanceHistoryScreenModel(get(), params.get<String>(), params.get<String>()) }
    factory { params ->
        StudentDetailScreenModel(get(), get(), get(), get(), get(), params.get(), params.get(), params.get(), params.get()) 
    }
    factory { EnrollmentScreenModel(get()) }
    factory { CourseDashboardScreenModel(get(), get(), get(), get(), get()) }
    factory { params -> MassiveGradeScreenModel(get(), get(), params.get()) }
    factory { CourseEditScreenModel(get()) }
    factory { ReportScreenModel(get()) }
}
