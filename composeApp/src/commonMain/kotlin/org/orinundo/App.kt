package org.orinundo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.screens.LoginScreen
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService: SupabaseAuthService = koinInject()

    MaterialTheme {
        Navigator(AppNavigation.startDestination()) { navigator ->
            LaunchedEffect(Unit) {
                authService.sessionStatusFlow
                    .filterIsInstance<SessionStatus.NotAuthenticated>()
                    .collect {
                        if (navigator.lastItem !is LoginScreen) {
                            navigator.replaceAll(LoginScreen)
                        }
                    }
            }
            SlideTransition(navigator)
        }
    }
}
