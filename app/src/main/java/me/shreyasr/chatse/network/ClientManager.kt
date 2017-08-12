package me.shreyasr.chatse.network

import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import me.shreyasr.chatse.App
import okhttp3.OkHttpClient

/**
 * Static manager of our HTTP Client.
 */
object ClientManager {
    /**
     * The client to use for network requests.
     */
    val client: OkHttpClient = OkHttpClient.Builder().cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(App.instance)) as ClearableCookieJar).addInterceptor { chain ->
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", App.USER_AGENT)
                .build()
        chain.proceed(requestWithUserAgent)
    }.build()
}