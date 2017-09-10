package com.tristanwiley.chatse.event.presenter

import android.content.Context
import java.util.*

/**
 * An unfiltered event presenter, used alongside MessageEventPresentor
 */
class UnfilteredEventPresenter : com.tristanwiley.chatse.event.presenter.EventPresenter<com.tristanwiley.chatse.event.ChatEvent> {


    val events = ArrayList<com.tristanwiley.chatse.event.ChatEvent>()

    override fun addEvent(event: com.tristanwiley.chatse.event.ChatEvent, roomNum: Int, context: Context, room: com.tristanwiley.chatse.chat.ChatRoom?) {
        if (event.room_id == roomNum) {
            events.add(event)
        }
    }

    override fun getEventsList(): List<com.tristanwiley.chatse.event.ChatEvent> {
        return Collections.unmodifiableList(ArrayList(events))
    }

    override fun getUsersList(): List<com.tristanwiley.chatse.event.ChatEvent> {
        return Collections.unmodifiableList(ArrayList(events))
    }
}
