package com.tuapp.libreta.navigation

actual fun updateBrowserHistory(path: String) {
    // No-op en Android
}

actual fun getInitialPath(): String = ""
