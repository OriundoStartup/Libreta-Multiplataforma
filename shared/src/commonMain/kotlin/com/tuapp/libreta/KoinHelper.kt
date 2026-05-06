package com.tuapp.libreta

import com.tuapp.libreta.di.appModule
import com.tuapp.libreta.di.platformModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule, appModule)
    }
}
