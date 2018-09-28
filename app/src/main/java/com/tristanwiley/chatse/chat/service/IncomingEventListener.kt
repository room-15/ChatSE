package com.tristanwiley.chatse.chat.service

import com.fasterxml.jackson.databind.JsonNode

interface IncomingEventListener {
    fun handleNewEvents(messagesJson: JsonNode)
}
