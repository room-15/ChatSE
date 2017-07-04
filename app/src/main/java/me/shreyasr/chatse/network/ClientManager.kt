package me.shreyasr.chatse.network

import com.squareup.okhttp.OkHttpClient

import me.shreyasr.chatse.App
import me.shreyasr.chatse.network.cookie.PersistentCookieStore

object ClientManager {

    val client = Client(OkHttpClient(), PersistentCookieStore(App.instance))
}
