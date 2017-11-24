package com.tristanwiley.chatse.event.presenter.message

import android.content.Context
import android.util.Log
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import com.tristanwiley.chatse.event.presenter.EventPresenter
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

/**
 * One of the most important files
 * Where messages are added to an ArrayList of MessageEvents. This implements EventPresentor which overrides getEventsList and getUsersList which get the list of messages and users respectively.
 * Inside this class, events are handled based on the eventType that StackExchange assigns it.
 * List of event types can be found in the ChatEvent file.
 *
 * @property messages: A TreeSet containing all the messages for a Room
 * @property users; A hashmap of all the users in a Room
 *
 * Get these by calling .getEventsList() and .getUsersList()
 */
class MessageEventPresenter : EventPresenter<MessageEvent> {

    private var messages = TreeSet<MessageEvent>()
    private var users = hashMapOf<Long, MessageEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?) {
        //If the event type is not for leaving but definitely exists then continue so we can add the user to the UsersList
        if (event.eventType != ChatEvent.EVENT_TYPE_LEAVE && event.userId != 0) {
            //If the user is already in the room, move them to the top
            if (users.containsKey(event.userId.toLong())) {
                //Create a new MessageEvent and say it's for the users list
                val newEvent = MessageEvent(event)
                newEvent.isForUsersList = true

                //Put in the users list
                users.put(event.userId.toLong(), newEvent)
            } else {
                //If the user is not in the users list get their profile picture
                //Determine what URL to use (StackOverflow or StackExchange)
                val url: String = if (room?.site == Client.SITE_STACK_OVERFLOW) {
                    "https://chat.stackoverflow.com/users/thumbs/${event.userId}"
                } else {
                    "https://chat.stackexchange.com/users/thumbs/${event.userId}"
                }
                //Make a call with Ion (for parsing simplicity) and parse the result
                doAsync {
                    val client = ClientManager.client

                    val soChatPageRequest = Request.Builder()
                            .url(url)
                            .build()
                    val response = client.newCall(soChatPageRequest).execute()
                    val jsonData = response.body().string()
                    val result = JSONObject(jsonData)

                    //Get the rooms array
                    val rooms = result.getJSONArray("rooms")
                    //Find the current room from it
                    val r: JSONObject? = (0..(rooms.length() - 1))
                            .map { rooms.getJSONObject(it) }
                            .lastOrNull {
                                it.getInt("id") == roomNum
                            }
                    //Ensure the user is in the room
                    if (r != null) {
                        //Get the profile picture for the user and add it to the user in the users list
                        val newEvent = MessageEvent(event)
                        newEvent.isForUsersList = true
                        val imageUrl = result.getString("email_hash").replace("!", "")
                        if (imageUrl.contains(".")) {
                            newEvent.emailHash = imageUrl
                        } else {
                            newEvent.emailHash = "https://www.gravatar.com/avatar/$imageUrl"
                        }
                        users.put(newEvent.userId, newEvent)
                    }
                }
            }
        }

        //Make sure the room is the correct room
        if (room?.num == event.roomId) {
            //Kotlin version of the switch statement, determine what to do with the event
            when (event.eventType) {
            //If the event is a message, add it to the messages
                ChatEvent.EVENT_TYPE_MESSAGE -> messages.add(MessageEvent(event))
            //If the event is an edit, or a delete, then add that new event in place of the old.
                ChatEvent.EVENT_TYPE_EDIT, ChatEvent.EVENT_TYPE_DELETE -> {
                    val newMessage = MessageEvent(event)
                    val originalMessage = messages.floor(newMessage)
                    if (originalMessage != newMessage) {
                        Log.w("MessageEventPresenter", "Attempting to edit nonexistent message")
                        return
                    }
                    newMessage.previous = originalMessage
                    messages.remove(originalMessage)
                    messages.add(newMessage)
                }
            //If the event is a starred message, modify the original message with the stars to add it to the event
                ChatEvent.EVENT_TYPE_STAR -> {
                    val newMessage = MessageEvent(event)
                    val originalMessage = messages.floor(newMessage)
                    if (originalMessage != null) {
                        newMessage.userId = originalMessage.userId
                        newMessage.userName = originalMessage.userName
                        newMessage.messageStars = event.messageStars
                        newMessage.messageStarred = event.messageStarred
                        messages.remove(originalMessage)
                        messages.add(newMessage)
                    }
                }
            }
        }
    }

    //Used from adapter to get all the messages
    override fun getEventsList(): List<MessageEvent> {
        return Collections.unmodifiableList(ArrayList(messages))
    }

    //Used from adapter to get all the users in a room
    override fun getUsersList(): List<MessageEvent> {
        return Collections.unmodifiableList(ArrayList(users.values).sortedByDescending { it.timestamp })
    }
}