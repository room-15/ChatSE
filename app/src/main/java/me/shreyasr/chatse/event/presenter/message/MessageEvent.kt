package me.shreyasr.chatse.event.presenter.message

import me.shreyasr.chatse.event.ChatEvent
import timber.log.Timber

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
        Timber.wtf(baseEvent.toString())
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

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + userName.hashCode()
        result = 31 * result + roomId.hashCode()
        result = 31 * result + roomName.hashCode()
        result = 31 * result + messageId
        result = 31 * result + parentId.hashCode()
        result = 31 * result + editCount
        result = 31 * result + onebox.hashCode()
        result = 31 * result + onebox_type.hashCode()
        result = 31 * result + onebox_content.hashCode()
        result = 31 * result + (previous?.hashCode() ?: 0)
        return result
    }
}
