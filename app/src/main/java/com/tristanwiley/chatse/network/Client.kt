package com.tristanwiley.chatse.network

import com.squareup.okhttp.Call
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import java.net.*

/**
 * Http Client used to make requests to SE.
 *
 * @property[httpClient] The base OkHttpClient to use.
 * @property[cookieStore] The cookie storage to use.
 */
class Client internal constructor(val httpClient: OkHttpClient, private val cookieStore: CookieStore) {

    init {
        val cookieManager = CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)
        httpClient.cookieHandler = cookieManager

        httpClient.networkInterceptors().add(Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", Client.USER_AGENT)
                    .build()
            chain.proceed(requestWithUserAgent)
        })
    }

    /**
     * Retrieves a cookie by URI string.
     *
     * @param[host] The host where the cookie is being stored.
     * @param[name] The name of the cookie to retrieve.
     *
     * @return The content of the cookie, or an empty string if it didn't exist.
     */
    fun getCookie(host: String, name: String): String {
        return getCookie(URI.create(host), name)
    }

    /**
     * Retrieves a cookie by URI.
     *
     * @param[uri] The URI where the cookie is being stored.
     * @param[name] The name of the cookie to retrieve.
     *
     * @return The content of the cookie, or an empty string if it didn't exist.
     */
    fun getCookie(uri: URI, name: String): String {
        return cookieStore.get(uri)
                .firstOrNull { it.name == name }
                ?.value.orEmpty()
    }

    /**
     * Stores a cookie by URI string.
     *
     * @param[uri] The URI where the cookie should be stored.
     * @param[cookieName] The name of the cookie to store.
     * @param[cookieContent] The content of the cookkie.
     */
    fun putCookie(uri: String, cookieName: String, cookieContent: String) {
        putCookie(URI.create(uri), cookieName, cookieContent)
    }

    /**
     * Stores a cookie by URI.
     *
     * @param[uri] The URI where the cookie should be stored.
     * @param[cookieName] The name of the cookie to store.
     * @param[cookieContent] The content of the cookkie.
     */
    private fun putCookie(uri: URI, cookieName: String, cookieContent: String) {
        cookieStore.add(uri, HttpCookie(cookieName, cookieContent))
    }

    /**
     * Creates a call for the HttpClient.
     *
     * @param[request]
     */
    fun newCall(request: Request): Call {
        return httpClient.newCall(request)
    }

    companion object {
        /**
         * The UserAgent for all calls.
         */
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64)" + "AppleWebKit/537.36 (KHTML, like Gecko)" + "Chrome/44.0.2403.155 Safari/537.36"

        /**
         * URL to the stack exchange site.
         */
        const val SITE_STACK_EXCHANGE = "https://chat.stackexchange.com"

        /**
         * URL to the stack overflow site.
         */
        const val SITE_STACK_OVERFLOW = "https://chat.stackoverflow.com"
    }
}
