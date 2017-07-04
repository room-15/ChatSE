package me.shreyasr.chatse.event

import me.shreyasr.chatse.event.presenter.UnfilteredEventPresenter
import me.shreyasr.chatse.event.presenter.message.MessageEventPresenter

class EventList(private val roomNum: Int) {

    var unfilteredPresenter = UnfilteredEventPresenter()
    var messagePresenter = MessageEventPresenter()

    private val presenters = arrayOf(unfilteredPresenter, messagePresenter)

    fun addEvent(event: ChatEvent) {
        for (presenter in presenters) {
            presenter.addEvent(event, roomNum)
        }
    }
}
