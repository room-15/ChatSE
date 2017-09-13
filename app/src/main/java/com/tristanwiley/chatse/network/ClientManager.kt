package com.tristanwiley.chatse.network

import com.squareup.okhttp.OkHttpClient

/**
 * Static manager of our HTTP Client.
 */
object ClientManager {
    /**
     * The client to use for network requests.
     */
    val client = Client(OkHttpClient(), com.tristanwiley.chatse.network.cookie.PersistentCookieStore(com.tristanwiley.chatse.App.instance))
}
