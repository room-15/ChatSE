package me.shreyasr.chatse.network

import com.squareup.okhttp.Call
import com.squareup.okhttp.Interceptor
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import java.net.*

class Client internal constructor(
        //    private static final Client instance = new Client(new OkHttpClient(),
        //            new PersistentCookieStore(App.get()));
        //    public static Client get() { return instance; }
        //    public static OkHttpClient getHttpClient() { return instance.httpClient; }
        //    public OkHttpClient getHttpClientL() { return instance.httpClient; }

        val httpClient: OkHttpClient, private val cookieStore: CookieStore) {

    init {

        val cookieManager = CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL)
        httpClient.cookieHandler = cookieManager

        httpClient.networkInterceptors().add(Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", USER_AGENT)
                    .build()
            chain.proceed(requestWithUserAgent)
        })
    }

    fun getCookie(host: String, name: String): String {
        return getCookie(URI.create(host), name)!!
    }

    fun getCookie(uri: URI, name: String): String? {
        for (cookie in cookieStore.get(uri)) {
            if (cookie.name == name) {
                return cookie.value
            }
        }
        return null
    }

    fun putCookie(uri: String, cookieName: String, cookieContent: String) {
        putCookie(URI.create(uri), cookieName, cookieContent)
    }

    fun putCookie(uri: URI, cookieName: String, cookieContent: String) {
        cookieStore.add(uri, HttpCookie(cookieName, cookieContent))
    }

    fun newCall(request: Request): Call {
        return httpClient.newCall(request)
    }

    companion object {

        val USER_AGENT = "Mozilla/5.0 (Windows NT 6.2; WOW64)" + "AppleWebKit/537.36 (KHTML, like Gecko)" + "Chrome/44.0.2403.155 Safari/537.36"

        val SITE_STACK_EXCHANGE = "http://chat.stackexchange.com"
        val SITE_STACK_OVERFLOW = "http://chat.stackoverflow.com"
    }
}
