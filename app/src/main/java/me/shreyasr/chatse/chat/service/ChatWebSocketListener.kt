package me.shreyasr.chatse.chat.service

import android.util.Log
import com.squareup.okhttp.Response
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import okio.Buffer
import okio.BufferedSource
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

import java.io.IOException

/**
 * WebSocketListener that listens to websocket
 */
class ChatWebSocketListener(private val site: String, private val listener: ServiceWebsocketListener) : WebSocketListener {
    private val mapper = ObjectMapper()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i("ChatWebSocketListener", "websocket open: " + site)
        listener.onConnect(site, true)
    }

    override fun onFailure(e: IOException, response: Response?) {
        listener.onConnect(site, false)
    }

    @Throws(IOException::class)
    override fun onMessage(payload: BufferedSource,
                           type: WebSocket.PayloadType) {
        val message = payload.readUtf8()
        payload.close()
        try {
            val root = mapper.readTree(message)
            listener.onNewEvents(site, root)
        } catch (e: IOException) {
            Log.e("ChatWebSocketListener", e.message)
        }

    }

    override fun onPong(payload: Buffer) {
        Log.i("ChatWebSocketListener", "websocket pong: $site")
    }

    override fun onClose(code: Int, reason: String) {
        Log.i("ChatWebSocketListener", "websocket close: $site: $code, $reason")
    }

    interface ServiceWebsocketListener {
        fun onNewEvents(site: String, root: JsonNode)

        fun onConnect(site: String, success: Boolean)
    }
}
