package com.tristanwiley.chatse.event.presenter.message

import com.tristanwiley.chatse.event.ChatEvent

class MessageEvent(baseEvent: ChatEvent) : Comparable<MessageEvent> {
    var content: String? = baseEvent.contents
    val timestamp: Long = baseEvent.timeStamp
    val id: Long = baseEvent.id.toLong()
    var userId: Long = baseEvent.userId.toLong()
    var userName: String = baseEvent.userName
    private val roomId: Long = baseEvent.roomId.toLong()
    private val roomName: String = baseEvent.roomName
    val messageId: Int = baseEvent.messageId
    private val parentId: Long = baseEvent.parentId.toLong()
    private val editCount: Int = baseEvent.messageEdits
    val onebox: Boolean = baseEvent.messageOnebox
    val oneboxType: String = baseEvent.oneboxType
    var messageStars: Int = baseEvent.messageStars
    val oneboxContent: String = baseEvent.oneboxContent
    var oneboxExtra: String = baseEvent.oneboxExtra
    var messageStarred: Boolean = baseEvent.messageStarred
    var isForUsersList: Boolean = baseEvent.isForUsersList
    var emailHash: String = baseEvent.emailHash
    var starTimestamp: String = baseEvent.starTimestamp
    internal var previous: MessageEvent? = null

    val isDeleted: Boolean
        get() = content == null || content == ""

    val isEdited: Boolean
        get() = (previous != null || editCount != 0) && !isDeleted

    override fun equals(other: Any?): Boolean {
        if (other !is MessageEvent) return false
        val event = other as MessageEvent?
        return this.messageId == event?.messageId
    }

    override fun compareTo(other: MessageEvent): Int {
        if (this == other) {
//            Log.d("compareTo", "This is equal - 0")
            return 0
        }

        return if (other.isForUsersList) {
            other.userId.compareTo(this.userId)

        } else {
            other.timestamp.compareTo(this.timestamp)

        }
//        Log.d("compareTo", "This is not equal, comparing timestamps")
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
        result = 31 * result + oneboxType.hashCode()
        result = 31 * result + oneboxContent.hashCode()
        result = 31 * result + (previous?.hashCode() ?: 0)
        return result
    }
}
