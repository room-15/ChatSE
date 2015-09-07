package me.shreyasr.chatse.event;

import org.unbescape.html.HtmlEscape;

public class ChatEvent {

    public static final int EVENT_TYPE_MESSAGE = 1;
    public static final int EVENT_TYPE_EDIT = 2;
    public static final int EVENT_TYPE_JOIN = 3;
    public static final int EVENT_TYPE_STAR = 6;
    public static final int EVENT_TYPE_MENTION = 8;
    public static final int EVENT_TYPE_DELETE = 10;

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

    public boolean message_starred = false;

    public void setContent(String content) {
        this.content = HtmlEscape.unescapeHtml(content);
    }
}
