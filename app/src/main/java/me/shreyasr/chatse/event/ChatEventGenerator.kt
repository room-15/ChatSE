package me.shreyasr.chatse.event

import me.shreyasr.chatse.util.Logger
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import java.io.IOException

class ChatEventGenerator {

    private val mapper = ObjectMapper()

    fun createEvent(json: JsonNode): ChatEvent? {
        var c: ChatEvent? = null
        try {
            c = mapper.readValue<ChatEvent>(json, ChatEvent::class.java)
        } catch (e: IOException) {
            Logger.exception(this.javaClass, "Failed to map json", e)
        }

        return c
    }
}
