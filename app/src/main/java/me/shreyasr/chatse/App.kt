package me.shreyasr.chatse

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        inst = this
    }

    companion object {

        val PREF_EMAIL = "email"
        val PREF_HAS_CREDS = "creds"

        val EXTRA_ROOM_NUM = "room"
        val EXTRA_SITE = "site"
        val EXTRA_FKEY = "fkey"

        private var inst: App? = null

        fun get(): App? {
            return inst
        }

        private val prefs: SharedPreferences
            get() = PreferenceManager.getDefaultSharedPreferences(inst)

        fun getPrefs(context: Context): SharedPreferences {
            return prefs
        }
    }
}
