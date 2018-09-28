package com.tristanwiley.chatse.event

import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.Jsoup
import org.unbescape.html.HtmlEscape

/**
 * ChatEvent class that is used for all new messages, contains all parameters that gets json mapped to it
 *
 * @property eventType: An integer which signifies what type of event an object is
 * @property timeStamp: When the event occurred
 * @property roomId: ID of the room in which the event occurred
 * @property userId: The ID of the user that did the event
 * @property userName: The user's username
 * @property messageId: The ID of the message (if eventType is equal to 1)
 * @property showParent: Unused
 * @property parentId: Parent's ID
 * @property id = ID of event
 * @property roomName: Name of Room that event occurred
 * @property contents: Content of event
 * @property messageEdits: Number of times the message was edited
 * @property messageStars: Number of times the message was starred
 * @property messageOwnerStars: Unused
 * @property targetUserId: Unused
 * @property messageOnebox: If the message is oneboxed (an image, wikipedia article, SO/SE post, etc.)
 * @property oneboxType: Type of oneboxed content
 * @property oneboxExtra: Extra data from onebox
 * @property messageStarred: If the message is starred
 * @property isForUsersList: If the message is for the list of users in MessageEventPresentor
 * @property emailHash: Email hash used for profile picture
 */
class ChatEvent {
    @JsonProperty("message_owner_stars")
    private var messageOwnerStars = 0
    @JsonProperty("target_user_id")
    private var targetUserId = -1
    @JsonProperty("show_parent")
    private var showParent = false

    @JsonProperty("message_starred")
    var messageStarred = false
    @JsonProperty("message_onebox")
    var messageOnebox = false

    @JsonProperty("onebox_content")
    var oneboxContent = ""
    @JsonProperty("star_timestamp")
    var starTimestamp = ""
    @JsonProperty("onebox_extra")
    var oneboxExtra = ""
    @JsonProperty("onebox_type")
    var oneboxType = ""
    @JsonProperty("email_hash")
    var emailHash = ""
    @JsonProperty("user_name")
    var userName = ""
    @JsonProperty("room_name")
    var roomName = ""

    @JsonProperty("message_edits")
    var messageEdits = 0
    @JsonProperty("message_stars")
    var messageStars = 0
    @JsonProperty("time_stamp")
    var timeStamp = 0L
    @JsonProperty("event_type")
    var eventType = 0
    @JsonProperty("message_id")
    var messageId = 0
    @JsonProperty("parent_id")
    var parentId = -1
    @JsonProperty("room_id")
    var roomId = 0
    @JsonProperty("user_id")
    var userId = 0

    var id = -1
    var contents: String = ""
    var isForUsersList = false


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
                    messageOnebox = true
                    oneboxType = "youtube"
                    this.contents = elements[0].child(0).getElementsByClass("ob-youtube-title").text()
                    oneboxContent = elements.select("img").attr("src")
                    oneboxExtra = elements[0].child(0).attr("href")
                }
                obType.contains("ob-wikipedia") -> println("This is Wikipedia")
                obType.contains("ob-image") -> {
                    val url = elements.select("img").first().absUrl("src")
                    messageOnebox = true
                    oneboxType = "image"
                    oneboxContent = url
                }
                obType.contains("ob-tweet") -> {
                    messageOnebox = true
                    oneboxType = "tweet"
                    val status = elements[2]
                    val writtenBy = elements[4]
                    oneboxContent = ""
                    oneboxContent += "<p>" + status.childNode(0).toString() + "</p>"
                    oneboxContent += "<p>" + writtenBy.toString() + "</p>"
                    shouldSetContent = false
                    this.contents = oneboxContent
                }
                else -> {
                    messageOnebox = false
                    oneboxType = ""
                    oneboxContent = ""
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
