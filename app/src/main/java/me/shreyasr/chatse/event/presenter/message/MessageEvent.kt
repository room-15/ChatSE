package me.shreyasr.chatse.event.presenter.message

import android.util.Log
import me.shreyasr.chatse.event.ChatEvent

class MessageEvent(baseEvent: ChatEvent) : Comparable<MessageEvent> {


    val content: String?
    val timestamp: Long
    val id: Long
    val userId: Long
    val userName: String
    val roomId: Long
    val roomName: String
    val messageId: Int
    val parentId: Long
    val editCount: Int
    val onebox: Boolean
    val onebox_type: String
    val onebox_content: String
    internal var previous: MessageEvent? = null

    init {
        Log.wtf("THISSHIT", baseEvent.toString())
        this.content = baseEvent.contents
        this.timestamp = baseEvent.time_stamp
        this.id = baseEvent.id.toLong()
        this.userId = baseEvent.user_id.toLong()
        this.userName = baseEvent.user_name
        this.roomId = baseEvent.room_id.toLong()
        this.roomName = baseEvent.room_name
        this.messageId = baseEvent.message_id
        this.parentId = baseEvent.parent_id.toLong()
        this.editCount = baseEvent.message_edits
        this.onebox = baseEvent.message_onebox
        this.onebox_type = baseEvent.onebox_type
        this.onebox_content = baseEvent.onebox_content

    }

    val isDeleted: Boolean
        get() = content == null

    val isEdited: Boolean
        get() = (previous != null || editCount != 0) && !isDeleted

    override fun equals(other: Any?): Boolean {
        if (other !is MessageEvent) return false
        val event = other as MessageEvent?
        return this.messageId == event!!.messageId
    }

    override fun compareTo(other: MessageEvent): Int {
        if (this == other) {
            return 0
        }
        return -java.lang.Long.valueOf(timestamp)!!.compareTo(other.timestamp)
    }
}
