package me.shreyasr.chatse.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.os.*
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.koushikdutta.ion.Ion
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.GridHolder
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import kotlinx.android.synthetic.main.picker_footer.view.*
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.adapters.MessageAdapter
import me.shreyasr.chatse.chat.adapters.UploadImageAdapter
import me.shreyasr.chatse.chat.service.IncomingEventListener
import me.shreyasr.chatse.event.ChatEventGenerator
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
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
    lateinit var dialog: DialogPlus

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
        chat_submit.setOnClickListener {
            val content = input.text.toString()
            input.setText("")
            onSubmit(content)
        }
        chat_submit.setOnLongClickListener {
            dialog = DialogPlus.newDialog(activity)
                    .setContentHolder(GridHolder(2))
                    .setGravity(Gravity.CENTER)
                    .setAdapter(UploadImageAdapter(activity))
                    .setOnItemClickListener { _, _, _, position ->
                        when (position) {
                            0 -> {
                                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                                startActivityForResult(cameraIntent, 0)
                            }
                            1 -> {
                                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        Log.wtf("PERMISSION", "REQUESTOPEN")
                                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                                    } else {
                                        Log.wtf("SDK Version", "SHOULD OPEN")
                                        openFileChooser()
                                    }
                                } else {
                                    Log.wtf("HAS PERMISSION", "SHOULD OPEN")
                                    openFileChooser()
                                }
                            }
                        }
                    }
                    .setFooter(R.layout.picker_footer)
                    .setPadding(50, 50, 50, 50)
                    .create()

            dialog.footerView.footer_confirm_button.setOnClickListener {
                uploadToImgur(dialog.footerView.footer_url_box.text.toString())
            }
            dialog.show()
            true
        }

        input = view.findViewById(R.id.chat_input_text) as EditText
        messageList = view.findViewById(R.id.chat_message_list) as RecyclerView

        messageAdapter = MessageAdapter(activity, events, chatFkey)
        messageList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        messageList.adapter = messageAdapter
        messageList.addItemDecoration(CoreDividerItemDecoration(activity, CoreDividerItemDecoration.VERTICAL_LIST))

        input.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val content = input.text.toString()
                input.setText("")
                onSubmit(content)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.wtf("GOT PERMISSION", "SHOULD OPEN")
                    openFileChooser()
                } else {
                    Toast.makeText(activity, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            when (requestCode) {
                0 -> {
                    if (data.extras != null) {
                        val photo = data.extras.get("data") as Bitmap
                        val byteArrayOutput = ByteArrayOutputStream()
                        photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutput)
                        val photoBytes = Base64.encodeToString(byteArrayOutput.toByteArray(), Base64.DEFAULT)
                        uploadToImgur(photoBytes)
                    }
                }
                1 -> {
                    val cursor: Cursor
                    if (data.data != null) {
                        val selectedImage = data.data
                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                        cursor = activity.contentResolver.query(selectedImage, filePathColumn, null, null, null)
                        cursor.use {
                            it.moveToFirst()
                            val picturePath = it.getString(it.getColumnIndex(filePathColumn[0]))
                            it.close()
                            uploadFileToImgur(File(picturePath))
                        }
                    }
                }
            }
        }
    }

    override fun handleNewEvents(messagesJson: JsonNode) {
        if (room == null) return
        messagesJson
                .mapNotNull { chatEventGenerator.createEvent(it) }
                .filter { it.room_id == room?.num }
                .forEach { events.addEvent(it) }

        uiThreadHandler.post { messageAdapter?.update() }
    }

    fun openFileChooser() {
        Log.wtf("OPENFILECHOOSER", "OPENING")
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"

        val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, "Select Image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

        startActivityForResult(chooserIntent, 1)
    }

    private fun onSubmit(content: String) {

        networkHandler?.post {
            try {
                newMessage(client, room, chatFkey, content)
            } catch (e: IOException) {
                Timber.e(e)
            }
        }
    }

    fun uploadToImgur(photoBytes: String) {
        Ion.with(activity)
                .load("POST", "https://api.imgur.com/3/image")
                .addHeader("authorization", "Client-ID c4b0ceea1a1b029")
                .setBodyParameter("image", photoBytes)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                        Log.w("OnImgurUpload", e.message)
                    }
                    if (result.has("data") && result.get("data").asJsonObject.has("link")) {
                        onSubmit(result.get("data").asJsonObject.get("link").asString)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                    }
                }
    }


    fun uploadFileToImgur(photo: File) {
        Ion.with(activity)
                .load("POST", "https://api.imgur.com/3/image")
                .addHeader("authorization", "Client-ID c4b0ceea1a1b029")
                .setMultipartFile("image", photo)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                        Log.w("OnFileUploadImgur", e.message)
                    } else {
                        onSubmit(result.get("data").asJsonObject.get("link").asString)
                        dialog.dismiss()
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

