package me.shreyasr.chatse.event.presenter.message;

import android.support.annotation.NonNull;
import android.util.Log;

import me.shreyasr.chatse.event.ChatEvent;

public class MessageEvent implements Comparable<MessageEvent> {

    MessageEvent last;

    public final String content;
    public final long timestamp;
    public final long id;
    public final long userId;
    public final String userName;
    public final long roomId;
    public final String roomName;
    public final int messageId;
    public final long parentId;

    public MessageEvent(ChatEvent baseEvent) {
        this.content = baseEvent.content;
        this.timestamp = baseEvent.time_stamp;
        this.id = baseEvent.id;
        this.userId = baseEvent.user_id;
        this.userName = baseEvent.user_name;
        this.roomId = baseEvent.room_id;
        this.roomName = baseEvent.room_name;
        this.messageId = baseEvent.message_id;
        this.parentId = baseEvent.parent_id;
    }

    @Override public boolean equals(Object o) {
        if (!(o instanceof MessageEvent)) return false;
        MessageEvent event = (MessageEvent) o;
        return this.messageId == event.messageId;
    }

    @Override public int compareTo(@NonNull MessageEvent other) {
        if (this.equals(other)) {
            return 0;
        }
        return -Long.valueOf(timestamp).compareTo(other.timestamp);
    }
}
