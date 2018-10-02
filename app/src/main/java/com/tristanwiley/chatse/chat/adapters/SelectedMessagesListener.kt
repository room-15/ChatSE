package com.tristanwiley.chatse.chat.adapters

interface SelectedMessagesListener {
    fun selectMessage(mId: Int)
    fun deselectMessage(mId: Int)
    fun isSelected(mId: Int): Boolean
}