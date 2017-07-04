package me.shreyasr.chatse.chat.service

import android.os.Binder
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.network.ClientManager
import org.json.JSONException
import java.io.IOException

/**
 * Exposes methods from the IncomingEventService.
 */
class IncomingEventServiceBinder internal constructor(private val service: IncomingEventService) : Binder() {

    fun registerListener(room: ChatRoom, listener: IncomingEventListener) {
        service.registerListener(room, listener)
    }

    /**
     * Load a specified room, getting the name of the room and the fkey.

     * @param room The room to load
     * *
     * @return A RoomInfo object, containing the room name and the fkey
     * *
     * @throws IOException If the page fails to load.
     */
    @Throws(IOException::class)
    fun loadRoom(room: ChatRoom): IncomingEventService.RoomInfo {
        return service.loadRoom(ClientManager.client, room)
    }

    /**
     * Joins a specified chat room.

     * @param room The chat room to join
     * *
     * @throws IOException   If the registration request or the websocket creation fails
     * *
     * @throws JSONException If the registration requests fails to parse the websocket url
     */
    @Throws(IOException::class, JSONException::class)
    fun joinRoom(room: ChatRoom, chatFkey: String) {
        service.joinRoom(ClientManager.client, room, chatFkey)
    }
}
