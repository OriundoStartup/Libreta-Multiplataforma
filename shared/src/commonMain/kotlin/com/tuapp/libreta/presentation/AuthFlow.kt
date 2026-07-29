package com.tuapp.libreta.presentation

import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.domain.model.UserRole

/**
 * FASE 4 — State machine de autenticación que reemplaza la pirámide
 * de `if` anidados de [org.oriundo.App.kt:85-127].
 *
 * Reduce 4 niveles de condiciones a un `when` lineal.
 *
 * Uso:
 * ```
 * val flow = AuthFlow.from(sessionStatus, currentScreen)
 * when (flow) {
 *     AuthFlow.Loading        -> showOverlay()
 *     AuthFlow.LoginRequired  -> nav.replaceAll(LoginScreen)
 *     is AuthFlow.NeedsRole   -> nav.replaceAll(RoleSelectionScreen())
 *     is AuthFlow.Ready       -> nav.replaceAll(AppNavigation.initialScreen(flow.role, flow.userId))
 *     is AuthFlow.Forbidden   -> nav.replaceAll(AppNavigation.initialScreen(flow.role, flow.userId))
 *     AuthFlow.Stay           -> Unit  // ya está donde debe
 * }
 * ```
 *
 * TODO[FASE-4]:
 *   1. Implementar `AuthFlow.from(...)` con la lógica completa de App.kt.
 *   2. Cubrir con tests todos los caminos.
 *   3. Reemplazar el LaunchedEffect(sessionStatus) en App.kt.
 */
sealed interface AuthFlow {
    data object Loading : AuthFlow
    data object LoginRequired : AuthFlow
    data class NeedsRole(val userId: String) : AuthFlow
    data class Ready(val role: UserRole, val userId: String) : AuthFlow
    data class Forbidden(val role: UserRole, val userId: String) : AuthFlow
    data object Stay : AuthFlow

    companion object {
        fun from(
            status: SessionStatus,
            currentScreenKind: ScreenKind,
            isSwitchingRole: Boolean = false
        ): AuthFlow {
            return when (status) {
                is SessionStatus.Loading -> Loading
                is SessionStatus.NotAuthenticated -> {
                    // Si estamos cargando o en una transición, no forzamos login de inmediato
                    if (currentScreenKind == ScreenKind.LOGIN) Stay else LoginRequired
                }
                is SessionStatus.Authenticated -> {
                    val role = status.role
                    val userId = status.user.id

                    when {
                        // Caso 1: Sin rol definido (Usuario recién registrado vía Google)
                        role == null -> {
                            AppLogger.d("AuthFlow", "Role is NULL. Redirecting to RoleSelection if not already there.")
                            if (currentScreenKind != ScreenKind.ROLE_SELECTION) {
                                NeedsRole(userId)
                            } else {
                                Stay
                            }
                        }
                        // Caso 2: Tiene rol pero está en pantalla de login
                        currentScreenKind == ScreenKind.LOGIN -> {
                            Ready(role, userId)
                        }
                        // Caso 3: Tiene rol y está en selección de rol (auto-redirect si no está swicheando)
                        currentScreenKind == ScreenKind.ROLE_SELECTION -> {
                            if (!isSwitchingRole) Ready(role, userId) else Stay
                        }
                        // Caso 4: Verificación de acceso denegado por rol
                        role == UserRole.TEACHER && currentScreenKind == ScreenKind.PARENT_HOME -> {
                            Forbidden(role, userId)
                        }
                        role == UserRole.PARENT && currentScreenKind == ScreenKind.TEACHER_HOME -> {
                            Forbidden(role, userId)
                        }
                        // Caso por defecto: Todo OK
                        else -> Stay
                    }
                }
            }
        }
    }
}

/** Categorización plana de las screens para que AuthFlow no dependa de tipos concretos. */
enum class ScreenKind {
    LOGIN,
    ROLE_SELECTION,
    TEACHER_HOME,
    PARENT_HOME,
    OTHER
}
