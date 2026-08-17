package org.oriundo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.tuapp.libreta.presentation.AuthFlow
import com.tuapp.libreta.presentation.ScreenKind
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.data.util.AppLogger
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
    val sessionStatus by authService.sessionStatusFlow.collectAsState(initial = SessionStatus.Loading)

    val startScreen = remember { initialScreen ?: WebPathMapper.fromPath(getInitialPath()) }

    LibretAppTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            val height = maxHeight
            
            ProvideWindowSize(width, height) {
                val windowSize = LocalWindowSize.current
                
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Contenedor Maestro Responsivo
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentMaxWidth = when (windowSize.widthSizeClass) {
                            WindowSizeClass.COMPACT -> Dp.Unspecified
                            WindowSizeClass.MEDIUM -> 720.dp
                            WindowSizeClass.EXPANDED -> 1024.dp
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

                                LaunchedEffect(sessionStatus, navigator.lastItem) {
                                    val currentScreen = navigator.lastItem
                                    val kind = when (currentScreen) {
                                        is LoginScreen -> ScreenKind.LOGIN
                                        is RoleSelectionScreen -> ScreenKind.ROLE_SELECTION
                                        is TeacherDashboardScreen -> ScreenKind.TEACHER_HOME
                                        is ParentDashboardScreen -> ScreenKind.PARENT_HOME
                                        else -> ScreenKind.OTHER
                                    }
                                    
                                    val isSwitching = (currentScreen as? RoleSelectionScreen)?.isSwitchingRole ?: false
                                    
                                    val flow = AuthFlow.from(sessionStatus, kind, isSwitching)
                                    
                                    // DEBUG LOG
                                    AppLogger.d("Guardian", "Flow status update: Status=$sessionStatus | Screen=$kind | Flow=$flow")

                                    when (flow) {
                                        AuthFlow.LoginRequired -> {
                                            if (currentScreen !is LoginScreen) {
                                                println("AuthFlow: Redirecting to Login")
                                                navigator.replaceAll(LoginScreen)
                                            }
                                        }
                                        is AuthFlow.NeedsRole -> {
                                            if (currentScreen !is RoleSelectionScreen) {
                                                println("AuthFlow: Redirecting to RoleSelection")
                                                navigator.replaceAll(RoleSelectionScreen())
                                            }
                                        }
                                        is AuthFlow.Ready -> {
                                            val target = AppNavigation.initialScreen(flow.role, flow.userId)
                                            // Comparación por tipo de clase para evitar loops infinitos
                                            if (currentScreen::class != target::class) {
                                                println("Guardian: Redirecting AUTHENTICATED user to ${target::class.simpleName}")
                                                navigator.replaceAll(target)
                                            }
                                        }
                                        is AuthFlow.Forbidden -> {
                                            val target = AppNavigation.initialScreen(flow.role, flow.userId)
                                            if (currentScreen::class != target::class) {
                                                println("AuthFlow: Access Forbidden. Redirecting to Home.")
                                                navigator.replaceAll(target)
                                            }
                                        }
                                        AuthFlow.Loading -> { /* Handled by global overlay */ }
                                        AuthFlow.Stay -> { /* Already there */ }
                                    }
                                }
                                SlideTransition(navigator)

                                // OVERLAY GLOBAL DE CARGA
                                if (sessionStatus is SessionStatus.Loading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .clickable(enabled = false) {},
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
