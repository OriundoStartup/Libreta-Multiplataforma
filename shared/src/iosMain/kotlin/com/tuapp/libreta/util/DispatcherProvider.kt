package com.tuapp.libreta.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun getIoDispatcher(): CoroutineDispatcher = Dispatchers.Default
