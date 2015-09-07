package me.shreyasr.chatse.chat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;

import java.io.IOException;

import me.shreyasr.chatse.R;
import me.shreyasr.chatse.chat.service.IncomingEventService;
import me.shreyasr.chatse.chat.service.IncomingEventServiceBinder;
import me.shreyasr.chatse.network.Client;
import me.shreyasr.chatse.util.Logger;

public class ChatActivity extends AppCompatActivity {

    private IncomingEventServiceBinder serviceBinder;
    private Handler networkHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, IncomingEventService.class);
        this.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        HandlerThread handlerThread = new HandlerThread("ChatActivityNetworkHandlerThread");
        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());

        if(savedInstanceState == null) {
//            try {
//                createChatFragment(new ChatRoom(Client.SITE_STACK_EXCHANGE, 16));
//            } catch (IOException | JSONException e) {
//                Logger.exception(this.getClass(), "Failed to create chat fragment", e);
//            }
        }
    }

    private void createChatFragment(ChatRoom room) throws IOException, JSONException {
        if (serviceBinder != null) {
            String fkey = serviceBinder.joinRoom(room);
            ChatActivityFragment chatFragment = ChatActivityFragment.createInstance(room, fkey);
            serviceBinder.registerListener(room, chatFragment);

            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, chatFragment)
                    .commit();
        } else {
            Logger.exception(this.getClass(), "Null serviceBinder", null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            serviceBinder = (IncomingEventServiceBinder) binder;
            Logger.message(this.getClass(), "Service connect");
            networkHandler.post(new Runnable() {
                @Override public void run() {
                    try {
                        createChatFragment(new ChatRoom(Client.SITE_STACK_EXCHANGE, 1));
                    } catch (IOException | JSONException e) {
                        Logger.exception(this.getClass(), "Failed to create chat fragment", e);
                    }
                }
            });
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            Logger.message(this.getClass(), "Service disconnect");
        }
    };
}
