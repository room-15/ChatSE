package me.shreyasr.chatse.event.presenter.message

import android.util.Log
import me.shreyasr.chatse.event.ChatEvent
import me.shreyasr.chatse.event.presenter.EventPresenter
import java.util.*

class MessageEventPresenter : EventPresenter<MessageEvent> {

    internal var messages = TreeSet<MessageEvent>()

    override fun addEvent(event: ChatEvent, roomNum: Int) {
        when (event.event_type) {
            ChatEvent.EVENT_TYPE_MESSAGE -> messages.add(MessageEvent(event))
            ChatEvent.EVENT_TYPE_EDIT, ChatEvent.EVENT_TYPE_DELETE -> {
                val newMessage = MessageEvent(event)
                val originalMessage = messages.floor(newMessage)
                if (originalMessage != newMessage) {
                    Log.w("MessageEventPresenter", "Attempting to edit nonexistent message")
                    return
                }
                newMessage.previous = originalMessage
                messages.remove(originalMessage)
                messages.add(newMessage)
            }
        }
    }

    override fun getEventsList(): List<MessageEvent> {
        return Collections.unmodifiableList(ArrayList(messages))
    }
}
