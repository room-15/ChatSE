package me.shreyasr.chatse.chat

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.service.IncomingEventListener
import me.shreyasr.chatse.event.ChatEventGenerator
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import timber.log.Timber
import java.io.IOException

class ChatFragment : Fragment(), IncomingEventListener {
    private lateinit var input: EditText
    private lateinit var messageList: RecyclerView
    private lateinit var events: EventList
    private var chatFkey: String? = null
    private var room: ChatRoom? = null
    private val client = ClientManager.client
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    private var messageAdapter: MessageAdapter? = null
    private val mapper = ObjectMapper()
    private val chatEventGenerator = ChatEventGenerator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        chatFkey = args.getString(EXTRA_FKEY)
        room = args.getParcelable<ChatRoom>(EXTRA_ROOM)

        assert(chatFkey != null)
        assert(room != null)

        events = EventList(room?.num ?: 0)

        val handlerThread = HandlerThread("NetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.fragment_chat, container, false)

        val chat_submit = view?.findViewById(R.id.chat_input_submit) as ImageButton
        chat_submit.setOnClickListener { onSubmit() }

        input = view.findViewById(R.id.chat_input_text) as EditText
        messageList = view.findViewById(R.id.chat_message_list) as RecyclerView

        messageAdapter = MessageAdapter(events)
        messageList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        messageList.adapter = messageAdapter
        messageList.addItemDecoration(CoreDividerItemDecoration(activity, CoreDividerItemDecoration.VERTICAL_LIST))

        input.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSubmit()
                return@OnEditorActionListener true
            }
            false
        })

        networkHandler?.post {
            try {
                val messages = getMessagesObject(client, room, 50)
                handleNewEvents(messages.get("events"))
            } catch (e: IOException) {
                Timber.e(e)
            }
        }

        input.requestFocus()
        return view
    }


    override fun handleNewEvents(messagesJson: JsonNode) {
        if (room == null) return
        messagesJson
                .mapNotNull { chatEventGenerator.createEvent(it) }
                .filter { it.room_id == room?.num }
                .forEach { events.addEvent(it) }

        uiThreadHandler.post { messageAdapter?.update() }
    }

    private fun onSubmit() {
        val content = input.text.toString()
        input.setText("")
        networkHandler?.post {
            try {
                newMessage(client, room, chatFkey, content)
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    @Throws(IOException::class)
    private fun getMessagesObject(client: Client, room: ChatRoom?, count: Int): JsonNode {
        val getMessagesRequestBody = FormEncodingBuilder()
                .add("since", 0.toString())
                .add("mode", "Messages")
                .add("msgCount", count.toString())
                .add("fkey", chatFkey)
                .build()
        val getMessagesRequest = Request.Builder()
                .url(room?.site + "/chats/" + room?.num + "/events")
                .post(getMessagesRequestBody)
                .build()

        val getMessagesResponse = client.newCall(getMessagesRequest).execute()
        return mapper.readTree(getMessagesResponse.body().byteStream())
    }

    @Throws(IOException::class)
    private fun newMessage(client: Client, room: ChatRoom?,
                           fkey: String?, message: String) {
        val newMessageRequestBody = FormEncodingBuilder()
                .add("text", message)
                .add("fkey", fkey)
                .build()
        val newMessageRequest = Request.Builder()
                .url(room?.site + "/chats/" + room?.num + "/messages/new/")
                .post(newMessageRequestBody)
                .build()

        client.newCall(newMessageRequest).execute()
        Timber.i("New message")
    }

    val pageTitle: String
        get() = arguments.getString(EXTRA_NAME)

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


