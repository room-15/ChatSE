package me.shreyasr.chatse.event.presenter.message;

import android.support.annotation.NonNull;

import me.shreyasr.chatse.event.ChatEvent;

public class MessageEvent implements Comparable<MessageEvent> {

    public final String content;
    public final long timestamp;
    public final long id;
    public final long userId;
    public final String userName;
    public final long roomId;
    public final String roomName;
    public final int messageId;
    public final long parentId;
    public final int editCount;
    public final boolean onebox;
    public final String onebox_type;
    public final String onebox_content;
    MessageEvent previous;
    public MessageEvent(ChatEvent baseEvent) {
        this.content = baseEvent.contents;
        this.timestamp = baseEvent.time_stamp;
        this.id = baseEvent.id;
        this.userId = baseEvent.user_id;
        this.userName = baseEvent.user_name;
        this.roomId = baseEvent.room_id;
        this.roomName = baseEvent.room_name;
        this.messageId = baseEvent.message_id;
        this.parentId = baseEvent.parent_id;
        this.editCount = baseEvent.message_edits;
        this.onebox = baseEvent.message_onebox;
        this.onebox_type = baseEvent.onebox_type;
        this.onebox_content = baseEvent.onebox_content;
    }

    public boolean isDeleted() {
        return content == null;
    }

    public boolean isEdited() {
        return (previous != null || editCount != 0) && !isDeleted();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageEvent)) return false;
        MessageEvent event = (MessageEvent) o;
        return this.messageId == event.messageId;
    }

    @Override
    public int compareTo(@NonNull MessageEvent other) {
        if (this.equals(other)) {
            return 0;
        }
        return -Long.valueOf(timestamp).compareTo(other.timestamp);
    }
}
