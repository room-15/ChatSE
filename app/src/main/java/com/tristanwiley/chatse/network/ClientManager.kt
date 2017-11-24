package com.tristanwiley.chatse.network

import com.squareup.okhttp.OkHttpClient
import com.tristanwiley.chatse.App
import com.tristanwiley.chatse.network.cookie.PersistentCookieStore

/**
 * Static manager of our HTTP Client.
 */
object ClientManager {
    /**
     * The client to use for network requests.
     */
    val client = Client(OkHttpClient(), PersistentCookieStore(App.instance))
}
