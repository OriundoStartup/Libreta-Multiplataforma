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
import kotlinx.serialization.json.JsonPrimitive

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

class SupabaseAuthService(
    private val supabase: SupabaseClient,
    private val syncManager: com.tuapp.libreta.data.sync.SyncManager
) {

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
    private var _cachedUserId: String? = null
    private var _lastUpdateTimestamp: Long = 0

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sessionStatusFlow: Flow<SessionStatus> = kotlinx.coroutines.flow.combine(
        supabase.auth.sessionStatus,
        _profileRefreshTrigger
    ) { status, trigger -> status to trigger }
        .flatMapLatest { (status, _) ->
            kotlinx.coroutines.flow.flow {
                // Log redactado para producción: No exponemos el objeto status/session completo
                AppLogger.d("AuthService", "Processing session status update.")
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        // Invalida explícitamente el cache en desconexión
                        invalidateCache()
                        emit(SessionStatus.NotAuthenticated)
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.Initializing -> {
                        emit(SessionStatus.Loading)
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val session = status.session
                        val user = session.user
                        if (user != null) {
                            // FASE 6 — Detección de cambio de usuario (Prevención de fuga de datos)
                            if (_cachedUserId != null && _cachedUserId != user.id) {
                                AppLogger.w("AuthService", "CAMBIO DE USUARIO DETECTADO sin logout previo. Limpiando estado.")
                                invalidateCache()
                                // Forzamos un estado de carga mientras limpiamos la DB
                                emit(SessionStatus.Loading)
                                
                                runCatching { 
                                    syncManager.clearAllLocalData() 
                                }.onFailure { e ->
                                    AppLogger.e("AuthService", "Error en limpieza local (procediendo de todos modos): ${e.message}")
                                }
                                
                                AppLogger.d("AuthService", "Limpieza completada. Obteniendo rol para nuevo usuario.")
                            }
                            
                            // Emisión optimista inmediata SOLO si el usuario es el mismo
                            if (_cachedUserId == user.id && _cachedRole != null) {
                                emit(SessionStatus.Authenticated(user, _cachedRole))
                            }
                            
                            // Si no hay rol en caché (ej. tras limpieza), forzamos Loading mientras consultamos la DB
                            if (_cachedRole == null) {
                                AppLogger.d("AuthService", "Cache vacío para ${user.email}, consultando...")
                                emit(SessionStatus.Loading)
                            }
                            
                            val role = try {
                                kotlinx.coroutines.withTimeout(5000) { 
                                    val dbRole = getUserRole(user.id)
                                    val now = currentEpochMs()
                                    
                                    // CAPA 2 REFINADA: Solo ignora null si estamos dentro de la ventana de 5s 
                                    // tras un updateRole exitoso. Un null fuera de esta ventana es legítimo.
                                    if (dbRole == null && _cachedRole != null && (now - _lastUpdateTimestamp) < 5000 && _cachedUserId == user.id) {
                                        _cachedRole
                                    } else {
                                        dbRole
                                    }
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                AppLogger.e("AuthService", "Fallo al obtener rol: ${e.message}")
                                if (_cachedUserId == user.id) _cachedRole else null
                            }
                            
                            _cachedRole = role
                            _cachedUserId = user.id
                            emit(SessionStatus.Authenticated(user, role))
                        } else {
                            emit(SessionStatus.NotAuthenticated)
                        }
                    }
                    else -> emit(SessionStatus.NotAuthenticated)
                }
            }
        }.distinctUntilChanged()

    private fun invalidateCache() {
        _cachedRole = null
        _cachedUserId = null
        _lastUpdateTimestamp = 0
        AppLogger.d("AuthService", "Cache de sesión invalidado.")
    }

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
            val response = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileSupabaseDto>()
            
            response
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Rethrow para cumplir con la cooperación de coroutines, pero sin loguear error
            throw e
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
        val roleStr = profile?.role ?: run {
            AppLogger.d("AuthService", "No role found in DB for $userId")
            return null
        }
        
        return try {
            val role = UserRole.valueOf(roleStr.uppercase().trim())
            AppLogger.d("AuthService", "Role resolved for $userId: $role")
            role
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
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("No hay sesión activa: updateRole falló")
        val uid = user.id
        
        AppLogger.d("AuthService", "Iniciando UPSERT atómico de rol para $uid -> ${role.name}")
        
        runCatching {
            // Extraer metadatos para asegurar que el perfil tenga nombre y email
            val fullName = user.userMetadata?.get("full_name")
                ?.let { if (it is JsonPrimitive) it.content else it.toString().replace("\"", "") }
            
            val profileUpdate = ProfileSupabaseDto(
                id = uid,
                email = user.email,
                fullName = fullName,
                role = role.name
            )

            // Cambiamos UPDATE por UPSERT para manejar usuarios nuevos sin perfil previo
            val response = supabase.from("profiles").upsert(profileUpdate) {
                select()
            }.decodeSingleOrNull<ProfileSupabaseDto>()
                ?: throw Exception("Error crítico: El servidor no devolvió el perfil tras el upsert.")
            
            val confirmedRole = response.role?.let { 
                UserRole.valueOf(it.uppercase().trim()) 
            } ?: throw Exception("El servidor devolvió un rol nulo inesperado tras el registro.")
            
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
        // Confirmación de invalidación de caché y datos locales
        invalidateCache()
        syncManager.clearAllLocalData()
        AppLogger.d("AuthService", "Datos locales y cache invalidados por SignOut.")
        supabase.auth.signOut()
    }

    fun isLoggedIn(): Boolean = supabase.auth.currentUserOrNull() != null

    fun currentUser(): UserInfo? = supabase.auth.currentUserOrNull()

    fun currentUserId(): UuidString? {
        val rawId = supabase.auth.currentUserOrNull()?.id ?: return null
        return rawId.toUuidOrNull()
    }
}
