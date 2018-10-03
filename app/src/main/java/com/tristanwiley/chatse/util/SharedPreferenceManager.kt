package com.tristanwiley.chatse.util

import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.tristanwiley.chatse.App

object SharedPreferenceManager {

    /**
     * The default SharedPreferences for this application.
     */
    val sharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(App.instance)

}

class UserPreferenceKeys {
    companion object {
        /**
         * The user's email.
         */
        const val EMAIL = "email"

        /**
         * Whether or not the user is logged in.
         */
        const val IS_LOGGED_IN = "is_logged_in"

        /**
         * Key for passing the room number as an intent extra.
         */
        const val EXTRA_ROOM_NUM = "room"

        /**
         * Key for passing the site name as an intent extra.
         */
        const val EXTRA_SITE = "site"

        /**
         * Key for passing the fkey as an intent extra.
         */
        const val EXTRA_FKEY = "fkey"
    }
}
