package com.tristanwiley.chatse.event

import org.jsoup.Jsoup
import org.unbescape.html.HtmlEscape

/**
 * ChatEvent class that is used for all new messages, contains all parameters that gets json mapped to it
 *
 * @param event_type: An integer which signifies what type of event an object is
 * @param time_stamp: When the event occurred
 * @param room_id: ID of the room in which the event occurred
 * @param user_id: The ID of the user that did the event
 * @param user_name: The user's username
 * @param message_id: The ID of the message (if event_type is equal to 1)
 * @param show_parent: Unused
 * @param parent_id: Parent's ID
 * @param id = ID of event
 * @param room_name: Name of Room that event occurred
 * @param contents: Content of event
 * @param message_edits: Number of times the message was edited
 * @param message_stars: Number of times the message was starred
 * @param message_owner_stars: Unused
 * @param target_user_id: Unused
 * @param message_onebox: If the message is oneboxed (an image, wikipedia article, SO/SE post, etc.)
 * @param onebox_type: Type of oneboxed content
 * @param onebox_extra: Extra data from onebox
 * @param message_starred: If the message is starred
 * @param isForUsersList: If the message is for the list of users in MessageEventPresentor
 * @param email_hash: Email hash used for profile picture
 */
class ChatEvent {

    var event_type: Int = 0
    var time_stamp: Long = 0
    var room_id: Int = 0
    var user_id: Int = 0
    var user_name: String = ""
    var message_id: Int = 0

    var show_parent = false
    var parent_id = -1

    var id = -1

    var room_name: String = ""
    var contents: String = ""
    var message_edits = 0
    var message_stars = 0
    var message_owner_stars = 0
    var target_user_id = -1

    var message_onebox = false
    var onebox_type = ""
    var onebox_content = ""
    var onebox_extra = ""

    var message_starred = false
    var isForUsersList = false
    var email_hash = ""

    var star_timestamp = ""

    fun setContent(content: String) {
        val doc = Jsoup.parse(content, "http://chat.stackexchange.com/")
        val elements = doc.select("div")
        var shouldSetContent = true

        if (elements.size != 0) {
            val obType = elements[0].className()

            when {
                obType.contains("ob-message") -> println("This is a quote")
                obType.contains("ob-youtube") -> {
                    shouldSetContent = false
                    message_onebox = true
                    onebox_type = "youtube"
                    this.contents = elements[0].child(0).getElementsByClass("ob-youtube-title").text()
                    onebox_content = elements.select("img").attr("src")
                    onebox_extra = elements[0].child(0).attr("href")
                }
                obType.contains("ob-wikipedia") -> println("This is Wikipedia")
                obType.contains("ob-image") -> {
                    val url = elements.select("img").first().absUrl("src")
                    message_onebox = true
                    onebox_type = "image"
                    onebox_content = url
                }
                obType.contains("ob-tweet") -> {
                    message_onebox = true
                    onebox_type = "tweet"
                    val status = elements[2]
                    val writtenBy = elements[4]
                    onebox_content = ""
                    onebox_content += "<p>" + status.childNode(0).toString() + "</p>"
                    onebox_content += "<p>" + writtenBy.toString() + "</p>"
                    shouldSetContent = false
                    this.contents = onebox_content
                }
                else -> {
                    message_onebox = false
                    onebox_type = ""
                    onebox_content = ""
                }
            }
        }
        if (shouldSetContent) {
            this.contents = HtmlEscape.unescapeHtml(content)
        }
    }


    /**
     * Different types of event types and their ID
     */
    companion object {
        val EVENT_TYPE_MESSAGE = 1
        val EVENT_TYPE_EDIT = 2
        val EVENT_TYPE_JOIN = 3
        val EVENT_TYPE_LEAVE = 4
        val EVENT_TYPE_STAR = 6
        val EVENT_TYPE_MENTION = 8
        val EVENT_TYPE_DELETE = 10
    }
}
