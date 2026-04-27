package com.tuapp.libreta.navigation

actual fun updateBrowserHistory(path: String) {
    // No-op en iOS
}

actual fun getInitialPath(): String = ""
