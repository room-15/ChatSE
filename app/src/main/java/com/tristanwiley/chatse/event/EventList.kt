package com.tristanwiley.chatse.event

import android.content.Context

/**
 * Maintains a list of events for a specific room.
 */
class EventList(private val roomNum: Int) {

    //List of unfilteredEvents
    var unfilteredPresenter = com.tristanwiley.chatse.event.presenter.UnfilteredEventPresenter()
    //List of events used for messages
    var messagePresenter = com.tristanwiley.chatse.event.presenter.message.MessageEventPresenter()

    //Array of the unfiltered and filtered events
    private val presenters = arrayOf(unfilteredPresenter, messagePresenter)

    fun addEvent(event: com.tristanwiley.chatse.event.ChatEvent, context: Context, room: com.tristanwiley.chatse.chat.ChatRoom?) {
        presenters.forEach { it.addEvent(event, roomNum, context, room) }
    }
}
