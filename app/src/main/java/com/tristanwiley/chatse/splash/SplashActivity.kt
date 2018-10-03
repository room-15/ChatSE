package com.tristanwiley.chatse.splash

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.login.LoginActivity
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SharedPreferenceManager.sharedPreferences
        val isLoggedIn = prefs.getBoolean(UserPreferenceKeys.IS_LOGGED_IN, false)

        val targetActivity = if (isLoggedIn) {
            ChatActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, targetActivity))
        finish()
    }
}
