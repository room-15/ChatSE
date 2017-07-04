package me.shreyasr.chatse.event.presenter.message

import me.shreyasr.chatse.event.ChatEvent

class MessageEvent(baseEvent: ChatEvent) : Comparable<MessageEvent> {

    var content: String? = baseEvent.content
    val timestamp: Long = baseEvent.time_stamp
    val id: Long = baseEvent.id.toLong()
    val userId: Long = baseEvent.user_id.toLong()
    val userName: String = baseEvent.user_name
    val roomId: Long = baseEvent.room_id.toLong()
    val roomName: String = baseEvent.room_name
    val messageId: Int = baseEvent.message_id
    val parentId: Long = baseEvent.parent_id.toLong()
    val editCount: Int = baseEvent.message_edits
    val onebox: Boolean = baseEvent.message_onebox
    val onebox_type: String = baseEvent.onebox_type
    val onebox_content: String = baseEvent.onebox_content
    internal var previous: MessageEvent? = null

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
