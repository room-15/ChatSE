package com.tristanwiley.chatse.event.presenter

import android.content.Context
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent

/**
 * Interface for EventPresentor. Most important usage is MessageEventPresentor
 */
interface EventPresenter<out T> {

    fun addEvent(event: ChatEvent, roomNum: Int, context: Context, room: ChatRoom?)

    fun getEventsList(): List<T>
    fun getUsersList(): List<T>
}
