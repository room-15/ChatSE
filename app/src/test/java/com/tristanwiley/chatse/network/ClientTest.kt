package com.tristanwiley.chatse.network

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import com.tristanwiley.chatse.getMockPrefs
import com.tristanwiley.chatse.network.cookie.PersistentCookieStore
import org.junit.Test
import kotlin.test.assertEquals

class ClientTest {

    @Test
    fun testClientCustomStore() {
        val cookieStore = givenCookieStore()
        val httpClient = OkHttpClient()
        val client = Client(httpClient, cookieStore)

        client.putCookie("http://stackexchange.com", "acct", "content")

        val expected = "content"
        val actual = client.getCookie("http://stackexchange.com", "acct")
        assertEquals(expected, actual)
    }

    @Test
    fun testClientGetFromWeb() {
        val server = MockWebServer()
        server.enqueue(MockResponse().addHeader("Set-Cookie", "acct=content"))
        server.start()

        val cookieStore = givenCookieStore()
        val httpClient = OkHttpClient()
        val client = Client(httpClient, cookieStore)

        val request = Request.Builder().apply {
            url(server.getUrl("/"))
        }.build()

        client.newCall(request).execute()

        val expected = "content"
        val actual = client.getCookie(server.getUrl("/").toURI(), "acct")
        assertEquals(expected, actual)

    }

    private fun givenCookieStore(): PersistentCookieStore {
        val sharedPefs = getMockPrefs()
        val context = mock<Context>()

        whenever(context.getSharedPreferences(any(), any())).doReturn(sharedPefs)

        return PersistentCookieStore(context)
    }

}
