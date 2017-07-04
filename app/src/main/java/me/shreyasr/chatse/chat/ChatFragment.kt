package me.shreyasr.chatse.chat

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response

import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper

import java.io.IOException

import me.shreyasr.chatse.App
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.service.IncomingEventListener
import me.shreyasr.chatse.event.ChatEvent
import me.shreyasr.chatse.event.ChatEventGenerator
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import me.shreyasr.chatse.util.Logger

class ChatFragment : Fragment(), IncomingEventListener {
    internal lateinit var input: EditText
    internal lateinit var messageList: RecyclerView
    private lateinit var roomName: String
    private var chatFkey: String? = null
    private var room: ChatRoom? = null
    private val client = ClientManager.client
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var messageAdapter: MessageAdapter? = null
    private val mapper = ObjectMapper()
    private val chatEventGenerator = ChatEventGenerator()
    private var prefs: SharedPreferences? = null
    private var events: EventList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        chatFkey = args.getString(EXTRA_FKEY)
        room = args.getParcelable<ChatRoom>(EXTRA_ROOM)

        assert(chatFkey != null)
        assert(room != null)

        events = EventList(room!!.num)

        prefs = App.getPrefs(activity)

        val handlerThread = HandlerThread("NetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_chat, container, false)

        //TODO add cat pictures instant add

        val chat_submit = view.findViewById(R.id.chat_input_submit) as ImageButton
        chat_submit.setOnClickListener { onSubmit() }

        input = view.findViewById(R.id.chat_input_text) as EditText
        messageList = view.findViewById(R.id.chat_message_list) as RecyclerView

        messageAdapter = MessageAdapter(events!!, activity.resources, activity)
        messageList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        messageList.adapter = messageAdapter
        messageList.addItemDecoration(DividerItemDecoration(activity, R.drawable.message_divider))


        input.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSubmit()
                return@OnEditorActionListener true
            }
            false
        })

        networkHandler!!.post {
            try {
                val messages = getMessagesObject(client, room!!, 50)
                handleNewEvents(messages.get("events"))
            } catch (e: IOException) {
                Log.e(e::class.simpleName, e.message, e)
            }
        }

        input.requestFocus()
        return view
    }

    override fun handleNewEvents(jsonEvents: JsonNode) {
        if (room == null || events == null) return
        for (jsonEvent in jsonEvents) {
            val newEvent = chatEventGenerator.createEvent(jsonEvent)
            if (newEvent!!.room_id == room!!.num) {
                events!!.addEvent(newEvent)
            }
        }
        uiThreadHandler.post { messageAdapter!!.update() }
    }

    internal fun onSubmit() {
        val content = input.text.toString()
        input.setText("")
        networkHandler!!.post {
            try {
                newMessage(client, room!!, chatFkey!!, content)
            } catch (e: IOException) {
                Log.e(e::class.simpleName, e.message, e)
            }
        }
    }

    @Throws(IOException::class)
    private fun getMessagesObject(client: Client, room: ChatRoom, count: Int): JsonNode {
        val getMessagesRequestBody = FormEncodingBuilder()
                .add("since", 0.toString())
                .add("mode", "Messages")
                .add("msgCount", count.toString())
                .add("fkey", chatFkey!!)
                .build()
        val getMessagesRequest = Request.Builder()
                .url(room.site + "/chats/" + room.num + "/events")
                .post(getMessagesRequestBody)
                .build()
        val getMessagesResponse = client.newCall(getMessagesRequest).execute()
        return mapper.readTree(getMessagesResponse.body().byteStream())
    }

    @Throws(IOException::class)
    private fun newMessage(client: Client, room: ChatRoom,
                           fkey: String, message: String) {
        val newMessageRequestBody = FormEncodingBuilder()
                .add("text", message)
                .add("fkey", fkey)
                .build()
        val newMessageRequest = Request.Builder()
                .url(room.site + "/chats/" + room.num + "/messages/new/")
                .post(newMessageRequestBody)
                .build()
        val newMessageResponse = client.newCall(newMessageRequest).execute()
        Logger.event(this.javaClass, "New message")
    }

    val pageTitle: String
        get() {
            roomName = arguments.getString(EXTRA_NAME)
            return roomName
        }

    companion object {

        private val EXTRA_ROOM = "room"
        private val EXTRA_NAME = "name"
        private val EXTRA_FKEY = "fkey"

        fun createInstance(room: ChatRoom, name: String, fkey: String): ChatFragment {
            val b = Bundle(3)
            b.putParcelable(EXTRA_ROOM, room)
            b.putString(EXTRA_NAME, name)
            b.putString(EXTRA_FKEY, fkey)

            val fragment = ChatFragment()
            fragment.arguments = b
            return fragment
        }
    }
}
