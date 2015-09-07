package me.shreyasr.chatse.chat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.view.ViewPager;
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

public class ChatActivity extends AppCompatActivity implements ServiceConnection {

    private IncomingEventServiceBinder serviceBinder;

    private Handler networkHandler;
    private Handler uiThreadHandler = new Handler(Looper.getMainLooper());
    ChatFragmentPagerAdapter pagerAdapter;
    ViewPager viewPager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_chat);

        Intent serviceIntent = new Intent(this, IncomingEventService.class);
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);

        HandlerThread handlerThread = new HandlerThread("ChatActivityNetworkHandlerThread");
        handlerThread.start();
        networkHandler = new Handler(handlerThread.getLooper());

        viewPager = (ViewPager) this.findViewById(R.id.pager);
        pagerAdapter = new ChatFragmentPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
//        viewPager.setOffscreenPageLimit(7);
//        viewPager.setCurrentItem();
    }

    @Override public void onServiceConnected(ComponentName name, IBinder binder) {
        Logger.message(this.getClass(), "Service connect");
        serviceBinder = (IncomingEventServiceBinder) binder;

        loadChatFragment(new ChatRoom(Client.SITE_STACK_EXCHANGE, 1));
        loadChatFragment(new ChatRoom(Client.SITE_STACK_OVERFLOW, 15));
    }

    @Override public void onServiceDisconnected(ComponentName name) {
        Logger.message(this.getClass(), "Service disconnect");
    }

    private void loadChatFragment(final ChatRoom room) {
        networkHandler.post(new Runnable() {
            @Override public void run() {
                try {
                    addChatFragment(createChatFragment(room));
                } catch (IOException | JSONException e) {
                    Logger.exception(this.getClass(), "Failed to create chat fragment", e);
                }
            }
        });
    }

    private void addChatFragment(final ChatFragment fragment) {
        uiThreadHandler.post(new Runnable() {
            @Override public void run() {
                pagerAdapter.addFragment(fragment);
            }
        });
    }

    private ChatFragment createChatFragment(ChatRoom room) throws IOException, JSONException {
        if (serviceBinder != null) {
            IncomingEventService.RoomInfo roomInfo = serviceBinder.loadRoom(room);
            serviceBinder.joinRoom(room, roomInfo.fkey);
            ChatFragment chatFragment = ChatFragment.createInstance(room, roomInfo.name, roomInfo.fkey);
            serviceBinder.registerListener(room, chatFragment);

            return chatFragment;
        } else {
            throw new IOException("null serviceBinder");
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
}