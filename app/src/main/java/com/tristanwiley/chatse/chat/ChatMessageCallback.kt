package com.tristanwiley.chatse.chat

/**
 * Created by mauker on 26/11/2017.
 * Callback that's used in the ChatFragment to handle user actions on the messages.
 */
interface ChatMessageCallback {
    fun onReplyMessage(id: Int)
}