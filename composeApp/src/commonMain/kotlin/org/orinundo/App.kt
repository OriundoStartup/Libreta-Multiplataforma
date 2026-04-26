package org.orinundo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.isAuthenticated
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.screens.LoginScreen
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService: SupabaseAuthService = koinInject()
    val sessionStatus by authService.sessionStatusFlow.collectAsState(initial = SessionStatus.Loading)
    
    val isAuthenticated = sessionStatus.isAuthenticated()
    val needsRoleSelection = sessionStatus is SessionStatus.Authenticated && 
        (sessionStatus as? SessionStatus.Authenticated)?.user?.let { false } == false

    val startScreen = when {
        sessionStatus is SessionStatus.Loading -> LoginScreen
        sessionStatus is SessionStatus.NotAuthenticated -> LoginScreen
        needsRoleSelection -> LoginScreen
        isAuthenticated -> AppNavigation.initialScreen()
        else -> LoginScreen
    }

    MaterialTheme {
        Navigator(startScreen) { navigator ->
            val currentScreen = navigator.lastItem

            LaunchedEffect(sessionStatus) {
                when (sessionStatus) {
                    is SessionStatus.NotAuthenticated -> {
                        if (currentScreen !is LoginScreen) {
                            navigator.replaceAll(LoginScreen)
                        }
                    }
                    is SessionStatus.Authenticated -> {
                        if (currentScreen is LoginScreen) {
                            navigator.replaceAll(AppNavigation.initialScreen())
                        }
                    }
                    else -> {}
                }
            }

            SlideTransition(navigator)
        }
    }
}