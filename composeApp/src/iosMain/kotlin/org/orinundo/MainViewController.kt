package org.orinundo

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController(
    configure = {
        com.tuapp.libreta.initKoin()
    }
) { App() }