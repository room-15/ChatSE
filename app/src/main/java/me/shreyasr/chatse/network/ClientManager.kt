package me.shreyasr.chatse.network

import com.squareup.okhttp.OkHttpClient
import me.shreyasr.chatse.App
import me.shreyasr.chatse.network.cookie.PersistentCookieStore

/**
 * Static manager of our HTTP Client.
 */
object ClientManager {
    /**
     * The client to use for network requests.
     */
    val client = Client(OkHttpClient(), PersistentCookieStore(App.instance))
}
