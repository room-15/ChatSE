package me.shreyasr.chatse.chat.service;

import android.os.Binder;

import org.json.JSONException;

import java.io.IOException;

import me.shreyasr.chatse.chat.ChatRoom;
import me.shreyasr.chatse.network.ClientManager;

/**
 * Exposes methods from the IncomingEventService.
 */
public class IncomingEventServiceBinder extends Binder {

    private IncomingEventService service;

    IncomingEventServiceBinder(IncomingEventService service) {
        this.service = service;
    }

    public void registerListener(ChatRoom room, IncomingEventListener listener) {
        service.registerListener(room, listener);
    }

    /**
     * Joins a specified chat room.
     *
     * @param room The chat room to join
     * @return The fkey for that chat room
     * @throws IOException If the page fails to load or the websocket request fails to send
     * @throws JSONException If the registration requests fails to parse the websocket url
     */
    public String joinRoom(ChatRoom room) throws IOException, JSONException {
        return service.joinRoom(ClientManager.getClient(), room);
    }
}
