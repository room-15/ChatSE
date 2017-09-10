package com.tristanwiley.chatse.event.presenter

import android.content.Context

/**
 * Interface for EventPresentor. Most important usage is MessageEventPresentor
 */
interface EventPresenter<out T> {

    fun addEvent(event: com.tristanwiley.chatse.event.ChatEvent, roomNum: Int, context: Context, room: com.tristanwiley.chatse.chat.ChatRoom?)

    fun getEventsList(): List<T>
    fun getUsersList(): List<T>
}
