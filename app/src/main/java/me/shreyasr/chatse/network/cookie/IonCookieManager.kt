package me.shreyasr.chatse.network.cookie

/**
 * From https://gist.github.com/littlefyr/e5bd337b53b1e42328b0
 */

import android.content.Context
import android.util.Log

import com.koushikdutta.async.http.Headers
import com.koushikdutta.ion.Ion

import java.io.IOException
import java.net.CookieManager
import java.net.URI
import java.net.URLConnection
import java.util.ArrayList

class IonCookieManager(context: Context) {
    internal var manager: CookieManager

    init {
        val ion = Ion.getDefault(context)
        manager = ion.cookieMiddleware.cookieManager
    }

    @Throws(IOException::class)
    fun storeCookies(conn: URLConnection) {

        val cookies = conn.headerFields["Set-Cookie"]
        val uri = URI.create(conn.url.toString())
        if (cookies != null) {
            storeCookies(uri, cookies)
        }
    }

    @Throws(IOException::class)
    fun storeCookies(uri: URI, cookies: List<String>) {
        val headers = Headers()
        headers.addAll("Set-Cookie", cookies)

        manager.put(uri, headers.multiMap)
    }

    @Throws(IOException::class)
    fun storeCookie(uri: URI, cookieName: String, cookieValue: String) {
        Log.wtf("COOKIES", cookieValue)
        val cookie = ArrayList<String>()
        cookie.add(String.format("%s=%s", cookieName, cookieValue))
        storeCookies(uri, cookie)
    }

}
