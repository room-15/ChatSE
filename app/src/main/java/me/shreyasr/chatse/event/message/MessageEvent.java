package me.shreyasr.chatse.event.message;

import android.support.annotation.NonNull;

public class MessageEvent implements Comparable<MessageEvent> {

    public int event_type;
    public long time_stamp;
    public int room_id;
    public int user_id;
    public String user_name;
    public int message_id;

    public boolean show_parent = false;
    public int parent_id = -1;

    public int id = -1;

    public String room_name;
    public String content = null;
    public int message_edits = 0;
    public int message_stars = 0;
    public int message_owner_stars = 0;
    public int target_user_id = -1;

//    MessageEvent(JSONObject json) {
//
//    }

    @Override public int compareTo(@NonNull MessageEvent other) {
        return -Long.valueOf(this.time_stamp).compareTo(other.time_stamp);
    }
}
