package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.data.remote.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Unauthenticated    : LoginUiState
    data object Loading            : LoginUiState
    data object NeedsRoleSelection : LoginUiState
    data class  Success(val role: UserRole, val userId: UuidString) : LoginUiState
    data class  Error(val message: String)  : LoginUiState
}

class LoginScreenModel(
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Unauthenticated)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        // Escuchamos la sesión globalmente. Si estamos autenticados, verificamos el rol y redirigimos.
        screenModelScope.launch {
            authService.sessionStatusFlow.collectLatest { status ->
                if (status is SessionStatus.Authenticated && _state.value !is LoginUiState.Success) {
                    checkUserStatus()
                } else if (status is SessionStatus.NotAuthenticated) {
                    _state.value = LoginUiState.Unauthenticated
                }
            }
        }
    }

    fun signInWithGoogle(launcher: (suspend () -> Unit)? = null) {
        println("LoginModel: signInWithGoogle called")
        screenModelScope.launch {
            _state.value = LoginUiState.Loading
            println("LoginModel: UI State -> Loading")
            
            val result = runCatching { 
                if (launcher != null) {
                    println("LoginModel: Invoking launcher...")
                    launcher.invoke()
                } else {
                    println("LoginModel: Using default authService.signInWithGoogle()")
                    authService.signInWithGoogle()
                }
            }

            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: "Error desconocido"
                println("LoginModel: FAILURE -> $errorMsg")
                _state.value = LoginUiState.Error(errorMsg)
            } else {
                println("LoginModel: SUCCESS (Waiting for redirection or callback)")
            }
        }
    }

    private suspend fun checkUserStatus() {
        val userId = authService.currentUserId()
        if (userId == null) {
            _state.value = LoginUiState.Error("Sesión iniciada pero no se pudo obtener el ID")
            return
        }

        val role = authService.getUserRole(userId.value)
        
        if (role == null) {
            _state.value = LoginUiState.NeedsRoleSelection
        } else {
            _state.value = LoginUiState.Success(role, userId)
        }
    }

    fun resetState() {
        _state.value = LoginUiState.Unauthenticated
    }
}
