package me.shreyasr.chatse.chat;

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

    private ChatActivityFragment fragment;
    private final ObjectMapper mapper;
    private final int roomNum;

    public ChatWebSocketListener(ChatActivityFragment fragment, ObjectMapper mapper, int roomNum) {
        this.fragment = fragment;
        this.mapper = mapper;
        this.roomNum = roomNum;
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        Log.e("ws", "ws open");
    }

    @Override public void onFailure(IOException e, Response response) {
        Log.e("ws", "ws fail");
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
            if (!root.has("r" + roomNum)) {
                Log.e("No room element", root.toString());
                return;
            }
            JsonNode roomNode = root.get("r" + roomNum);
            if (!roomNode.has("e")) return;

            fragment.handleNewEvents(roomNode.get("e"));
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
}
