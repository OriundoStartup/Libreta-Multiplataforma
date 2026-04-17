package org.orinundo

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.tuapp.libreta.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        Navigator(AppNavigation.initialScreen()) { navigator ->
            SlideTransition(navigator)
        }
    }
}
