package com.abeerapps.kmpexpensesplitter

import android.app.Application
import com.abeerapps.kmpexpensesplitter.config.AppConfig
import com.abeerapps.kmpexpensesplitter.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/** Android process entry point: starts the shared DI graph once per process. */
class ExpenseSplitterApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(AppConfig(isDebug = BuildConfig.DEBUG, versionName = BuildConfig.VERSION_NAME)) {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.NONE)
            androidContext(this@ExpenseSplitterApplication)
        }
    }
}
