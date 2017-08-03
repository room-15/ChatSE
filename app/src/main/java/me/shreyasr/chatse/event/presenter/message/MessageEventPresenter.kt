package me.shreyasr.chatse.event.presenter.message

import android.content.Context
import android.util.Log
import com.koushikdutta.ion.Ion
import me.shreyasr.chatse.chat.ChatActivity
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.chat.Room
import me.shreyasr.chatse.event.ChatEvent
import me.shreyasr.chatse.event.presenter.EventPresenter
import me.shreyasr.chatse.network.Client
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList


class MessageEventPresenter : EventPresenter<MessageEvent> {

    internal var messages = TreeSet<MessageEvent>()
    internal var users = hashMapOf<Long, MessageEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?) {
        if (event.event_type != ChatEvent.EVENT_TYPE_LEAVE && event.user_id != 0) {
            if (users.containsKey(event.user_id.toLong())) {
                val newEvent = MessageEvent(event)
                newEvent.isForUsersList = true
                users.put(event.user_id.toLong(), newEvent)
            } else {
                val url: String
                if (room?.site == Client.SITE_STACK_OVERFLOW) {
                    url = "https://chat.stackoverflow.com/users/thumbs/${event.user_id}"
                } else {
                    url = "https://chat.stackexchange.com/users/thumbs/${event.user_id}"
                }
                Ion.with(context)
                        .load(url)
                        .asJsonObject()
                        .setCallback { e, result ->
                            if (e != null) {
                                Log.w("MessageEventPresenter", e.message.toString())
                            }
                            if (result != null) {
                                val rooms = result.get("rooms").asJsonArray
                                val r = rooms.find { it.asJsonObject.get("id").asInt == roomNum }
                                if (r != null) {
                                    val newEvent = MessageEvent(event)
                                    newEvent.isForUsersList = true
                                    val image_url = result.get("email_hash").asString.replace("!", "")
                                    if (image_url.contains(".")) {
                                        newEvent.email_hash = image_url
                                    } else {
                                        newEvent.email_hash = "https://www.gravatar.com/avatar/$image_url"
                                    }
                                    users.put(newEvent.userId, newEvent)
                                }
                            }
                        }
            }
        }
        if (room?.num == event.room_id) {
            when (event.event_type) {
                ChatEvent.EVENT_TYPE_MESSAGE -> messages.add(MessageEvent(event))
                ChatEvent.EVENT_TYPE_EDIT, ChatEvent.EVENT_TYPE_DELETE -> {
                    val newMessage = MessageEvent(event)
                    val originalMessage = messages.floor(newMessage)
                    if (originalMessage != newMessage) {
                        Timber.w("Attempting to edit nonexistent message")
                        return
                    }
                    newMessage.previous = originalMessage
                    messages.remove(originalMessage)
                    messages.add(newMessage)
                }
                ChatEvent.EVENT_TYPE_STAR -> {
                    val newMessage = MessageEvent(event)
                    val originalMessage = messages.floor(newMessage)
                    if (originalMessage != null) {
                        newMessage.userId = originalMessage.userId
                        newMessage.userName = originalMessage.userName
                        newMessage.message_stars = event.message_stars
                        newMessage.message_starred = event.message_starred
                        messages.remove(originalMessage)
                        messages.add(newMessage)
                    }
                }
                ChatEvent.EVENT_TYPE_MENTION -> {
                }
            }
            when (event.event_type) {
                ChatEvent.EVENT_TYPE_JOIN -> {
                    val userObj = MessageEvent(event)
                    userObj.content = "Someone just joined"
                    users.put(userObj.userId, userObj)
                    Log.e("JOINED", userObj.userId.toString())
                    if (userObj.roomId == roomNum.toLong()) {
                        (context as ChatActivity).addRoom(Room(userObj.roomName, userObj.roomId, 0))
                    }
                }
            }
        }
    }

    override fun getEventsList(): List<MessageEvent> {
        return Collections.unmodifiableList(ArrayList(messages))
    }

    override fun getUsersList(): List<MessageEvent> {
        return Collections.unmodifiableList(ArrayList(users.values).sortedByDescending { it.timestamp })
    }
}
