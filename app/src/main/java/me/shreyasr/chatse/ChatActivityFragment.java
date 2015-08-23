package me.shreyasr.chatse;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ws.WebSocketCall;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.shreyasr.chatse.event.message.MessageEvent;
import me.shreyasr.chatse.event.message.MessageEventGenerator;

public class ChatActivityFragment extends Fragment {

    @Bind(R.id.chat_input_text) EditText input;
    @Bind(R.id.chat_message_list) RecyclerView messageList;

    private String chatFkey = null;
    private String site = "http://chat.stackexchange.com";
    private int roomNum = 1;
    private OkHttpClient httpClient = Client.get();

    private Handler networkHandler;
    private Handler updateThread = new Handler();
    private MessageAdapter messageAdapter;
    private ObjectMapper mapper = new ObjectMapper();
    private MessageEventGenerator messageEventGenerator = new MessageEventGenerator();

    private int firstMessageTime;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandlerThread handlerThread = new HandlerThread("NetworkHandlerThread");
        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, view);

        messageAdapter = new MessageAdapter();
        messageList.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
        messageList.setAdapter(messageAdapter);

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    onSubmit();
                    return true;
                }
                return false;
            }
        });

        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    chatFkey = getChatFkey(httpClient, site);
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    JsonNode messages = getMessagesObject(httpClient, site, roomNum, 100);
                    firstMessageTime = messages.get("time").getIntValue();
                    handleNewEvents(messages.get("events"));
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    initWebsocket();
                } catch (IOException | JSONException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
        return view;
    }

    private void initWebsocket() throws IOException, JSONException {
        RequestBody wsUrlRequestBody = new FormEncodingBuilder()
                .add("roomid", String.valueOf(roomNum))
                .add("fkey", chatFkey).build();
        Request wsUrlRequest = new Request.Builder()
                .url("http://chat.stackexchange.com/ws-auth")
                .post(wsUrlRequestBody)
                .build();
        Response wsUrlResponse = httpClient.newCall(wsUrlRequest).execute();
        JSONObject wsUrlJson = new JSONObject(wsUrlResponse.body().string());
        Log.e("websocket", "url: " + wsUrlJson.getString("url"));
        String wsUrl = wsUrlJson.getString("url");// + "?l=" + firstMessageTime;
        Request wsRequest = new Request.Builder()
                .addHeader("User-Agent", Client.USER_AGENT)
                .addHeader("Sec-WebSocket-Extensions", "permessage-deflate")
                .addHeader("Sec-WebSocket-Extensions", "client_max_window_bits")
                .addHeader("Origin", "http://stackexchange.com")
                .url(wsUrl + "?l=" + firstMessageTime)
                .build();
        WebSocketCall wsCall = WebSocketCall.create(httpClient, wsRequest);
        wsCall.enqueue(new ChatWebSocketListener(ChatActivityFragment.this, mapper, roomNum));
        Log.e("ws", "init");
    }

    public void handleNewEvents(JsonNode messages) {
        final List<MessageEvent> events = new ArrayList<>();
        for (JsonNode message : messages) {
            events.add(messageEventGenerator.createMessageEvent(message));
        }
        updateThread.post(new Runnable() {
            @Override public void run() {
                messageAdapter.addMessages(events);
            }
        });
    }

    @OnClick(R.id.chat_input_submit) void onSubmit() {
        final String content = input.getText().toString();
        input.setText("");
        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    newMessage(httpClient, "http://chat.stackexchange.com", roomNum,
                            chatFkey, content);
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
    }

    private JsonNode getMessagesObject(OkHttpClient client,
                                       String site, int room, int count) throws IOException {
        RequestBody newMessageRequestBody = new FormEncodingBuilder()
                .add("since", String.valueOf(0))
                .add("mode", "Messages")
                .add("msgCount", String.valueOf(count))
                .add("fkey", chatFkey)
                .build();
        Request newMessageRequest = new Request.Builder()
                .url(site + "/chats/" + room + "/events")
                .post(newMessageRequestBody)
                .build();
        Response newMessageResponse = client.newCall(newMessageRequest).execute();
        return mapper.readTree(newMessageResponse.body().byteStream());
    }

    private void newMessage(OkHttpClient client, String site, int room,
                            String fkey, String message) throws IOException {
        RequestBody newMessageRequestBody = new FormEncodingBuilder()
                .add("text", message)
                .add("fkey", fkey)
                .build();
        Request newMessageRequest = new Request.Builder()
                .url(site + "/chats/" + room + "/messages/new/")
                .post(newMessageRequestBody)
                .build();
        Response newMessageResponse = client.newCall(newMessageRequest).execute();
        Log.v("new message", message);
    }

    private String getChatFkey(OkHttpClient client, String site) throws IOException {
        Request chatPageRequest = new Request.Builder()
                .url(site)
                .build();
        Response chatPageResponse = client.newCall(chatPageRequest).execute();
        return Jsoup.parse(chatPageResponse.body().string())
                .select("input[name=fkey]").attr("value");
    }
}
