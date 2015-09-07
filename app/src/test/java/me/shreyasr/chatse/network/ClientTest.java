package me.shreyasr.chatse.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import junit.framework.TestCase;

import java.net.CookieStore;

import me.shreyasr.chatse.TestUtils;
import me.shreyasr.chatse.network.cookie.PersistentCookieStore;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientTest extends TestCase {

    private PersistentCookieStore getCookieStore() {
        SharedPreferences mockSharedPrefs = TestUtils.getMockPrefs();
        Context mockContext = mock(Context.class);

        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPrefs);

        return new PersistentCookieStore(mockContext);
    }

    public void testClientCustomStore() throws Exception {
        CookieStore cookieStore = getCookieStore();
        OkHttpClient httpClient = new OkHttpClient();

        Client client = new Client(httpClient, cookieStore);
        client.putCookie("http://stackexchange.com", "acct", "content");

        assertEquals("content", client.getCookie("http://stackexchange.com", "acct"));
    }

    public void testClientGetFromWeb() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().addHeader("Set-Cookie", "acct=content"));
        server.start();

        CookieStore cookieStore = getCookieStore();
        OkHttpClient httpClient = new OkHttpClient();

        Request request = new Request.Builder()
                .url(server.getUrl("/"))
                .build();

        Client client = new Client(httpClient, cookieStore);
        client.newCall(request).execute();

        assertEquals("content", client.getCookie(server.getUrl("/").toURI(), "acct"));
    }
}