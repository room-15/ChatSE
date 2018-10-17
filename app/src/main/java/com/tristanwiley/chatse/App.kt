package com.tristanwiley.chatse

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.tristanwiley.chatse.log.CrashlyticsTimberTree
import io.fabric.sdk.android.Fabric
import timber.log.Timber

// TODO integrate timber and fabric.
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        else {
            Fabric.with(this, Crashlytics())
            Timber.plant(CrashlyticsTimberTree)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
