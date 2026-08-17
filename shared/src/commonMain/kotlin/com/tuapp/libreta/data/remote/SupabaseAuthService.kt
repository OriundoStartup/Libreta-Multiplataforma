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
import io.github.jan.supabase.postgrest.from
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

    fun refreshProfile() {
        _profileRefreshTrigger.value += 1
    }

    // CAPA 2: Estado de cache y marca de tiempo para blindaje temporal
    private var _cachedRole: UserRole? = null
    private var _lastUpdateTimestamp: Long = 0

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionStatusFlow: Flow<SessionStatus> = kotlinx.coroutines.flow.combine(
        supabase.auth.sessionStatus,
        _profileRefreshTrigger
    ) { status, trigger -> status to trigger }
        .flatMapLatest { (status, _) ->
            kotlinx.coroutines.flow.flow {
                AppLogger.d("AuthService", "Processing session status: $status")
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        // Invalida explícitamente el cache en desconexión
                        _cachedRole = null
                        _lastUpdateTimestamp = 0
                        emit(SessionStatus.NotAuthenticated)
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.Initializing -> {
                        emit(SessionStatus.Loading)
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val session = status.session
                        val user = session.user
                        if (user != null) {
                            // Emisión optimista inmediata desde caché para evitar parpadeos
                            _cachedRole?.let { 
                                AppLogger.d("AuthService", "Emitiendo desde CACHE: $it")
                                emit(SessionStatus.Authenticated(user, it)) 
                            }
                            
                            val role = try {
                                kotlinx.coroutines.withTimeout(4000) { 
                                    val dbRole = getUserRole(user.id)
                                    val now = currentEpochMs()
                                    
                                    // CAPA 2 REFINADA: Solo ignora null si estamos dentro de la ventana de 5s 
                                    // tras un updateRole exitoso. Un null fuera de esta ventana es legítimo.
                                    if (dbRole == null && _cachedRole != null && (now - _lastUpdateTimestamp) < 5000) {
                                        AppLogger.d("AuthService", "Capa 2: Ignorando null transitorio (ventana de 5s activa).")
                                        _cachedRole
                                    } else {
                                        dbRole
                                    }
                                }
                            } catch (e: Exception) {
                                AppLogger.e("AuthService", "Error al obtener rol: ${e.message}")
                                _cachedRole // Revertir al caché si falla la red/timeout
                            }
                            
                            _cachedRole = role
                            AppLogger.d("AuthService", "Emisión FINAL sessionStatusFlow: role=$role")
                            emit(SessionStatus.Authenticated(user, role))
                        } else {
                            emit(SessionStatus.NotAuthenticated)
                        }
                    }
                    else -> emit(SessionStatus.NotAuthenticated)
                }
            }
        }.distinctUntilChanged()

    fun getGoogleOAuthUrl(redirectTo: String? = null, prompt: String? = null): String {
        val url = supabase.auth.getOAuthUrl(
            provider = Google,
            redirectUrl = redirectTo
        )
        return if (prompt != null) {
            val separator = if (url.contains("?")) "&" else "?"
            "$url${separator}prompt=$prompt"
        } else {
            url
        }
    }

    suspend fun signInWithGoogle(redirectUrl: String? = null) = supabase.auth.signInWith(
        provider = Google,
        redirectUrl = redirectUrl ?: if (SupabaseConfig.REDIRECT_URL.isNotBlank()) SupabaseConfig.REDIRECT_URL else null
    ) {
        queryParams["prompt"] = "select_account"
    }

    suspend fun getProfile(userId: String): ProfileSupabaseDto? {
        return try {
            supabase.from("profiles")
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
        val roleStr = profile?.role ?: return null
        
        return try {
            UserRole.valueOf(roleStr.uppercase().trim())
        } catch (e: Exception) {
            AppLogger.e("AuthService", "Rol inválido en BD: '$roleStr' para usuario $userId")
            null
        }
    }

    suspend fun hasRoleSet(): Boolean {
        val uid = currentUserId()?.value ?: return false
        return getUserRole(uid) != null
    }

    suspend fun updateRole(role: UserRole) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: throw Exception("No hay sesión activa: updateRole falló")
        
        AppLogger.d("AuthService", "Iniciando UPDATE atómico de rol para $uid -> ${role.name}")
        
        runCatching {
            val updateData = mapOf("role" to role.name)

            // CAPA 1: Atomicidad usando .select() en el update
            val response = supabase.from("profiles").update(updateData) {
                filter { eq("id", uid) }
                select()
            }.decodeSingleOrNull<ProfileSupabaseDto>()
                ?: throw Exception("Error de consistencia: El perfil no existe en la base de datos.")
            
            val confirmedRole = response.role?.let { 
                UserRole.valueOf(it.uppercase().trim()) 
            } ?: throw Exception("El servidor devolvió un rol nulo inesperado tras el update.")
            
            confirmedRole
        }.onSuccess { confirmedRole ->
            AppLogger.d("AuthService", "Rol confirmado atómicamente por el servidor: $confirmedRole")
            
            // Actualización de estado para la ventana de blindaje (Capa 2)
            _cachedRole = confirmedRole
            _lastUpdateTimestamp = currentEpochMs()
            
            refreshProfile()
        }.onFailure { e ->
            AppLogger.e("AuthService", "Error al actualizar rol: ${e.message}")
            throw e
        }
    }

    suspend fun claimInvitationCode(code: String) {
        val uid = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.from("invitation_codes").update({ set("claimed_by", uid) }) {
            filter { eq("code", code.uppercase()) }
        }
    }

    suspend fun validateInvitationCode(code: String): Boolean = runCatching {
        val result = supabase.from("invitation_codes")
            .select { filter { eq("code", code.uppercase()) } }
            .decodeList<InvitationCodeCheck>()
        
        if (result.isEmpty()) return false
        
        val check = result.first()
        val now = currentEpochMs()
        val expiresMs = runCatching { Instant.parse(check.expiresAt).toEpochMilliseconds() }.getOrElse { Long.MAX_VALUE }
        
        check.claimedBy == null && expiresMs > now
    }.getOrElse { false }

    suspend fun signOut() {
        // Confirmación de invalidación de caché
        _cachedRole = null
        _lastUpdateTimestamp = 0
        AppLogger.d("AuthService", "Cache invalidado por SignOut.")
        supabase.auth.signOut()
    }

    fun isLoggedIn(): Boolean = supabase.auth.currentUserOrNull() != null

    fun currentUser(): UserInfo? = supabase.auth.currentUserOrNull()

    fun currentUserId(): UuidString? {
        val rawId = supabase.auth.currentUserOrNull()?.id ?: return null
        return rawId.toUuidOrNull()
    }
}
