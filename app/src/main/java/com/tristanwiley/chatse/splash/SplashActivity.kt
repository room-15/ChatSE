package com.tristanwiley.chatse.splash

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.tristanwiley.chatse.App
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = App.sharedPreferences
        val isLoggedIn = prefs.getBoolean(App.PREF_HAS_CREDS, false)

        val targetActivity = if (isLoggedIn) {
            ChatActivity::class.java
        } else {
            LoginActivity::class.java
        }

        startActivity(Intent(this, targetActivity))
        finish()
    }
}
