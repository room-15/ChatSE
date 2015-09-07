package me.shreyasr.chatse.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.shreyasr.chatse.App;
import me.shreyasr.chatse.R;
import me.shreyasr.chatse.chat.service.IncomingEventListener;
import me.shreyasr.chatse.event.ChatEvent;
import me.shreyasr.chatse.event.ChatEventGenerator;
import me.shreyasr.chatse.event.EventList;
import me.shreyasr.chatse.network.Client;
import me.shreyasr.chatse.network.ClientManager;
import me.shreyasr.chatse.util.Logger;

public class ChatFragment extends Fragment implements IncomingEventListener {

    private static final String EXTRA_ROOM = "room";
    private static final String EXTRA_FKEY = "fkey";

    public static ChatFragment createInstance(ChatRoom room, String fkey) {
        Bundle b = new Bundle(2);
        b.putParcelable(EXTRA_ROOM, room);
        b.putString(EXTRA_FKEY, fkey);

        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Bind(R.id.chat_input_text) EditText input;
    @Bind(R.id.chat_message_list) RecyclerView messageList;

    private String chatFkey;
    private ChatRoom room;

    private Client client = ClientManager.getClient();

    private Handler networkHandler;
    private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    private MessageAdapter messageAdapter;
    private ObjectMapper mapper = new ObjectMapper();
    private ChatEventGenerator chatEventGenerator = new ChatEventGenerator();
    private SharedPreferences prefs;

    private EventList events;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        chatFkey = args.getString(EXTRA_FKEY);
        room = args.getParcelable(EXTRA_ROOM);

        assert chatFkey != null;
        assert room != null;

        events = new EventList(room.num);

        prefs = App.getPrefs(getActivity());

        HandlerThread handlerThread = new HandlerThread("NetworkHandlerThread");
        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ButterKnife.bind(this, view);

        messageAdapter = new MessageAdapter(events, getActivity().getResources());
        messageList.setLayoutManager(
                new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
        messageList.setAdapter(messageAdapter);
        messageList.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.message_divider));

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
                    JsonNode messages = getMessagesObject(client, room, 50);
                    handleNewEvents(messages.get("events"));
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });

        input.requestFocus();
        return view;
    }

    public void handleNewEvents(JsonNode jsonEvents) {
        for (JsonNode jsonEvent : jsonEvents) {
            ChatEvent newEvent = chatEventGenerator.createEvent(jsonEvent);
            if (newEvent.room_id == room.num) {
                events.addEvent(newEvent);
            }
        }
        uiThreadHandler.post(new Runnable() {
            @Override public void run() {
                messageAdapter.update();
            }
        });
    }

    @OnClick(R.id.chat_input_submit) void onSubmit() {
        final String content = input.getText().toString();
        input.setText("");
        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    newMessage(client, room, chatFkey, content);
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
    }

    private JsonNode getMessagesObject(Client client, ChatRoom room, int count) throws IOException {
        RequestBody getMessagesRequestBody = new FormEncodingBuilder()
                .add("since", String.valueOf(0))
                .add("mode", "Messages")
                .add("msgCount", String.valueOf(count))
                .add("fkey", chatFkey)
                .build();
        Request getMessagesRequest = new Request.Builder()
                .url(room.site + "/chats/" + room.num + "/events")
                .post(getMessagesRequestBody)
                .build();
        Response getMessagesResponse = client.newCall(getMessagesRequest).execute();
        return mapper.readTree(getMessagesResponse.body().byteStream());
    }

    private void newMessage(Client client, ChatRoom room,
                            String fkey, String message) throws IOException {
        RequestBody newMessageRequestBody = new FormEncodingBuilder()
                .add("text", message)
                .add("fkey", fkey)
                .build();
        Request newMessageRequest = new Request.Builder()
                .url(room.site + "/chats/" + room.num + "/messages/new/")
                .post(newMessageRequestBody)
                .build();
        Response newMessageResponse = client.newCall(newMessageRequest).execute();
        Logger.event(this.getClass(), "New message");
    }

    public String getPageTitle() {
        return room != null ? room.toString() : "null";
    }
}
