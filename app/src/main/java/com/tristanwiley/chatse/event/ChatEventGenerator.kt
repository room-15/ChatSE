package com.tristanwiley.chatse.event

import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

/**
 * Generates an event for a JSON payload.
 */
class ChatEventGenerator {

    //An ObjectMapper used to map incoming json to ChatEvent objects
    private val mapper = ObjectMapper()

    /**
     * @param json: is a JSONNode that is take in and mapped to a ChatEvent
     * @return a ChatEvent
     */
    fun createEvent(json: JsonNode): ChatEvent? {
        var c: ChatEvent? = null
        try {
            c = mapper.readValue<ChatEvent>(json.toString(), ChatEvent::class.java)
        } catch (e: IOException) {
            Log.e("ChatEventGenerator", "Failed to map json", e)
        }

        return c
    }
}
