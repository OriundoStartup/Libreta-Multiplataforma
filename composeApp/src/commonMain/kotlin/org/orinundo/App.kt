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
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.data.remote.SessionStatus
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.ui.theme.LibretAppTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    val authService: SupabaseAuthService = koinInject()
    val sessionStatus by authService.sessionStatusFlow.collectAsState(initial = SessionStatus.Loading)

    LibretAppTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Contenedor responsivo: En web (pantallas anchas) centra el contenido
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 600.dp) // Ancho máximo tipo móvil/tablet para legibilidad
                ) {
                    Navigator(LoginScreen) { navigator ->
                        LaunchedEffect(sessionStatus) {
                            if (sessionStatus is SessionStatus.NotAuthenticated && navigator.lastItem !is LoginScreen) {
                                navigator.replaceAll(LoginScreen)
                            }
                        }
                        SlideTransition(navigator)
                    }
                }
            }
        }
    }
}
