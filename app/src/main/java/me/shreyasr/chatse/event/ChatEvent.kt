package me.shreyasr.chatse.event

import me.shreyasr.chatse.util.Logger
import org.jsoup.Jsoup
import org.unbescape.html.HtmlEscape

class ChatEvent {

    var event_type: Int = 0
    var time_stamp: Long = 0
    var room_id: Int = 0
    var user_id: Int = 0
    lateinit var user_name: String
    var message_id: Int = 0

    var show_parent = false
    var parent_id = -1

    var id = -1

    lateinit var room_name: String
    lateinit var content: String
    var message_edits = 0
    var message_stars = 0
    var message_owner_stars = 0
    var target_user_id = -1

    var message_onebox = false
    var onebox_type = ""
    var onebox_content = ""

    var message_starred = false

    fun setChatContent(content: String) {

        val doc = Jsoup.parse(content, "http://chat.stackexchange.com/")
        val elements = doc.select("div")

        if (elements.size != 0) {
            val obType = elements[0].className()

            if (obType.contains("ob-message")) {
                println("This is a quote")
            } else if (obType.contains("ob-message")) {
                println("This is a Youtube Video")
            } else if (obType.contains("ob-wikipedia")) {
                println("This is Wikipedia")
            } else if (obType.contains("ob-image")) {
                val url = elements.select("img").first().absUrl("src")
                Logger.debug(javaClass, "ob-image: " + url)
                message_onebox = true
                onebox_type = "image"
                onebox_content = url
            } else {
                message_onebox = false
                onebox_type = ""
                onebox_content = ""
            }
        }


        this.content = HtmlEscape.unescapeHtml(content)
    }

    companion object {

        val EVENT_TYPE_MESSAGE = 1
        val EVENT_TYPE_EDIT = 2
        val EVENT_TYPE_JOIN = 3
        val EVENT_TYPE_STAR = 6
        val EVENT_TYPE_MENTION = 8
        val EVENT_TYPE_DELETE = 10
    }
}
