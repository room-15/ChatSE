package me.shreyasr.chatse.chat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocketCall;

import org.codehaus.jackson.JsonNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.shreyasr.chatse.chat.ChatRoom;
import me.shreyasr.chatse.network.Client;

public class IncomingEventService extends Service
        implements ChatWebSocketListener.ServiceWebsocketListener {

    private static final String TAG = IncomingEventService.class.getSimpleName();
    private List<MessageListenerHolder> listeners = new ArrayList<>();

    public IncomingEventService() { }

    @Override
    public IBinder onBind(Intent intent) {
        return new IncomingEventServiceBinder(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    void registerListener(ChatRoom room, IncomingEventListener listener) {
        listeners.add(new MessageListenerHolder(room, listener));
    }

    class MessageListenerHolder {

        public final IncomingEventListener listener;
        public final ChatRoom room;

        public MessageListenerHolder(ChatRoom room, IncomingEventListener listener) {
            this.room = room;
            this.listener = listener;
        }
    }

    @Override public void onNewEvents(JsonNode message) {
        for (MessageListenerHolder holder : listeners) {
            if (!message.has("r" + holder.room.num)) {
                Log.e("No room element", message.toString());
                return;
            }
            JsonNode roomNode = message.get("r" + holder.room.num);
            if (roomNode.has("e")) {
                holder.listener.handleNewEvents(roomNode.get("e"));
            }
        }
    }

    @Override public void onConnect(boolean success) {
        hasWsConn = true;
        creatingWsConn = false;
    }

    private volatile boolean hasWsConn = false;
    private volatile boolean creatingWsConn = false;

    String joinRoom(Client client, ChatRoom room) throws IOException, JSONException {
        String chatFkey = getChatFkey(client, room.site);
        String wsUrl = registerRoom(client, room, chatFkey);
        if (!hasWsConn && !creatingWsConn) {
            creatingWsConn = true;
            initWs(client, wsUrl, room.site);
        }
        return chatFkey;
    }

    private String registerRoom(Client client, ChatRoom room, String chatFkey)
            throws IOException, JSONException {
        RequestBody wsUrlRequestBody = new FormEncodingBuilder()
                .add("roomid", String.valueOf(room.num))
                .add("fkey", chatFkey).build();
        Request wsUrlRequest = new Request.Builder()
                .url(room.site + "/ws-auth")
                .post(wsUrlRequestBody)
                .build();

        Response wsRegisterResponse = client.newCall(wsUrlRequest).execute();
        JSONObject wsUrlJson = new JSONObject(wsRegisterResponse.body().string());
        return wsUrlJson.getString("url");
    }

    private void initWs(Client client, String wsUrl, String site) throws IOException {
        Request wsRequest = new Request.Builder()
                .addHeader("User-Agent", Client.USER_AGENT)
                .addHeader("Sec-WebSocket-Extensions", "permessage-deflate")
                .addHeader("Sec-WebSocket-Extensions", "client_max_window_bits")
                .addHeader("Origin", site)
                .url(wsUrl + "?l=0")// + "?l=" + firstMessageTime;
                .build();
        WebSocketCall wsCall = WebSocketCall.create(client.getHttpClient(), wsRequest);
        wsCall.enqueue(new ChatWebSocketListener(this));
        Log.e("ws", "init");
    }

    private String getChatFkey(Client client, String site) throws IOException {
        Request chatPageRequest = new Request.Builder()
                .url(site)
                .build();
        Response chatPageResponse = client.newCall(chatPageRequest).execute();
        return Jsoup.parse(chatPageResponse.body().string())
                .select("input[name=fkey]").attr("value");
    }
}

