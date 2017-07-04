package me.shreyasr.chatse.event.presenter

import me.shreyasr.chatse.event.ChatEvent

interface EventPresenter<T> {

    fun addEvent(event: ChatEvent, roomNum: Int)

    val events: List<T>
}
