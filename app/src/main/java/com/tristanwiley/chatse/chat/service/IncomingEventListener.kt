package com.tristanwiley.chatse.chat.service

import org.codehaus.jackson.JsonNode

interface IncomingEventListener {
    fun handleNewEvents(messagesJson: JsonNode)
}
