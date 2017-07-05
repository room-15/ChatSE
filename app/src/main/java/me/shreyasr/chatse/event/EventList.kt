package me.shreyasr.chatse.event

import me.shreyasr.chatse.event.presenter.UnfilteredEventPresenter
import me.shreyasr.chatse.event.presenter.message.MessageEventPresenter

/**
 * Maintains a list of events for a specific room.
 */
class EventList(private val roomNum: Int) {

    var unfilteredPresenter = UnfilteredEventPresenter()
    var messagePresenter = MessageEventPresenter()

    private val presenters = arrayOf(unfilteredPresenter, messagePresenter)

    fun addEvent(event: ChatEvent) {
        presenters.forEach { it.addEvent(event, roomNum) }
    }
}
