package me.shreyasr.chatse.chat.service

import android.util.Log
import com.squareup.okhttp.Response
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import me.shreyasr.chatse.util.Logger
import okio.Buffer
import okio.BufferedSource
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import java.io.IOException

class ChatWebSocketListener(private val site: String, private val listener: ServiceWebsocketListener) : WebSocketListener {

    private val mapper = ObjectMapper()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Logger.event(this.javaClass, "websocket open: " + site)
        listener.onConnect(site, true)
    }

    override fun onFailure(e: IOException, response: Response) {
        Logger.event(this.javaClass, "websocket fail: " + site)
        listener.onConnect(site, false)
        Log.e(e.javaClass.simpleName, e.message, e)
    }

    @Throws(IOException::class)
    override fun onMessage(payload: BufferedSource,
                           type: WebSocket.PayloadType) {
        val message = payload.readUtf8()
        payload.close()
        Logger.event(this.javaClass, "websocket message: $site: $message")
        try {
            val root = mapper.readTree(message)
            listener.onNewEvents(site, root)
        } catch (e: IOException) {
            Log.e(e.javaClass.simpleName, e.message, e)
        }

    }

    override fun onPong(payload: Buffer) {
        Logger.event(this.javaClass, "websocket pong: " + site)
    }

    override fun onClose(code: Int, reason: String) {
        Logger.event(this.javaClass, "websocket close: $site: $code, $reason")
    }

    interface ServiceWebsocketListener {

        fun onNewEvents(site: String, root: JsonNode)

        fun onConnect(site: String, success: Boolean)
    }
}
