package fr.coppernic.samples.ask

import android.app.Application
import android.util.Log
import fr.bipi.tressence.file.FileLoggerTree
import fr.coppernic.samples.ask.di.log.timberLogger
import fr.coppernic.samples.ask.di.modules.appModule
import fr.coppernic.samples.ask.di.modules.scopesModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {

    companion object {
        @JvmStatic
        lateinit var fileTree: FileLoggerTree
    }

    override fun onCreate() {
        super.onCreate()
        setupDi()
        setupLog()
    }

    private fun setupDi() {
        startKoin {
            timberLogger()
            androidContext(this@App)
            modules(appModule)
            modules(scopesModule)
        }
    }

    private fun setupLog() {

        Timber.plant(Timber.DebugTree())

        // File Log
        fileTree = FileLoggerTree.Builder()
                .withDirName(filesDir.absolutePath)
                .withMinPriority(Log.VERBOSE).build()
        Timber.plant(fileTree)
    }
}
