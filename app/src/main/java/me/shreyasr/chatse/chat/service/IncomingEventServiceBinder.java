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
     * Load a specified room, getting the name of the room and the fkey.
     *
     * @param room The room to load
     * @return A RoomInfo object, containing the room name and the fkey
     * @throws IOException If the page fails to load.
     */
    public IncomingEventService.RoomInfo loadRoom(ChatRoom room) throws IOException {
        return service.loadRoom(ClientManager.getClient(), room);
    }

    /**
     * Joins a specified chat room.
     *
     * @param room The chat room to join
     * @throws IOException   If the registration request or the websocket creation fails
     * @throws JSONException If the registration requests fails to parse the websocket url
     */
    public void joinRoom(ChatRoom room, String chatFkey) throws IOException, JSONException {
        service.joinRoom(ClientManager.getClient(), room, chatFkey);
    }
}
