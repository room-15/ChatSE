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
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.shreyasr.chatse.App;
import me.shreyasr.chatse.R;
import me.shreyasr.chatse.chat.service.IncomingEventListener;
import me.shreyasr.chatse.event.message.MessageEvent;
import me.shreyasr.chatse.event.message.MessageEventGenerator;
import me.shreyasr.chatse.network.Client;
import me.shreyasr.chatse.network.ClientManager;

public class ChatActivityFragment extends Fragment implements IncomingEventListener {

    private static final String EXTRA_ROOM = "room";
    private static final String EXTRA_FKEY = "fkey";

    public static ChatActivityFragment createInstance(ChatRoom room, String fkey) {
        Bundle b = new Bundle(2);
        b.putParcelable(EXTRA_ROOM, room);
        b.putString(EXTRA_FKEY, fkey);

        ChatActivityFragment fragment = new ChatActivityFragment();
        fragment.setArguments(b);
        return fragment;
    }

    @Bind(R.id.chat_input_text) EditText input;
    @Bind(R.id.chat_message_list) RecyclerView messageList;

    private String chatFkey;
    private ChatRoom room;

    private Client client = ClientManager.getClient();

    private Handler networkHandler;
    private Handler updateThread = new Handler(Looper.getMainLooper());
    private MessageAdapter messageAdapter;
    private ObjectMapper mapper = new ObjectMapper();
    private MessageEventGenerator messageEventGenerator = new MessageEventGenerator();
    private SharedPreferences prefs;

    private int firstMessageTime;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        chatFkey = args.getString(EXTRA_FKEY);
        room = args.getParcelable(EXTRA_ROOM);

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

        messageAdapter = new MessageAdapter();
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
                    JsonNode messages = getMessagesObject(client, room, 100);
                    firstMessageTime = messages.get("time").getIntValue();
                    handleNewEvents(messages.get("events"));
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
        return view;
    }

    public void handleNewEvents(JsonNode messages) {
        final List<MessageEvent> events = new ArrayList<>();
        for (JsonNode message : messages) {
            MessageEvent newMessageEvent = messageEventGenerator.createMessageEvent(message);
            if (newMessageEvent.room_id == room.num) {
                events.add(newMessageEvent);
            }
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
                    newMessage(client, room, chatFkey, content);
                } catch (IOException e) {
                    Log.e(e.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        });
    }

    private JsonNode getMessagesObject(Client client, ChatRoom room, int count) throws IOException {
        RequestBody newMessageRequestBody = new FormEncodingBuilder()
                .add("since", String.valueOf(0))
                .add("mode", "Messages")
                .add("msgCount", String.valueOf(count))
                .add("fkey", chatFkey)
                .build();
        Request newMessageRequest = new Request.Builder()
                .url(room.site + "/chats/" + room.num + "/events")
                .post(newMessageRequestBody)
                .build();
        Response newMessageResponse = client.newCall(newMessageRequest).execute();
        return mapper.readTree(newMessageResponse.body().byteStream());
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
        Log.e("newmssageresposne", newMessageResponse.body().string());
        Log.v("new message", message);
    }
}
