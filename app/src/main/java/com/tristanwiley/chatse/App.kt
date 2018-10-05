package com.tristanwiley.chatse

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import timber.log.Timber

// TODO integrate timber and fabric.
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        Fabric.with(this, Crashlytics())

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
