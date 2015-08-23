package me.shreyasr.chatse;

import android.util.Log;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketListener;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSource;

public class ChatWebSocketListener implements WebSocketListener {
    @Override public void onOpen(WebSocket webSocket, Response response) {
        Log.e("ws", "ws open");
//        Buffer buffer = new Buffer();
//        buffer.writeString("Hello!", Charset.forName("UTF-8"));
//        try {
//            webSocket.sendMessage(WebSocket.PayloadType.TEXT, buffer);
//        } catch (IOException e) {
//            Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
//        }
    }

    @Override public void onFailure(IOException e, Response response) {
        Log.e("ws", "ws fail");
        Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
    }

    @Override public void onMessage(BufferedSource payload,
                                    WebSocket.PayloadType type) throws IOException {
        Log.e("ws", "ws message");
        String message = payload.readUtf8();
        Log.e("ws", message);
        payload.close();
        /*@Override public void onCompleted(Exception ex, WebSocket webSocket) {
            Log.e("websocket", "we haz websocket");
            if (ex != null) {
                Log.e(ex.getClass().getSimpleName(), ex.getMessage(), ex);
                return;
            }
            webSocket.setStringCallback(new WebSocket.StringCallback() {
                @Override public void onStringAvailable(String s) {
                    Log.e("websocket", "Recv: " + s);
                    try {
                        JsonNode root = mapper.readTree(s);
                        if (!root.has("r" + roomNum)) {
                            Log.e("No room element", root.toString());
                            return;
                        }
                        JsonNode roomNode = root.get("r" + roomNum);
                        if (!roomNode.has("e")) return;

                        handleNewEvents(roomNode.get("e"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        */
    }

    @Override public void onPong(Buffer payload) {
        Log.e("ws", "ws pong");
    }

    @Override public void onClose(int code, String reason) {
        Log.e("ws", "ws close");
    }
}
