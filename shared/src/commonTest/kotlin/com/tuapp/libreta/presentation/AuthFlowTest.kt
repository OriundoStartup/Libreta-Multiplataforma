package com.tuapp.libreta.presentation

import kotlin.test.Ignore

import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.domain.model.UserRole
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthFlowTest {

    private fun createAuthStatus(userId: String, role: UserRole? = null): SessionStatus.Authenticated {
        // Mock de UserInfo - Usualmente se puede instanciar con nulls para campos no usados
        // o usar una instancia delegada si la librería lo permite.
        // Aquí simulamos lo mínimo necesario.
        val fakeUser = UserInfo(
            id = userId,
            aud = "",
            email = "test@example.com",
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
            appMetadata = null,
            userMetadata = null,
            identities = emptyList()
        )
        return SessionStatus.Authenticated(fakeUser, role)
    }

    @Test
    fun `Loading status returns Loading flow`() {
        val flow = AuthFlow.from(SessionStatus.Loading, ScreenKind.LOGIN)
        assertEquals(AuthFlow.Loading, flow)
    }

    @Test
    fun `NotAuthenticated on Login screen returns Stay`() {
        val flow = AuthFlow.from(SessionStatus.NotAuthenticated, ScreenKind.LOGIN)
        assertEquals(AuthFlow.Stay, flow)
    }

    @Test
    fun `NotAuthenticated on other screen returns LoginRequired`() {
        val flow = AuthFlow.from(SessionStatus.NotAuthenticated, ScreenKind.OTHER)
        assertEquals(AuthFlow.LoginRequired, flow)
    }

    @Test
    fun `Authenticated without role on Login returns NeedsRole`() {
        val flow = AuthFlow.from(createAuthStatus("123"), ScreenKind.LOGIN)
        assertEquals(AuthFlow.NeedsRole("123"), flow)
    }

    @Test
    fun `Authenticated without role on RoleSelection returns Stay`() {
        val flow = AuthFlow.from(createAuthStatus("123"), ScreenKind.ROLE_SELECTION)
        assertEquals(AuthFlow.Stay, flow)
    }

    @Test
    fun `Authenticated with role on RoleSelection without switching returns Ready`() {
        val flow = AuthFlow.from(createAuthStatus("123", UserRole.TEACHER), ScreenKind.ROLE_SELECTION, isSwitchingRole = false)
        assertEquals(AuthFlow.Ready(UserRole.TEACHER, "123"), flow)
    }

    @Test
    fun `Authenticated with role on RoleSelection while switching returns Stay`() {
        val flow = AuthFlow.from(createAuthStatus("123", UserRole.TEACHER), ScreenKind.ROLE_SELECTION, isSwitchingRole = true)
        assertEquals(AuthFlow.Stay, flow)
    }

    @Test
    fun `Teacher on Parent Home returns Forbidden`() {
        val flow = AuthFlow.from(createAuthStatus("123", UserRole.TEACHER), ScreenKind.PARENT_HOME)
        assertEquals(AuthFlow.Forbidden(UserRole.TEACHER, "123"), flow)
    }

    @Test
    fun `Parent on Teacher Home returns Forbidden`() {
        val flow = AuthFlow.from(createAuthStatus("123", UserRole.PARENT), ScreenKind.TEACHER_HOME)
        assertEquals(AuthFlow.Forbidden(UserRole.PARENT, "123"), flow)
    }

    @Test
    fun `Teacher on Teacher Home returns Stay`() {
        val flow = AuthFlow.from(createAuthStatus("123", UserRole.TEACHER), ScreenKind.TEACHER_HOME)
        assertEquals(AuthFlow.Stay, flow)
    }
}
