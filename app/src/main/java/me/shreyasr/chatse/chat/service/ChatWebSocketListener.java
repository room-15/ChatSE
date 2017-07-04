package me.shreyasr.chatse.chat.service;

import android.util.Log;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketListener;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import me.shreyasr.chatse.util.Logger;
import okio.Buffer;
import okio.BufferedSource;

public class ChatWebSocketListener implements WebSocketListener {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServiceWebsocketListener listener;
    private final String site;

    public ChatWebSocketListener(String site, ServiceWebsocketListener listener) {
        this.listener = listener;
        this.site = site;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Logger.event(this.getClass(), "websocket open: " + site);
        listener.onConnect(site, true);
    }

    @Override
    public void onFailure(IOException e, Response response) {
        Logger.event(this.getClass(), "websocket fail: " + site);
        listener.onConnect(site, false);
        Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
    }

    @Override
    public void onMessage(BufferedSource payload,
                          WebSocket.PayloadType type) throws IOException {
        String message = payload.readUtf8();
        payload.close();
        Logger.event(this.getClass(), "websocket message: " + site + ": " + message);
        try {
            JsonNode root = mapper.readTree(message);
            listener.onNewEvents(site, root);
        } catch (IOException e) {
            Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public void onPong(Buffer payload) {
        Logger.event(this.getClass(), "websocket pong: " + site);
    }

    @Override
    public void onClose(int code, String reason) {
        Logger.event(this.getClass(), "websocket close: " + site + ": " + code + ", " + reason);
    }

    interface ServiceWebsocketListener {

        void onNewEvents(String site, JsonNode root);

        void onConnect(String site, boolean success);
    }
}
