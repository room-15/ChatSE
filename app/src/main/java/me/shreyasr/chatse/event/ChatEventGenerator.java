package me.shreyasr.chatse.event;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import me.shreyasr.chatse.util.Logger;

public class ChatEventGenerator {

    private ObjectMapper mapper = new ObjectMapper();

    public ChatEvent createEvent(JsonNode json) {
        try {
            return mapper.readValue(json, ChatEvent.class);
        } catch (IOException e) {
            Logger.exception(this.getClass(), "Failed to map json", e);
        }
        return null;
    }
}
