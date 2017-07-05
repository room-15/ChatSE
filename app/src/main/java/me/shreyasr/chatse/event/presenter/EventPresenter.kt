package me.shreyasr.chatse.event.presenter

import me.shreyasr.chatse.event.ChatEvent

interface EventPresenter<out T> {

    fun addEvent(event: ChatEvent, roomNum: Int)

    fun getEventsList(): List<T>
}
