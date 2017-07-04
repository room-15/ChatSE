package me.shreyasr.chatse.event;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.unbescape.html.HtmlEscape;

import me.shreyasr.chatse.util.Logger;

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
    public String contents = null;
    public int message_edits = 0;
    public int message_stars = 0;
    public int message_owner_stars = 0;
    public int target_user_id = -1;

    public boolean message_onebox = false;
    public String onebox_type = "";
    public String onebox_content = "";

    public boolean message_starred = false;

    public void setContent(String content) {

        Document doc = Jsoup.parse(content, "http://chat.stackexchange.com/");
        Elements elements = doc.select("div");

        if (elements.size() != 0) {
            String obType = elements.get(0).className();

            if (obType.contains("ob-message")) {
                System.out.println("This is a quote");
            } else if (obType.contains("ob-message")) {
                System.out.println("This is a Youtube Video");
            } else if (obType.contains("ob-wikipedia")) {
                System.out.println("This is Wikipedia");
            } else if (obType.contains("ob-image")) {
                String url = elements.select("img").first().absUrl("src");
                Logger.debug(getClass(), "ob-image: " + url);
                message_onebox = true;
                onebox_type = "image";
                onebox_content = url;
            } else {
                message_onebox = false;
                onebox_type = "";
                onebox_content = "";
            }
        }


        this.contents = HtmlEscape.unescapeHtml(content);
    }
}
