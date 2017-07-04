package me.shreyasr.chatse.event.presenter

import me.shreyasr.chatse.event.ChatEvent
import java.util.*

class UnfilteredEventPresenter : EventPresenter<ChatEvent> {

    override val events = ArrayList<ChatEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int) {
        if (event.room_id == roomNum) {
            events.add(event)
        }
    }

    fun getEventsList(): List<ChatEvent> {
        return Collections.unmodifiableList(ArrayList(events))
    }
}
