package me.shreyasr.chatse.event.message;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import me.shreyasr.chatse.util.Logger;

public class MessageEventGenerator {

    ObjectMapper mapper = new ObjectMapper();

    public MessageEvent createMessageEvent(JsonNode json) {
        try {
            return mapper.readValue(json, MessageEvent.class);
        } catch (IOException e) {
            Logger.exception(this.getClass(), "Failed to map json", e);
        }
        return null;
    }
}
