package me.shreyasr.chatse.event

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import timber.log.Timber
import java.io.IOException

class ChatEventGenerator {

    private val mapper = ObjectMapper()

    fun createEvent(json: JsonNode): ChatEvent? {
        var c: ChatEvent? = null
        try {
            c = mapper.readValue<ChatEvent>(json, ChatEvent::class.java)
        } catch (e: IOException) {
            Timber.e("Failed to map json", e)
        }

        return c
    }
}
