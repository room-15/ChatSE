package me.shreyasr.chatse.network;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;

public class Client {

    public static final String USER_AGENT
            = "Mozilla/5.0 (Windows NT 6.2; WOW64)"
            + "AppleWebKit/537.36 (KHTML, like Gecko)"
            + "Chrome/44.0.2403.155 Safari/537.36";

//    private static final Client instance = new Client(new OkHttpClient(),
//            new PersistentCookieStore(App.get()));
//    public static Client get() { return instance; }
//    public static OkHttpClient getHttpClient() { return instance.httpClient; }
//    public OkHttpClient getHttpClientL() { return instance.httpClient; }

    private final OkHttpClient httpClient;
    private final CookieStore cookieStore;

    public OkHttpClient getHttpClient() { return httpClient; }

    Client(OkHttpClient client, CookieStore cookieStore) {
        this.httpClient = client;
        this.cookieStore = cookieStore;

        CookieManager cookieManager = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
        httpClient.setCookieHandler(cookieManager);

        httpClient.networkInterceptors().add(new Interceptor() {
            @Override public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request requestWithUserAgent = originalRequest.newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader("User-Agent", USER_AGENT)
                        .build();
                return chain.proceed(requestWithUserAgent);
            }
        });
    }

    public String getCookie(String host, String name) {
        return getCookie(URI.create(host), name);
    }

    public String getCookie(URI uri, String name) {
        for (HttpCookie cookie : cookieStore.get(uri)) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void putCookie(String uri, String cookieName, String cookieContent) {
        putCookie(URI.create(uri), cookieName, cookieContent);
    }
    public void putCookie(URI uri, String cookieName, String cookieContent) {
        cookieStore.add(uri, new HttpCookie(cookieName, cookieContent));
    }

    public Call newCall(Request request) {
        return httpClient.newCall(request);
    }
}
