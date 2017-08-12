package me.shreyasr.chatse.chat

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.koushikdutta.ion.Ion
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.GridHolder
import kotlinx.android.synthetic.main.fragment_chat.view.*
import kotlinx.android.synthetic.main.picker_footer.view.*
import me.shreyasr.chatse.App
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.adapters.MessageAdapter
import me.shreyasr.chatse.chat.adapters.UploadImageAdapter
import me.shreyasr.chatse.chat.adapters.UsersAdapter
import me.shreyasr.chatse.chat.service.IncomingEventListener
import me.shreyasr.chatse.event.ChatEventGenerator
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.network.ClientManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/**
 * ChatFragment is the fragment used to display the current ChatRoom
 *
 * @param input: The EditText where new messages are inputted
 * @param messageList: The RecyclerView where all messages are contained
 * @param userList: The RecyclerView where all users are contained, for the right NavigationDrawer
 * @param events: EventList of all events
 * @param chatFkey: The fkey for the room
 * @param room: The current ChatRoom object
 * @param client: The OkHttp client
 * @param networkHandler: Handler that is used for the network requests
 * @param uiThreadHandler: Handler that is used for the uiThread
 * @param messageAdapter: Adapter for displaying all messages
 * @param usersAdapter: Adapter for displaying all users
 * @param mapper: ObjectMapper to get the Messages Object
 * @param chatEventGenerator: Generator that generates ChatEvent objects
 * @param dialog: Used for DialogPlus displaying the image uploading dialog.
 */
class ChatFragment : Fragment(), IncomingEventListener {

    private lateinit var input: EditText
    private lateinit var messageList: RecyclerView
    private lateinit var userList: RecyclerView
    private lateinit var events: EventList
    private lateinit var loadMessagesLayout: SwipeRefreshLayout
    lateinit var chatFkey: String
    lateinit var room: ChatRoom
    private val client = ClientManager.client
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var usersAdapter: UsersAdapter
    private val mapper = ObjectMapper()
    private val chatEventGenerator = ChatEventGenerator()
    lateinit var dialog: DialogPlus
    var currentLoadCount = 50

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Signify this fragment has an options menu
        setHasOptionsMenu(true)

        //Get arguments passed in
        val args = arguments

        //Get the fkey from the arguments
        chatFkey = args.getString(EXTRA_FKEY)

        //Get the current ChatRoom from the arguments
        room = args.getParcelable<ChatRoom>(EXTRA_ROOM)

        if (room.site == App.SITE_STACK_OVERFLOW) {
            //Set the multitasking color to orange
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.setTaskDescription(ActivityManager.TaskDescription((activity as AppCompatActivity).supportActionBar?.title.toString(), ActivityManager.TaskDescription().icon, ContextCompat.getColor(activity, R.color.stackoverflow_orange)))
            }

        } else if (room.site == App.SITE_STACK_EXCHANGE) {
            //Set the multitasking color to blue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.setTaskDescription(ActivityManager.TaskDescription((activity as AppCompatActivity).supportActionBar?.title.toString(), ActivityManager.TaskDescription().icon, ContextCompat.getColor(activity, R.color.stackexchange_blue)))
            }
        }
        //Set the EventList by the room number
        events = EventList(room.num)
    }

    //When the fragment view is created
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        //Create a variable for changing the theme
        var contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme_SO)

        //Depending on the room, change the theme and status bar color
        if (room.site == App.SITE_STACK_OVERFLOW) {
            //Check version and set status bar color, theme already defaulted to SO
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.primary_dark)
            }
        } else if (room.site == App.SITE_STACK_EXCHANGE) {
            //Set theme to SE and color to SE color
            contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme_SE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.se_primary_dark)
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
                    .setAdapter(UploadImageAdapter(activity))
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
                                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
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
            dialog.footerView.footer_confirm_button.setOnClickListener {
                uploadToImgur(dialog.footerView.footer_url_box.text.toString())
            }
            dialog.show()
        }

        //Set all variables from layout
        input = view.findViewById(R.id.chat_input_text) as EditText
        messageList = view.findViewById(R.id.chat_message_list) as RecyclerView
        userList = activity.findViewById(R.id.room_users) as RecyclerView
        loadMessagesLayout = view.findViewById(R.id.load_messages_layout) as SwipeRefreshLayout

        messageAdapter = MessageAdapter(activity, events, chatFkey, room)
        usersAdapter = UsersAdapter(activity, events)
        messageList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, true)
        messageList.adapter = messageAdapter
        messageList.addItemDecoration(CoreDividerItemDecoration(activity, CoreDividerItemDecoration.VERTICAL_LIST))
        userList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        userList.adapter = usersAdapter
        userList.addItemDecoration(CoreDividerItemDecoration(activity, CoreDividerItemDecoration.VERTICAL_LIST))

        //When you reach the top and swipe to load more, add 25 to the current loaded amount and load more
        loadMessagesLayout.setOnRefreshListener {
            currentLoadCount += 25
            doAsync {
                val messages = getMessagesObject(client, room, currentLoadCount)
                handleNewEvents(messages.get("events"))
            }
        }

        //On input send. Create message and set text to nothing ("")
        input.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val content = input.text.toString()
                input.setText("")
                onSubmit(content)
                return@OnEditorActionListener true
            }
            false
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
     * @param room_information: When clicking on this, the dialog opens displaying room information
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.room_information -> {
                Ion.with(activity)
                        .load("https://chat.stackoverflow.com/rooms/thumbs/${room.num}")
                        .asJsonObject()
                        .setCallback { e, result ->
                            if (e != null) {
                                Log.e("ChatFragment", e.message.toString())
                            } else {
                                //Create dialog
                                val builder = AlertDialog.Builder(context)

                                //Set the dialog title to the room name
                                builder.setTitle(result.get("name").asString)

                                //Create a LinearLayout and set it's orientation to vertical
                                val l = LinearLayout(context)
                                l.orientation = LinearLayout.VERTICAL

                                //Get the display density as a variable for use with padding
                                val dpi = activity.resources.displayMetrics.density.toInt()

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
                                    roomText.text = Html.fromHtml(result.get("description").asString, Html.FROM_HTML_MODE_COMPACT)
                                } else {
                                    @Suppress("DEPRECATION")
                                    roomText.text = Html.fromHtml(result.get("description").asString)
                                }
                                //Set the TextView to match parent
                                roomText.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

                                //Add the TextView to the Layoutt
                                l.addView(roomText)

                                //Create a FlowLayout (instead of LinearLayout so we can display multiple tags)
                                val tagsLayout = FlowLayout(context)

                                //For each tag create a TextView
                                val tags = Jsoup.parse(result.get("tags").asString).getElementsByClass("tag")
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
                                        tagview.background = ContextCompat.getDrawable(context, R.drawable.tag_background)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        tagview.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.tag_background))
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
                    val cursor: Cursor
                    if (data.data != null) {
                        //Get the photo
                        val selectedImage = data.data
                        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                        cursor = activity.contentResolver.query(selectedImage, filePathColumn, null, null, null)
                        cursor.use {
                            it.moveToFirst()
                            val picturePath = it.getString(it.getColumnIndex(filePathColumn[0]))
                            it.close()
                            //Get the path and get it as a File and upload it
                            uploadFileToImgur(File(picturePath))
                        }
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
                .filter { it.room_id == room.num }
                .forEach {
                    events.addEvent(it, this.activity, room)
                }


        //Update adapters so we know to check for new events
        activity.runOnUiThread {
            messageAdapter.update()
            usersAdapter.update()
            (activity as ChatActivity).addRoomsToDrawer(chatFkey)
            loadMessagesLayout.isRefreshing = false
        }
    }

    //Open the file chooser so we can get an image to upload to imgur
    fun openFileChooser() {
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
    fun uploadToImgur(content: String) {
        //Make network request with Ion for simplicity
        Ion.with(activity)
                .load("POST", "https://api.imgur.com/3/image")
                .addHeader("authorization", "Client-ID c4b0ceea1a1b029")
                .setBodyParameter("image", content)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                        Log.w("OnImgurUpload", e.message.toString())
                    }
                    //Check to see if the result is good and data was uploaded
                    if (result.has("data") && result.get("data").asJsonObject.has("link")) {
                        //Submit the url to the chat
                        onSubmit(result.get("data").asJsonObject.get("link").asString)
                        //Dismiss the DialogPlus dialog
                        dialog.dismiss()
                    } else {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                    }
                }
    }

    /**
     * Upload to Imgur some content
     * @param content: A string that contains a File
     */
    fun uploadFileToImgur(photo: File) {
        Ion.with(activity)
                .load("POST", "https://api.imgur.com/3/image")
                .addHeader("authorization", "Client-ID c4b0ceea1a1b029")
                .setMultipartFile("image", photo)
                .asJsonObject()
                .setCallback { e, result ->
                    if (e != null) {
                        Toast.makeText(activity, "Failed to Upload Image", Toast.LENGTH_SHORT).show()
                        Log.w("OnFileUploadImgur", e.message.toString())
                    } else {
                        //Get the result from the data and submit it to the chat
                        onSubmit(result.get("data").asJsonObject.get("link").asString)
                        dialog.dismiss()
                    }
                }
    }

    /**
     * Map a message to an object and return a JsonNode
     */
    @Throws(IOException::class)
    private fun getMessagesObject(client: OkHttpClient, room: ChatRoom, count: Int): JsonNode {
        val getMessagesRequestBody = FormBody.Builder()
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
        return mapper.readTree(getMessagesResponse.body()?.byteStream())
    }

    /**
     * Create a new message and post it to the chat, creating it!
     */
    @Throws(IOException::class)
    private fun newMessage(client: OkHttpClient, room: ChatRoom,
                           fkey: String?, message: String) {
        val newMessageRequestBody = FormBody.Builder()
                .add("text", message)
                .add("fkey", fkey)
                .build()
        val newMessageRequest = Request.Builder()
                .url(room.site + "/chats/" + room.num + "/messages/new/")
                .post(newMessageRequestBody)
                .build()

        client.newCall(newMessageRequest).execute()
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
