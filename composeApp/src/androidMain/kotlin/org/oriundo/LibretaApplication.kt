package org.oriundo

import android.app.Application
import com.tuapp.libreta.data.util.AppContextHolder
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.di.appModule
import com.tuapp.libreta.di.platformModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LibretaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val seeder: DataSeeder by inject()

    override fun onCreate() {
        super.onCreate()
        AppContextHolder.init(this)
        
        // Inicialización unificada de Koin
        com.tuapp.libreta.initKoin {
            androidContext(this@LibretaApplication)
        }
        
        applicationScope.launch { seeder.seedIfEmpty() }
    }
}
