package me.shreyasr.chatse.chat.service

import org.codehaus.jackson.JsonNode

interface IncomingEventListener {
    fun handleNewEvents(messagesJson: JsonNode)
}
