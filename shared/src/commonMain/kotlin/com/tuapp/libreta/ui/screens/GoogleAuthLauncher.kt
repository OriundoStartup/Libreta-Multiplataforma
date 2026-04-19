package com.tuapp.libreta.ui.screens

import androidx.compose.runtime.Composable

@Composable
expect fun rememberGoogleAuthLauncher(): (suspend () -> Unit)?
