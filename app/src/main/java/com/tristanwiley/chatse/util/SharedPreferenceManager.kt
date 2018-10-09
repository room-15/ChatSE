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
    }
}

class RoomPreferenceKeys {
    companion object {

        /**
         * Last room site.
         */
        const val LAST_ROOM_SITE = "last_room_site"

        /**
         * Last room number.
         */
        const val LAST_ROOM_NUM = "last_room_num"
    }
}

