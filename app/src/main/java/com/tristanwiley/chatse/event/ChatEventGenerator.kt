package com.tristanwiley.chatse.event

import android.util.Log
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

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
    fun createEvent(json: JsonNode): com.tristanwiley.chatse.event.ChatEvent? {
        var c: com.tristanwiley.chatse.event.ChatEvent? = null
        try {
            c = mapper.readValue<com.tristanwiley.chatse.event.ChatEvent>(json, com.tristanwiley.chatse.event.ChatEvent::class.java)
        } catch (e: IOException) {
            Log.e("ChatEventGenerator", "Failed to map json", e)
        }

        return c
    }
}
