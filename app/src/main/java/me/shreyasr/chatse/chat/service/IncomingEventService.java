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
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.shreyasr.chatse.chat.ChatRoom;
import me.shreyasr.chatse.network.Client;
import me.shreyasr.chatse.util.Logger;

public class IncomingEventService extends Service
        implements ChatWebsocketListener.ServiceWebsocketListener {

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

    @Override public void onNewEvents(String site, JsonNode message) {
        for (MessageListenerHolder holder : listeners) {
            if (!holder.room.site.equals(site)) continue;
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

    @Override public void onConnect(String site, boolean success) {
        siteStatuses.put(site, WebsocketConnectionStatus.ESTABLISHED);
    }

    private enum WebsocketConnectionStatus { ESTABLISHED, CREATING, DISCONNECTED }
    private Map<String, WebsocketConnectionStatus> siteStatuses = new HashMap<>();

    RoomInfo loadRoom(Client client, ChatRoom room) throws IOException {
        Request chatPageRequest = new Request.Builder()
                .url(room.site + "/rooms/" + room.num)
                .build();
        Response chatPageResponse = client.newCall(chatPageRequest).execute();
        Document chatPage = Jsoup.parse(chatPageResponse.body().string());

        String fkey = chatPage.select("input[name=fkey]").attr("value");
        String name = chatPage.select("span[id=roomname]").text();

        Logger.message(this.getClass(), "Loaded room: " + name);

        return new RoomInfo(name, fkey);
    }

    void joinRoom(Client client, ChatRoom room, String chatFkey) throws IOException, JSONException {
        if (!siteStatuses.containsKey(room.site)) {
            siteStatuses.put(room.site, WebsocketConnectionStatus.DISCONNECTED);
        }
        String wsUrl = registerRoom(client, room, chatFkey);
        if (siteStatuses.get(room.site) != WebsocketConnectionStatus.ESTABLISHED) {
            siteStatuses.put(room.site, WebsocketConnectionStatus.CREATING);
            initWs(client, wsUrl, room.site);
        }
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
        wsCall.enqueue(new ChatWebsocketListener(site, this));
    }

    public class RoomInfo {

        public final String name;
        public final String fkey;

        RoomInfo(String name, String fkey) {
            this.name = name;
            this.fkey = fkey;
        }
    }
}

