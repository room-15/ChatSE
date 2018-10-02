package com.tristanwiley.chatse.chat.service

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.squareup.okhttp.ws.WebSocketCall
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.network.Client
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup

import java.io.IOException
import java.util.*

/**
 * Service that handles incoming events and listens to the websocket
 */
class IncomingEventService : Service(), ChatWebSocketListener.ServiceWebsocketListener {
    private val listeners = ArrayList<MessageListenerHolder>()
    private val siteStatuses = HashMap<String, WebsocketConnectionStatus>()

    override fun onBind(intent: Intent): IBinder? {
        return IncomingEventServiceBinder(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IncomingEventService", "onDestroy")
    }

    fun registerListener(room: ChatRoom, listener: IncomingEventListener) {
        listeners.clear()
        listeners.add(MessageListenerHolder(room, listener))
    }

    private fun isAppInForeground(context: Context): Boolean {
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
            return true
        } else if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING ||
                appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED) {
            return false
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        //TODO: This is deprecated, Tinker and test with this - https://stackoverflow.com/a/35423996/3131147
        val foregroundTaskInfo = am.getRunningTasks(1)[0]
        val foregroundTaskPackageName = foregroundTaskInfo.topActivity.packageName
        return foregroundTaskPackageName.toLowerCase() == context.packageName.toLowerCase()
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
                if (!isAppInForeground(this)) {
                    val mBuilder = NotificationCompat.Builder(this, "new_message")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("New messages in room " + holder.room.num)
                            .setAutoCancel(true)
                    val mNotificationId = 1
                    val resultIntent = Intent(this, ChatActivity::class.java)
                    resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    val resultPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    mBuilder.setContentIntent(resultPendingIntent)
                    val mNotifyMgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    mNotifyMgr.notify(mNotificationId, mBuilder.build())
                }

            }
        }
    }

    override fun onConnect(site: String, success: Boolean) {
        siteStatuses[site] = WebsocketConnectionStatus.ESTABLISHED
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
        val soRequestBody = FormEncodingBuilder()
                .add("fkey", chatFkey)
                .add("immediate", "true")
                .add("quiet", "true")
                .build()
        val soChatPageRequest = Request.Builder()
                .url(Client.SITE_STACK_OVERFLOW + "/chats/join/" + room.num)
                .post(soRequestBody)
                .build()
        client.newCall(soChatPageRequest).execute()
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
                .url("$wsUrl?l=0")
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

