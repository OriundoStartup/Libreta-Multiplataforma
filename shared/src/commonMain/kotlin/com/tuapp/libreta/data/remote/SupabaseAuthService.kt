package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.ProfileSupabaseDto
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ProfileRole(val role: String? = null)

@Serializable
private data class InvitationCodeCheck(
    @kotlinx.serialization.SerialName("claimed_by") val claimedBy: String? = null,
    @kotlinx.serialization.SerialName("expires_at") val expiresAt: String = ""
)

sealed class SessionStatus {
    data object NotAuthenticated : SessionStatus()
    data object Loading : SessionStatus()
    data class Authenticated(val user: UserInfo, val role: UserRole? = null) : SessionStatus()
    
    fun isAuthenticated(): Boolean = this is Authenticated
}

class SupabaseAuthService(private val supabase: SupabaseClient) {

    private val jsonHelper = Json { ignoreUnknownKeys = true }

    val isLoggedInFlow: Flow<Boolean> = supabase.auth.sessionStatus
        .map { it is io.github.jan.supabase.auth.status.SessionStatus.Authenticated }
        .distinctUntilChanged()

    private val _profileRefreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionStatusFlow: Flow<SessionStatus> = kotlinx.coroutines.flow.combine(
        supabase.auth.sessionStatus,
        _profileRefreshTrigger
    ) { status, _ -> status }.flatMapLatest { status ->
        kotlinx.coroutines.flow.flow {
            when (status) {
                is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> emit(SessionStatus.NotAuthenticated)
                is io.github.jan.supabase.auth.status.SessionStatus.Initializing -> emit(SessionStatus.Loading)
                is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user != null) {
                        emit(SessionStatus.Loading) // Mostrar loading mientras buscamos el rol
                        val role = getUserRole(user.id)
                        emit(SessionStatus.Authenticated(user, role))
                    } else {
                        emit(SessionStatus.NotAuthenticated)
                    }
                }
                else -> emit(SessionStatus.NotAuthenticated)
            }
        }
    }.distinctUntilChanged()

    fun getGoogleOAuthUrl(redirectTo: String? = null): String {
        return supabase.auth.getOAuthUrl(
            provider = Google,
            redirectUrl = redirectTo
        )
    }

    suspend fun signInWithGoogle() = supabase.auth.signInWith(Google)

    suspend fun getProfile(userId: String): ProfileSupabaseDto? {
        return try {
            supabase.postgrest["profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileSupabaseDto>()
        } catch (e: Exception) {
            AppLogger.e("getProfile", "Error al obtener perfil para $userId: ${e.message}")
            null
        }
    }

    /**
     * Obtiene el rol del usuario desde la tabla 'profiles'.
     * Retorna null si el perfil no existe o el rol no está definido.
     */
    suspend fun getUserRole(userId: String): UserRole? {
        val profile = getProfile(userId)
        return profile?.role?.let { roleStr ->
            runCatching { UserRole.valueOf(roleStr) }.getOrNull()
        }
    }

    suspend fun hasRoleSet(): Boolean {
        val uid = currentUserId()?.value ?: return false
        return getUserRole(uid) != null
    }

    suspend fun updateRole(role: UserRole, courseId: String? = null) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: run {
            AppLogger.e("updateRole", "Error: No hay usuario autenticado")
            return
        }
        
        AppLogger.d("updateRole", "Iniciando UPDATE para UID: $uid con Rol: ${role.name}")
        
        runCatching {
            val updateData = mutableMapOf<String, String?>(
                "role" to role.name
            )
            if (courseId != null) {
                updateData["course_id"] = courseId
            }

            supabase.postgrest["profiles"].update(updateData) {
                filter { eq("id", uid) }
            }
        }.onSuccess {
            AppLogger.d("updateRole", "Update EXITOSO para $uid")
            
            // Forzar actualización del flujo de sesión
            _profileRefreshTrigger.value += 1
            
            // Verificación inmediata
            val check = getUserRole(uid)
            AppLogger.d("updateRole", "Verificación post-update: rol en BD = ${check?.name}")
        }.onFailure { e ->
            AppLogger.e("updateRole", "ERROR CRÍTICO en update: ${e.message}", e)
        }
    }

    suspend fun claimInvitationCode(code: String) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.postgrest["invitation_codes"].update({ set("claimed_by", uid) }) {
            filter { eq("code", code.uppercase()) }
        }
    }

    suspend fun validateInvitationCode(code: String): Boolean = runCatching {
        val result = supabase.postgrest["invitation_codes"]
            .select { filter { eq("code", code.uppercase()) } }
            .decodeList<InvitationCodeCheck>()
        
        if (result.isEmpty()) return false
        
        val check = result.first()
        val now = currentEpochMs()
        val expiresMs = runCatching { Instant.parse(check.expiresAt).toEpochMilliseconds() }.getOrElse { Long.MAX_VALUE }
        
        check.claimedBy == null && expiresMs > now
    }.getOrElse { false }

    suspend fun signOut() = supabase.auth.signOut()

    fun isLoggedIn(): Boolean = supabase.auth.currentUserOrNull() != null

    fun currentUser(): UserInfo? = supabase.auth.currentUserOrNull()

    fun currentUserId(): UuidString? {
        val rawId = supabase.auth.currentUserOrNull()?.id ?: return null
        return rawId.toUuidOrNull()
    }
}
