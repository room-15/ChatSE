package me.shreyasr.chatse.event.message;

import android.util.Log;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class MessageEventGenerator {

    ObjectMapper mapper = new ObjectMapper();

    public MessageEvent createMessageEvent(JsonNode json) {
        try {
            return mapper.readValue(json, MessageEvent.class);
        } catch (IOException e) {
            Log.e(MessageEventGenerator.class.getSimpleName(), e.getMessage(), e);
        }
        return null;
    }
}
