package com.tristanwiley.chatse

import android.app.Application
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        instance = this
    }

    companion object {
        /**
         * The application instance.
         */
        lateinit var instance: App
            private set
    }
}
