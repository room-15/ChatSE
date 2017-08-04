package me.shreyasr.chatse.event.presenter

import android.content.Context
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.event.ChatEvent

/**
 * Interface for EventPresentor. Most important usage is MessageEventPresentor
 */
interface EventPresenter<out T> {

    fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?)

    fun getEventsList(): List<T>
    fun getUsersList(): List<T>
}
