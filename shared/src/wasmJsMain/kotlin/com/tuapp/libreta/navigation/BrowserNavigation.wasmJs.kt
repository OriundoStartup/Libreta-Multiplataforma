package com.tuapp.libreta.navigation

import kotlinx.browser.window

actual fun updateBrowserHistory(path: String) {
    window.history.pushState(null, "", path)
}

actual fun getInitialPath(): String {
    return window.location.pathname
}
