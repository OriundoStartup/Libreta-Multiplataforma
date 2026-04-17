package com.tuapp.libreta

import com.tuapp.libreta.di.appModule
import com.tuapp.libreta.di.platformModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(platformModule, appModule)
    }
}
