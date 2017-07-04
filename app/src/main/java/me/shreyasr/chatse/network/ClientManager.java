package me.shreyasr.chatse.network;

import com.squareup.okhttp.OkHttpClient;

import me.shreyasr.chatse.App;
import me.shreyasr.chatse.network.cookie.PersistentCookieStore;

public class ClientManager {

    private static Client client
            = new Client(new OkHttpClient(), new PersistentCookieStore(App.get()));

    public static Client getClient() {
        return client;
    }
}
