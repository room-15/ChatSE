package me.shreyasr.chatse.chat.service;

import android.util.Log;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketListener;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;

public class ChatWebSocketListener implements WebSocketListener {

    private ServiceWebsocketListener listener;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatWebSocketListener(ServiceWebsocketListener listener) {
        this.listener = listener;
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        Log.e("ws", "ws open");
        listener.onConnect(true);
    }

    @Override public void onFailure(IOException e, Response response) {
        Log.e("ws", "ws fail");
        listener.onConnect(false);
        Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
    }

    @Override public void onMessage(BufferedSource payload,
                                    WebSocket.PayloadType type) throws IOException {
        Log.e("ws", "ws message");
        String message = payload.readUtf8();
        payload.close();
        Log.e("ws", "recv: " + message);
        try {
            JsonNode root = mapper.readTree(message);
            listener.onNewEvents(root);
        } catch (IOException e) {
            Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override public void onPong(Buffer payload) {
        Log.e("ws", "ws pong");
    }

    @Override public void onClose(int code, String reason) {
        Log.e("ws", "ws close");
    }

    interface ServiceWebsocketListener {

        void onNewEvents(JsonNode root);
        void onConnect(boolean success);
    }
}
