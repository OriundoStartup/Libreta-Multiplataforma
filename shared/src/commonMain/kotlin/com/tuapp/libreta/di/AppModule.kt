package com.tuapp.libreta.di

import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAttendanceDataSource
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseCommunicationRepository
import com.tuapp.libreta.data.remote.SupabaseCourseAssignmentRepository
import com.tuapp.libreta.data.remote.SupabaseCoursesRepository
import com.tuapp.libreta.data.remote.SupabaseInvitationRepository
import com.tuapp.libreta.data.remote.SupabaseJustificationRepository
import com.tuapp.libreta.data.remote.SupabaseMessageRepository
import com.tuapp.libreta.data.remote.SupabaseSchoolRepository
import com.tuapp.libreta.data.remote.SupabaseStudentRepository
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.CommunicationRepository
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import com.tuapp.libreta.domain.repository.InvitationCodeRepository
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.SchoolRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.usecase.ClaimInvitationCodeUseCase
import com.tuapp.libreta.domain.usecase.DeleteStudentUseCase
import com.tuapp.libreta.domain.usecase.GenerateInvitationCodeUseCase
import com.tuapp.libreta.domain.usecase.GetConversationUseCase
import com.tuapp.libreta.domain.usecase.GetCourseAnalyticsUseCase
import com.tuapp.libreta.domain.usecase.GetInboxUseCase
import com.tuapp.libreta.domain.usecase.GetPendingJustificationsUseCase
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import com.tuapp.libreta.domain.usecase.GetTeacherInvitationsUseCase
import com.tuapp.libreta.domain.usecase.MarkAsReadUseCase
import com.tuapp.libreta.domain.usecase.ReviewJustificationUseCase
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import com.tuapp.libreta.domain.usecase.SubmitJustificationUseCase
import com.tuapp.libreta.presentation.AttendanceHistoryScreenModel
import com.tuapp.libreta.presentation.AttendanceScreenModel
import com.tuapp.libreta.presentation.CourseEditScreenModel
import com.tuapp.libreta.presentation.EnrollmentScreenModel
import com.tuapp.libreta.presentation.JustificationScreenModel
import com.tuapp.libreta.presentation.LoginScreenModel
import com.tuapp.libreta.presentation.MessageScreenModel
import com.tuapp.libreta.presentation.NoticeListScreenModel
import com.tuapp.libreta.presentation.NoticeScreenModel
import com.tuapp.libreta.presentation.ParentDashboardScreenModel
import com.tuapp.libreta.presentation.ProfileScreenModel
import com.tuapp.libreta.presentation.RoleSelectionScreenModel
import com.tuapp.libreta.presentation.StatsScreenModel
import com.tuapp.libreta.presentation.StudentDetailScreenModel
import com.tuapp.libreta.presentation.StudentListScreenModel
import com.tuapp.libreta.presentation.TeacherDashboardScreenModel
import org.koin.dsl.module

val appModule = module {

    // ── Auth ──────────────────────────────────────────────────────────────────
    single { SupabaseAuthService(get()) }

    // ── Supabase repositories ─────────────────────────────────────────────────
    single<AttendanceRepository>       { SupabaseAttendanceDataSource(get()) }
    single<InvitationCodeRepository>   { SupabaseInvitationRepository(get()) }
    single<SchoolRepository>           { SupabaseSchoolRepository(get()) }
    single<CommunicationRepository>    { SupabaseCommunicationRepository(get()) }
    single<CourseAssignmentRepository> { SupabaseCourseAssignmentRepository(get()) }
    single<CoursesRepository>          { SupabaseCoursesRepository(get()) }
    single<StudentRepository>          { SupabaseStudentRepository(get()) }
    single<JustificationRepository>    { SupabaseJustificationRepository(get()) }
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
    factory { MessageScreenModel(get(), get(), get(), get(), get(), get()) }
    factory { JustificationScreenModel(get(), get(), get(), get()) }
    factory { NoticeScreenModel(get(), get(), get()) }
    factory { NoticeListScreenModel(get()) }
    factory { StatsScreenModel(get()) }
    factory { (params: org.koin.core.parameter.ParametersHolder) -> AttendanceScreenModel(get(), get(), params.get(), params.get()) }
    factory { (params: org.koin.core.parameter.ParametersHolder) -> AttendanceHistoryScreenModel(get(), params.get<String>(), params.get<String>()) }
    factory { (params: org.koin.core.parameter.ParametersHolder) -> StudentDetailScreenModel(get(), get(), params.get<String>(), params.get<String>(), params.get<String>(), params.get<String>()) }
    factory { EnrollmentScreenModel(get()) }
    factory { CourseEditScreenModel(get()) }
}
