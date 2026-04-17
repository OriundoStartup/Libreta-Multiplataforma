package org.orinundo

import android.app.Application
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.di.appModule
import com.tuapp.libreta.di.platformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.android.ext.android.inject

class LibretaApplication : Application() {
    private val seeder: DataSeeder by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LibretaApplication)
            modules(platformModule, appModule)
        }
        // Ejecuta una sola vez, fuera de Compose, en background
        CoroutineScope(Dispatchers.IO).launch { seeder.seedIfEmpty() }
    }
}
