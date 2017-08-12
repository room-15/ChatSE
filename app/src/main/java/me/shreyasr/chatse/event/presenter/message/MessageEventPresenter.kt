package me.shreyasr.chatse.event.presenter.message

import android.content.Context
import android.util.Log
import com.koushikdutta.ion.Ion
import me.shreyasr.chatse.App
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.event.ChatEvent
import me.shreyasr.chatse.event.presenter.EventPresenter
import java.util.*
import kotlin.collections.ArrayList

/**
 * One of the most important files
 * Where messages are added to an ArrayList of MessageEvents. This implements EventPresentor which overrides getEventsList and getUsersList which get the list of messages and users respectively.
 * Inside this class, events are handled based on the event_type that StackExchange assigns it.
 * List of event types can be found in the ChatEvent file.
 *
 * @param messages: A TreeSet containing all the messages for a Room
 * @param users; A hashmap of all the users in a Room
 *
 * Get these by calling .getEventsList() and .getUsersList()
 */
class MessageEventPresenter : EventPresenter<MessageEvent> {

    internal var messages = TreeSet<MessageEvent>()
    internal var users = hashMapOf<Long, MessageEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?) {
        //If the event type is not for leaving but definitely exists then continue so we can add the user to the UsersList
        if (event.event_type != ChatEvent.EVENT_TYPE_LEAVE && event.user_id != 0) {
            //If the user is already in the room, move them to the top
            if (users.containsKey(event.user_id.toLong())) {
                //Create a new MessageEvent and say it's for the users list
                val newEvent = MessageEvent(event)
                newEvent.isForUsersList = true

                //Put in the users list
                users.put(event.user_id.toLong(), newEvent)
            } else {
                //If the user is not in the users list get their profile picture
                val url: String
                //Determine what URL to use (StackOverflow or StackExchange)
                if (room?.site == App.SITE_STACK_OVERFLOW) {
                    url = "https://chat.stackoverflow.com/users/thumbs/${event.user_id}"
                } else {
                    url = "https://chat.stackexchange.com/users/thumbs/${event.user_id}"
                }
                //Make a call with Ion (for parsing simplicity) and parse the result
                Ion.with(context)
                        .load(url)
                        .asJsonObject()
                        .setCallback { e, result ->
                            //Ensure there's no error and the result is not null
                            if (e != null) {
                                Log.w("MessageEventPresenter", e.message.toString())
                            }
                            if (result != null) {
                                //Get the rooms array
                                val rooms = result.get("rooms").asJsonArray
                                //Find the current room from it
                                val r = rooms.find { it.asJsonObject.get("id").asInt == roomNum }
                                //Ensure the user is in the room
                                if (r != null) {
                                    //Get the profile picture for the user and add it to the user in the users list
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

        //Make sure the room is the correct room
        if (room?.num == event.room_id) {
            //Kotlin version of the switch statement, determine what to do with the event
            when (event.event_type) {
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
                        newMessage.message_stars = event.message_stars
                        newMessage.message_starred = event.message_starred
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
