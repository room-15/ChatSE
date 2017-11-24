package com.tristanwiley.chatse.event.presenter

import android.content.Context
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import java.util.*

/**
 * An unfiltered event presenter, used alongside MessageEventPresentor
 */
class UnfilteredEventPresenter : EventPresenter<ChatEvent> {

    private val events = ArrayList<ChatEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?) {
        if (event.roomId == roomNum) {
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
