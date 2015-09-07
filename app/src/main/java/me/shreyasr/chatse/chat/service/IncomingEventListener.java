package me.shreyasr.chatse.chat.service;

import org.codehaus.jackson.JsonNode;

public interface IncomingEventListener {

    void handleNewEvents(JsonNode messagesJson);
}
