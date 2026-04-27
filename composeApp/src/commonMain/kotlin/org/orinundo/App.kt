package org.orinundo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.ui.screens.RoleSelectionScreen
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.screens.TeacherDashboardScreen
import com.tuapp.libreta.ui.screens.ParentDashboardScreen
import com.tuapp.libreta.ui.theme.LibretAppTheme
import org.koin.compose.koinInject

import androidx.compose.foundation.layout.BoxWithConstraints
import com.tuapp.libreta.ui.util.ProvideWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass
import com.tuapp.libreta.ui.util.LocalWindowSize

@Composable
fun App() {
    val authService: SupabaseAuthService = koinInject()
    val sessionStatus by authService.sessionStatusFlow.collectAsState(initial = SessionStatus.Loading)

    LibretAppTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            val height = maxHeight
            
            ProvideWindowSize(width, height) {
                val windowSize = LocalWindowSize.current
                
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Contenedor Maestro Responsivo
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Ajuste dinámico del ancho máximo según el dispositivo
                        val contentMaxWidth = when (windowSize.widthSizeClass) {
                            WindowSizeClass.COMPACT -> Dp.Unspecified
                            WindowSizeClass.MEDIUM -> 720.dp
                            WindowSizeClass.EXPANDED -> 1024.dp // Un poco más ancho en Desktop para dashboards
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (contentMaxWidth != Dp.Unspecified) 
                                        Modifier.widthIn(max = contentMaxWidth) 
                                    else Modifier
                                )
                        ) {
                            Navigator(LoginScreen) { navigator ->
                                LaunchedEffect(sessionStatus) {
                                    val currentScreen = navigator.lastItem
                                    when (val status = sessionStatus) {
                                        is SessionStatus.Authenticated -> {
                                            val role = status.role
                                            val userId = status.user.id
                                            
                                            if (role == null) {
                                                if (currentScreen !is RoleSelectionScreen) {
                                                    navigator.replaceAll(RoleSelectionScreen)
                                                }
                                            } else {
                                                val isAlreadyInDashboard = when(role) {
                                                    com.tuapp.libreta.domain.model.UserRole.TEACHER -> currentScreen is TeacherDashboardScreen
                                                    com.tuapp.libreta.domain.model.UserRole.PARENT -> currentScreen is ParentDashboardScreen
                                                }
                                                if (!isAlreadyInDashboard) {
                                                    navigator.replaceAll(AppNavigation.initialScreen(role, userId))
                                                }
                                            }
                                        }
                                        is SessionStatus.NotAuthenticated -> {
                                            if (currentScreen !is LoginScreen) {
                                                navigator.replaceAll(LoginScreen)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                SlideTransition(navigator)
                            }
                        }
                    }
                }
            }
        }
    }
}
