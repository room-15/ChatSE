package com.tristanwiley.chatse.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.util.Linkify
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.GridHolder
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.adapters.MessageAdapter
import com.tristanwiley.chatse.chat.adapters.UploadImageAdapter
import com.tristanwiley.chatse.chat.adapters.UsersAdapter
import com.tristanwiley.chatse.chat.service.IncomingEventListener
import com.tristanwiley.chatse.event.ChatEventGenerator
import com.tristanwiley.chatse.event.EventList
import com.tristanwiley.chatse.event.presenter.message.MessageEvent
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import com.tristanwiley.chatse.network.ClientManager.client
import com.tristanwiley.chatse.stars.StarsActivity
import kotlinx.android.synthetic.main.fragment_chat.view.*
import kotlinx.android.synthetic.main.picker_footer.view.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * ChatFragment is the fragment used to display the current ChatRoom
 *
 * @property input: The EditText where new messages are inputted
 * @property messageList: The RecyclerView where all messages are contained
 * @property userList: The RecyclerView where all users are contained, for the right NavigationDrawer
 * @property events: EventList of all events
 * @property chatFkey: The fkey for the room
 * @property room: The current ChatRoom object
 * @property client: The OkHttp client
 * @property messageAdapter: Adapter for displaying all messages
 * @property usersAdapter: Adapter for displaying all users
 * @property mapper: ObjectMapper to get the Messages Object
 * @property chatEventGenerator: Generator that generates ChatEvent objects
 * @property dialog: Used for DialogPlus displaying the image uploading dialog.
 */
class ChatFragment : Fragment(), IncomingEventListener, ChatMessageCallback {

    private lateinit var input: EditText
    private lateinit var messageList: RecyclerView
    private lateinit var userList: RecyclerView
    private lateinit var events: EventList
    private lateinit var loadMessagesLayout: SwipeRefreshLayout
    private lateinit var chatFkey: String
    lateinit var room: ChatRoom
    private lateinit var roomName: String
    private val client = ClientManager.client
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersAdapter: UsersAdapter
    private val mapper = ObjectMapper()
    private val chatEventGenerator = ChatEventGenerator()
    lateinit var dialog: DialogPlus
    private var currentLoadCount = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Signify this fragment has an options menu
        setHasOptionsMenu(true)

        //Get arguments passed in
        val args = arguments

        //Get the fkey from the arguments
        args?.getString(ChatFragment.EXTRA_FKEY)?.let {
            chatFkey = it
        }

        //Get the current ChatRoom from the arguments
        args?.getParcelable<ChatRoom>(ChatFragment.EXTRA_ROOM)?.let {
            room = it
        }

        //Get the roomName from the arguments
        args?.getString(ChatFragment.EXTRA_NAME)?.let {
            roomName = it
        }

        if (room.site == Client.SITE_STACK_OVERFLOW) {
            //Set the multitasking color to orange
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as ChatActivity).setTaskDescription(ActivityManager.TaskDescription((activity as AppCompatActivity).supportActionBar?.title.toString(), ActivityManager.TaskDescription().icon, ContextCompat.getColor(activity as ChatActivity, R.color.stackoverflow_orange)))
            }

        } else if (room.site == Client.SITE_STACK_EXCHANGE) {
            //Set the multitasking color to blue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as ChatActivity).setTaskDescription(ActivityManager.TaskDescription((activity as AppCompatActivity).supportActionBar?.title.toString(), ActivityManager.TaskDescription().icon, ContextCompat.getColor(activity as ChatActivity, R.color.stackexchange_blue)))
            }
        }
        //Set the EventList by the room number
        events = EventList(room.num)
        (activity as ChatActivity).createRoom(room.site, roomName, room.num.toLong(), 0, chatFkey)
    }

    //When the fragment view is created
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        //Create a variable for changing the theme
        var contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme_SO)

        //Depending on the room, change the theme and status bar color
        if (room.site == Client.SITE_STACK_OVERFLOW) {
            //Check version and set status bar color, theme already defaulted to SO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as ChatActivity).window.statusBarColor = ContextCompat.getColor(activity as ChatActivity, R.color.primary_dark)
            }
        } else if (room.site == Client.SITE_STACK_EXCHANGE) {
            //Set theme to SE and color to SE color
            contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme_SE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (activity as ChatActivity).window.statusBarColor = ContextCompat.getColor(activity as ChatActivity, R.color.se_primary_dark)
            }
        }

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        //Inflate the chat fragment view
        val view = localInflater.inflate(R.layout.fragment_chat, container, false)

        //Set a onClickListener for the submit button
        view.chat_input_submit.setOnClickListener {
            val content = input.text.toString()
            input.setText("")
            onSubmit(content)
        }

        //Set a long click listener for the image upload button and display a dialog using DialogPlus
        view.chat_input_upload.setOnClickListener {
            dialog = DialogPlus.newDialog(activity)
                    .setContentHolder(GridHolder(2))
                    .setGravity(Gravity.CENTER)
                    .setAdapter(UploadImageAdapter(activity as ChatActivity))
                    .setOnItemClickListener { _, _, _, position ->
                        when (position) {
                            0 -> {
                                //When you click on the camera button, open a camera intent and get the result in onActivityResult
                                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                                startActivityForResult(cameraIntent, 0)
                            }
                            1 -> {
                                //When you click on the gallery button, open a camera intent and get the result in onActivityResult
                                //Request external storage permission
                                if (ContextCompat.checkSelfPermission(activity as ChatActivity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        ActivityCompat.requestPermissions(activity as ChatActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                                    } else {
                                        openFileChooser()
                                    }
                                } else {
                                    openFileChooser()
                                }
                            }
                        }
                    }
                    //Set the footer for inputting a url
                    .setFooter(R.layout.picker_footer)
                    .setPadding(50, 50, 50, 50)
                    .create()

            //If uploading from URL, upload url to Imgur
            dialog.footerView.footer_confirm_button.setOnClickListener { _ ->
                uploadToImgur(dialog.footerView.footer_url_box.text.toString())
            }
            dialog.show()
        }

        //Set all variables from layout
        input = view.findViewById(R.id.chat_input_text)
        messageList = view.findViewById(R.id.chat_message_list)
        userList = (activity as ChatActivity).findViewById(R.id.room_users)
        loadMessagesLayout = view.findViewById(R.id.load_messages_layout)

        messageAdapter = MessageAdapter(context!!, chatFkey, room, messageCallback = this)
        usersAdapter = UsersAdapter(context!!, events)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        messageList.layoutManager = layoutManager
        messageList.adapter = messageAdapter
//        messageList.addItemDecoration(CoreDividerItemDecoration(activity, CoreDividerItemDecoration.VERTICAL_LIST))
        userList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        userList.adapter = usersAdapter
        userList.addItemDecoration(DividerItemDecoration(activity as ChatActivity))
        usersAdapter.notifyDataSetChanged()
        //When you reach the top and swipe to load more, add 25 to the current loaded amount and load more
        loadMessagesLayout.setOnRefreshListener {
            currentLoadCount += 25
            doAsync {
                val messages = getMessagesObject(client, room, currentLoadCount)
                handleNewEvents(messages.get("events"))
            }
        }

        messageAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                val totalItemCount = messageAdapter.itemCount -1
                val lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()

                if(lastVisiblePosition == totalItemCount){
                    // We have reached the end of the recycler view.
                    // no need to scroll
                }
                else if (positionStart == 0) {
                    layoutManager.scrollToPosition(0)
                }
            }

        })

        //Get handle new events
        doAsync {
            val messages = getMessagesObject(client, room, 50)
            handleNewEvents(messages.get("events"))
        }
        //Get focus of EditText input
        input.requestFocus()
        return view
    }

    /**
     * onOptionsItemSelected - For each item in Toolbar
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.room_information -> {
                doAsync {
                    val client = ClientManager.client
                    val request = Request.Builder()
                            .url("https://chat.stackoverflow.com/rooms/thumbs/${room.num}")
                            .build()
                    val response = client.newCall(request).execute()
                    val jsonResult = JSONObject(response.body().string())

                    uiThread {
                        //Create dialog
                        val builder = AlertDialog.Builder(context as Context)

                        //Set the dialog title to the room name
                        builder.setTitle(jsonResult.getString("name"))

                        //Create a LinearLayout and set it's orientation to vertical
                        val l = LinearLayout(context)
                        l.orientation = LinearLayout.VERTICAL

                        //Get the display density as a variable for use with padding
                        val dpi = (activity as ChatActivity).resources.displayMetrics.density.toInt()

                        //Set the padding of the layout so it looks natural
                        l.setPadding((19 * dpi), (5 * dpi), (14 * dpi), (5 * dpi))

                        //Create a TextView that houses the description of the room
                        val roomText = TextView(context)

                        //Set the text size so it looks nice
                        roomText.textSize = 18F

                        //Make it so when clicking links, it opens in browser like expected
                        roomText.autoLinkMask = Linkify.WEB_URLS

                        //Depending on SDK version, set the text from HTML different ways.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            roomText.text = Html.fromHtml(jsonResult.getString("description"), Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            @Suppress("DEPRECATION")
                            roomText.text = Html.fromHtml(jsonResult.getString("description"))
                        }
                        //Set the TextView to match parent
                        roomText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

                        //Add the TextView to the Layoutt
                        l.addView(roomText)

                        //Create a FlowLayout (instead of LinearLayout so we can display multiple tags)
                        val tagsLayout = FlowLayout(context as Context)

                        //For each tag create a TextView
                        val tags = Jsoup.parse(jsonResult.getString("tags")).getElementsByClass("tag")
                        tags.forEach {
                            //Create a TextView
                            val tagview = TextView(context)
                            //Set the text from parsing the HTML
                            tagview.text = it.text()
                            //Set a variable to the tag url
                            val url = it.attr("href")

                            //On tag click, open the tag in the browser
                            tagview.setOnClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }

                            //Set the text color to white
                            tagview.setTextColor(Color.WHITE)

                            //Set the TextView padding
                            tagview.setPadding(14, 14, 14, 14)

                            //Make sure the tag stays on one line
                            tagview.setSingleLine(true)

                            //Set the layout param
                            val layoutparam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                            //Set some margins and set the LayoutParams to the TextView
                            layoutparam.setMargins(24, 0, 0, 0)
                            tagview.layoutParams = layoutparam

                            //Set the background to a rectangle to look like it does on the web
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                tagview.background = ContextCompat.getDrawable(context as Context, R.drawable.tag_background)
                            } else {
                                @Suppress("DEPRECATION")
                                tagview.setBackgroundDrawable(ContextCompat.getDrawable(context as Context, R.drawable.tag_background))
                            }

                            //Add the tag to the layout
                            tagsLayout.addView(tagview)
                        }

                        //Add all the tags to the main layout
                        l.addView(tagsLayout)

                        //Set the LinearLayout as the AlertDialog view
                        builder.setView(l)

                        //Show that AlertDialog
                        builder.show()
                    }
                }
                return true
            }
            R.id.room_stars -> {
                //Load intent for showing stars
                val intent = Intent(activity, StarsActivity::class.java)
                intent.putExtra("room", room)
                intent.putExtra("roomName", roomName)
                startActivity(intent)
            }
        }
        return false
    }

    /**
     * On permissions granted open the file chooser
     * If the permissions were denied, tell the user they can't choose a picture without accepting
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileChooser()
                } else {
                    Toast.makeText(activity, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    /**
     * When we receive a result from the camera via Intent, or from the Gallery via intent
     * do something
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //If the data isn't null
        if (data != null) {
            when (requestCode) {
                //If from the camera
                0 -> {
                    if (data.extras != null) {
                        //Get the photo from the data as a Bitmap
                        val photo = data.extras.get("data") as Bitmap

                        //Convert to a ByteStream
                        val byteArrayOutput = ByteArrayOutputStream()

                        //Compress so Imgur likes it
                        photo.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutput)

                        //Get the bytes
                        val photoBytes = Base64.encodeToString(byteArrayOutput.toByteArray(), Base64.DEFAULT)

                        //Upload bytes to Imgur
                        uploadToImgur(photoBytes)
                    }
                }
                //If from the gallery
                1 -> {
                    if (data.data != null) {
                        val photoBytes = getImageBytes(data.data)
                        uploadToImgur(Base64.encodeToString(photoBytes, Base64.DEFAULT))
                    }
                }
            }
        }
    }

    /**
     * Important function that handles all new events
     * Maps the events to a ChatEvent and adds it to the events list
     * Also updates adapters to signify there was a new event
     */
    override fun handleNewEvents(messagesJson: JsonNode) {
        if (this.activity == null) {
            return
        }
        messagesJson
                .mapNotNull { chatEventGenerator.createEvent(it) }
                .filter { it.roomId == room.num }
                .forEach {
                    events.addEvent(it, (this.activity as ChatActivity), room)
                }

        // fetch url for each message based on user Id
        (activity as ChatActivity).runOnUiThread {
            val messageList = events.messagePresenter.getEventsList()
            for (messageEvent in messageList) {
                //val messageEvent = messageList[i]
                val userId = messageEvent.userId
                val index = messageList.indexOf(messageEvent)
                if (!messageEvent.isFetchedUrl) {
                    doAsync {
                        val client = ClientManager.client
                        val request = Request.Builder()
                                .url("${room.site}/users/thumbs/$userId")
                                .build()
                        val response = client.newCall(request).execute()
                        val jsonResult = JSONObject(response.body().string())

                        //Get the emailHash attribute which contains either a link to Imgur or a hash for Gravatar
                        val hash = jsonResult.getString("email_hash").replace("!", "")
                        var imageLink = hash
                        //If Gravatar, create link
                        if (!hash.contains(".")) {
                            imageLink = "https://www.gravatar.com/avatar/$hash"
                        } else if (!hash.contains("http")) {
                            imageLink = room.site + hash
                        }
                        (activity as ChatActivity).runOnUiThread {
                            Timber.i("Index :$index and url :$imageLink and context ${messageEvent.content}")
                            messageEvent.emailHash = imageLink
                            messageEvent.isFetchedUrl = true
                            messageAdapter.add(messageEvent)
                            loadMessagesLayout.isRefreshing = false
                        }
                    }
                }
            }
            usersAdapter.update()
            usersAdapter.notifyDataSetChanged()

        }
    }

    //Open the file chooser so we can get an image to upload to imgur
    private fun openFileChooser() {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"

        val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = "image/*"

        val chooserIntent = Intent.createChooser(getIntent, "Select Image")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

        startActivityForResult(chooserIntent, 1)
    }

    //On submit, post a new message
    private fun onSubmit(content: String) {

        doAsync {
            try {
                newMessage(client, room, chatFkey, content)
            } catch (e: IOException) {
                Log.e("handleNewEvents", e.message)
            }
        }
    }

    /**
     * Upload to Imgur some content
     * @param content: A string that contains either bytes or a URL
     */
    private fun uploadToImgur(content: String) {
        doAsync {
            val client = ClientManager.client

            //Create RequestBody with image bytes
            val imgurRequestBody = FormEncodingBuilder()
                    .add("image", content)
                    .build()

            //Build request to post bytes and upload image
            val newMessageRequest = Request.Builder()
                    .url("https://api.imgur.com/3/image")
                    .addHeader("authorization", "Client-ID c4b0ceea1a1b029")
                    .post(imgurRequestBody)
                    .build()

            //Upload image and get result
            val response = client.newCall(newMessageRequest).execute()
            val result = JSONObject(response.body().string())

            //Check to see if the result is good and data was uploaded
            if (result.has("data") && result.getJSONObject("data").has("link")) {
                //Submit the url to the chat
                onSubmit(result.getJSONObject("data").getString("link"))
                //Dismiss the DialogPlus dialog
                dialog.dismiss()
            } else {
                Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getImageBytes(uri: Uri): ByteArray? {
        var inStream: InputStream? = null
        var bitmap: Bitmap? = null

        return try {
            inStream = (activity as ChatActivity).contentResolver.openInputStream(uri)
            bitmap = BitmapFactory.decodeStream(inStream)
            val outStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.toByteArray()
        } catch (e: FileNotFoundException) {
            null
        } finally {
            inStream?.close()
            bitmap?.recycle()
        }
    }

    /**
     * Map a message to an object and return a JsonNode
     */
    @Throws(IOException::class)
    private fun getMessagesObject(client: Client, room: ChatRoom, count: Int): JsonNode {
        val getMessagesRequestBody = FormEncodingBuilder()
                .add("since", 0.toString())
                .add("mode", "Messages")
                .add("msgCount", count.toString())
                .add("fkey", chatFkey)
                .build()
        val getMessagesRequest = Request.Builder()
                .url(room.site + "/chats/" + room.num + "/events")
                .post(getMessagesRequestBody)
                .build()

        val getMessagesResponse = client.newCall(getMessagesRequest).execute()
        return mapper.readTree(getMessagesResponse.body().byteStream())
    }

    /**
     * Create a new message and post it to the chat, creating it!
     */
    @Throws(IOException::class)
    private fun newMessage(client: Client, room: ChatRoom,
                           fkey: String?, message: String) {
        val newMessageRequestBody = FormEncodingBuilder()
                .add("text", message)
                .add("fkey", fkey)
                .build()
        val newMessageRequest = Request.Builder()
                .url(room.site + "/chats/" + room.num + "/messages/new/")
                .post(newMessageRequestBody)
                .build()

        client.newCall(newMessageRequest).execute()
    }

    @SuppressLint("all")
    override fun onReplyMessage(id: Int) {
        input.setText(":$id ")
        input.setSelection(input.length())
    }

    companion object {

        private val EXTRA_ROOM = "room"
        private val EXTRA_NAME = "name"
        private val EXTRA_FKEY = "fkey"

        fun createInstance(room: ChatRoom, name: String, fkey: String): ChatFragment {
            val b = Bundle(3)
            b.putParcelable(ChatFragment.Companion.EXTRA_ROOM, room)
            b.putString(ChatFragment.Companion.EXTRA_NAME, name)
            b.putString(ChatFragment.Companion.EXTRA_FKEY, fkey)

            val fragment = ChatFragment()
            fragment.arguments = b
            return fragment
        }
    }
}
