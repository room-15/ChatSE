package me.shreyasr.chatse;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class Client {

    public static final String USER_AGENT
            = "Mozilla/5.0 (Windows NT 6.2; WOW64)"
            + "AppleWebKit/537.36 (KHTML, like Gecko)"
            + "Chrome/44.0.2403.155 Safari/537.36";

    private static final Client instance = new Client();
    public static OkHttpClient get() { return instance.httpClient; }

    private OkHttpClient httpClient = new OkHttpClient();
    private CookieManager cookieManager = new CookieManager();

    private Client() {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
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
}
