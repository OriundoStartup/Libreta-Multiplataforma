package org.oriundo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.navigation.WebPathMapper
import com.tuapp.libreta.navigation.getInitialPath
import com.tuapp.libreta.navigation.updateBrowserHistory
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.ui.screens.ParentDashboardScreen
import com.tuapp.libreta.ui.screens.RoleSelectionScreen
import com.tuapp.libreta.ui.screens.TeacherDashboardScreen
import com.tuapp.libreta.ui.theme.LibretAppTheme
import com.tuapp.libreta.ui.util.LocalWindowSize
import com.tuapp.libreta.ui.util.ProvideWindowSize
import com.tuapp.libreta.ui.util.WindowSizeClass
import org.koin.compose.koinInject

@Composable
fun App(initialScreen: cafe.adriel.voyager.core.screen.Screen? = null) {
    val authService: SupabaseAuthService = koinInject()
    val sessionStatus by authService.sessionStatusFlow.collectAsState(initial = SessionStatus.NotAuthenticated)

    val startScreen = remember { initialScreen ?: WebPathMapper.fromPath(getInitialPath()) }

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
                            Navigator(startScreen) { navigator ->
                                // SINCRONIZAR URL CON EL NAVEGADOR
                                LaunchedEffect(navigator.lastItem) {
                                    updateBrowserHistory(WebPathMapper.toPath(navigator.lastItem))
                                }

                                LaunchedEffect(sessionStatus) {
                                    val currentScreen = navigator.lastItem
                                    when (val status = sessionStatus) {
                                        is SessionStatus.Authenticated -> {
                                            val role = status.role
                                            val userId = status.user.id
                                            
                                            // CASO 1: Usuario recién logueado o sin rol
                                            if (currentScreen is LoginScreen || (role == null && currentScreen !is RoleSelectionScreen)) {
                                                navigator.replaceAll(RoleSelectionScreen())
                                                return@LaunchedEffect
                                            }

                                            // CASO 2: Usuario ya tiene rol y está en RoleSelectionScreen (Auto-redirect)
                                            // Solo auto-redigimos si NO estamos forzando el cambio de rol
                                            if (role != null && currentScreen is RoleSelectionScreen && !currentScreen.isSwitchingRole) {
                                                navigator.replaceAll(AppNavigation.initialScreen(role, userId))
                                                return@LaunchedEffect
                                            }

                                            // CASO 3: Usuario ya tiene rol y está intentando entrar a una zona prohibida
                                            if (role != null) {
                                                val isForbidden = when(role) {
                                                    com.tuapp.libreta.domain.model.UserRole.TEACHER -> currentScreen is ParentDashboardScreen
                                                    com.tuapp.libreta.domain.model.UserRole.PARENT -> currentScreen is TeacherDashboardScreen
                                                }
                                                
                                                // Si está en una pantalla prohibida para su rol actual, lo sacamos.
                                                // Pero NO lo sacamos de RoleSelectionScreen automáticamente, 
                                                // dejamos que la pantalla maneje la lógica de "Continuar".
                                                if (isForbidden) {
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

                                // OVERLAY GLOBAL DE CARGA
                                if (sessionStatus is SessionStatus.Loading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .clickable(enabled = false) {}, // Bloquear clics
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
