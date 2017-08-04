package me.shreyasr.chatse.chat.service

import com.squareup.okhttp.Response
import com.squareup.okhttp.ws.WebSocket
import com.squareup.okhttp.ws.WebSocketListener
import okio.Buffer
import okio.BufferedSource
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import timber.log.Timber
import java.io.IOException

/**
 * WebSocketListener that listens to websocket
 */
class ChatWebSocketListener(private val site: String, private val listener: ServiceWebsocketListener) : WebSocketListener {
    private val mapper = ObjectMapper()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.i("websocket open: " + site)
        listener.onConnect(site, true)
    }

    override fun onFailure(e: IOException, response: Response?) {
        Timber.i("websocket fail: $site")
        listener.onConnect(site, false)
        Timber.e(e)
    }

    @Throws(IOException::class)
    override fun onMessage(payload: BufferedSource,
                           type: WebSocket.PayloadType) {
        val message = payload.readUtf8()
        payload.close()
        Timber.i("websocket message: $site: $message")
        try {
            val root = mapper.readTree(message)
            listener.onNewEvents(site, root)
        } catch (e: IOException) {
            Timber.e(e)
        }

    }

    override fun onPong(payload: Buffer) {
        Timber.i("websocket pong: $site")
    }

    override fun onClose(code: Int, reason: String) {
        Timber.i("websocket close: $site: $code, $reason")
    }

    interface ServiceWebsocketListener {
        fun onNewEvents(site: String, root: JsonNode)

        fun onConnect(site: String, success: Boolean)
    }
}
