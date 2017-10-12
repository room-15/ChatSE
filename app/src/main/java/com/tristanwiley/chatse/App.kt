package com.tristanwiley.chatse

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import io.realm.Realm

/**
 * Application class that manages certain constants for us.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        initRealm()
        com.tristanwiley.chatse.App.Companion.instance = this
    }

    fun initRealm(){
        Realm.init(this)
    }

    companion object {

        /**
         * Key for storing the user's email in shared preferences.
         */
        val PREF_EMAIL = "email"

        /**
         * Key for storing the user's credentials in shared preferences.
         */
        val PREF_HAS_CREDS = "creds"

        /**
         * Key for passing the room number as an intent extra.
         */
        val EXTRA_ROOM_NUM = "room"

        /**
         * Key for passing the site name as an intent extra.
         */
        val EXTRA_SITE = "site"

        /**
         * Key for passing the fkey as an intent extra.
         */
        val EXTRA_FKEY = "fkey"

        /**
         * The application context.
         */
        lateinit var instance: com.tristanwiley.chatse.App
            private set

        /**
         * The default SharedPreferences for this application.
         */
        val sharedPreferences: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(com.tristanwiley.chatse.App.Companion.instance)
    }
}
