package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable

object GoogleAuthRegistry {
    var launcher: (suspend () -> Unit)? = null
}

@Composable
actual fun rememberGoogleAuthLauncher(): (suspend () -> Unit)? = GoogleAuthRegistry.launcher
