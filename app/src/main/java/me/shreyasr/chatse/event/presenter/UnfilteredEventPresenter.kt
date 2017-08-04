package me.shreyasr.chatse.event.presenter

import android.content.Context
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.event.ChatEvent
import java.util.*

/**
 * An unfiltered event presenter, used alongside MessageEventPresentor
 */
class UnfilteredEventPresenter : EventPresenter<ChatEvent> {


    val events = ArrayList<ChatEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?) {
        if (event.room_id == roomNum) {
            events.add(event)
        }
    }

    override fun getEventsList(): List<ChatEvent> {
        return Collections.unmodifiableList(ArrayList(events))
    }

    override fun getUsersList(): List<ChatEvent> {
        return Collections.unmodifiableList(ArrayList(events))
    }
}
