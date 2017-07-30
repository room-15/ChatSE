package me.shreyasr.chatse.event.presenter

import android.content.Context
import me.shreyasr.chatse.event.ChatEvent

interface EventPresenter<out T> {

    fun addEvent(event: ChatEvent, roomNum: Int, context: Context)

    fun getEventsList(): List<T>
    fun getUsersList(): List<T>
}
