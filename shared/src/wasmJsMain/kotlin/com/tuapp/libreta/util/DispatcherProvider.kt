package com.tuapp.libreta.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// JS es single-threaded; Default evita bloquear el event loop que Unconfined causaba
actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.Default
