package com.tristanwiley.chatse.event.presenter.message

class MessageEvent(baseEvent: com.tristanwiley.chatse.event.ChatEvent) : Comparable<com.tristanwiley.chatse.event.presenter.message.MessageEvent> {
    var content: String?
    val timestamp: Long
    val id: Long
    var userId: Long
    var userName: String
    val roomId: Long
    val roomName: String
    val messageId: Int
    val parentId: Long
    val editCount: Int
    val onebox: Boolean
    val onebox_type: String
    var message_stars: Int
    val onebox_content: String
    var onebox_extra: String
    var message_starred: Boolean
    var isForUsersList: Boolean
    var email_hash: String
    internal var previous: com.tristanwiley.chatse.event.presenter.message.MessageEvent? = null

    init {
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
        this.message_stars = baseEvent.message_stars
        this.message_starred = baseEvent.message_starred
        this.onebox = baseEvent.message_onebox
        this.onebox_type = baseEvent.onebox_type
        this.onebox_content = baseEvent.onebox_content
        this.onebox_extra = baseEvent.onebox_extra
        this.isForUsersList = baseEvent.isForUsersList
        this.email_hash = baseEvent.email_hash
    }

    val isDeleted: Boolean
        get() = content == null || content == ""

    val isEdited: Boolean
        get() = (previous != null || editCount != 0) && !isDeleted

    override fun equals(other: Any?): Boolean {
        if (other !is com.tristanwiley.chatse.event.presenter.message.MessageEvent) return false
        val event = other as com.tristanwiley.chatse.event.presenter.message.MessageEvent?
        return this.messageId == event?.messageId
    }

    override fun compareTo(other: com.tristanwiley.chatse.event.presenter.message.MessageEvent): Int {
        if (this == other) {
//            Log.d("compareTo", "This is equal - 0")
            return 0
        }
        if (other.isForUsersList) {
            return other.userId.compareTo(this.userId)

        } else {
            return other.timestamp.compareTo(this.timestamp)

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
        result = 31 * result + onebox_type.hashCode()
        result = 31 * result + onebox_content.hashCode()
        result = 31 * result + (previous?.hashCode() ?: 0)
        return result
    }
}
