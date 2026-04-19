package org.orinundo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.screens.LoginScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.filterIsInstance
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    val authService = remember { GlobalContext.get().get<SupabaseAuthService>() }

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
