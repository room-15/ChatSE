package me.shreyasr.chatse.chat.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.squareup.okhttp.ws.WebSocketCall
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.network.Client
import org.codehaus.jackson.JsonNode
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException
import java.util.*

class IncomingEventService : Service(), ChatWebSocketListener.ServiceWebsocketListener {
    private val listeners = ArrayList<MessageListenerHolder>()
    private val siteStatuses = HashMap<String, WebsocketConnectionStatus>()

    override fun onBind(intent: Intent): IBinder? {
        return IncomingEventServiceBinder(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
    }

    fun registerListener(room: ChatRoom, listener: IncomingEventListener) {
        //Just clearing for now, need to make sure the filter actually works 100% of the time.
        listeners.clear()
//        if (listeners.filter { room.num == it.room.num }.isEmpty()) {
            listeners.add(MessageListenerHolder(room, listener))
//        }
    }

    override fun onNewEvents(site: String, root: JsonNode) {
        for (holder in listeners) {
            if (holder.room.site != site) continue
            if (!root.has("r" + holder.room.num)) {
                Log.e("Current Room Element", holder.room.num.toString())
                Log.e("No room element", root.toString())
                return
            }
            val roomNode = root.get("r" + holder.room.num)
            if (roomNode.has("e")) {
                holder.listener.handleNewEvents(roomNode.get("e"))
            }
        }
    }

    override fun onConnect(site: String, success: Boolean) {
        siteStatuses.put(site, WebsocketConnectionStatus.ESTABLISHED)
    }

    @Throws(IOException::class)
    internal fun loadRoom(client: Client, room: ChatRoom): RoomInfo {
        val chatPageRequest = Request.Builder()
                .url(room.site + "/rooms/" + room.num)
                .build()
        val chatPageResponse = client.newCall(chatPageRequest).execute()
        val chatPage = Jsoup.parse(chatPageResponse.body().string())

        val fkey = chatPage.select("input[name=fkey]").attr("value")
        val name = chatPage.select("span[id=roomname]").text()

        Timber.i("Loaded room: $name")

        return RoomInfo(name, fkey)
    }

    @Throws(IOException::class, JSONException::class)
    internal fun joinRoom(client: Client, room: ChatRoom, chatFkey: String) {
        if (!siteStatuses.containsKey(room.site)) {
            siteStatuses.put(room.site, WebsocketConnectionStatus.DISCONNECTED)
        }
        val wsUrl = registerRoom(client, room, chatFkey)
        if (siteStatuses[room.site] != WebsocketConnectionStatus.ESTABLISHED) {
            siteStatuses.put(room.site, WebsocketConnectionStatus.CREATING)
            initWs(client, wsUrl, room.site)
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun registerRoom(client: Client, room: ChatRoom, chatFkey: String): String {
        val wsUrlRequestBody = FormEncodingBuilder()
                .add("roomid", room.num.toString())
                .add("fkey", chatFkey).build()
        val wsUrlRequest = Request.Builder()
                .url(room.site + "/ws-auth")
                .post(wsUrlRequestBody)
                .build()

        val wsRegisterResponse = client.newCall(wsUrlRequest).execute()
        val wsUrlJson = JSONObject(wsRegisterResponse.body().string())
        return wsUrlJson.getString("url")
    }

    @Throws(IOException::class)
    private fun initWs(client: Client, wsUrl: String, site: String) {
        val wsRequest = Request.Builder()
                .addHeader("User-Agent", Client.USER_AGENT)
                .addHeader("Sec-WebSocket-Extensions", "permessage-deflate")
                .addHeader("Sec-WebSocket-Extensions", "client_max_window_bits")
                .addHeader("Origin", site)
                .url(wsUrl + "?l=0")
                .build()
        val wsCall = WebSocketCall.create(client.httpClient, wsRequest)
        wsCall.enqueue(ChatWebSocketListener(site, this))
    }

    private enum class WebsocketConnectionStatus {
        ESTABLISHED, CREATING, DISCONNECTED
    }

    class MessageListenerHolder(val room: ChatRoom, val listener: IncomingEventListener)

    class RoomInfo internal constructor(val name: String, val fkey: String)
}

