package com.tristanwiley.chatse.chat

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.App
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.about.AboutActivity
import com.tristanwiley.chatse.chat.adapters.RoomAdapter
import com.tristanwiley.chatse.chat.service.IncomingEventService
import com.tristanwiley.chatse.chat.service.IncomingEventServiceBinder
import com.tristanwiley.chatse.login.LoginActivity
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import com.tristanwiley.chatse.network.cookie.PersistentCookieStore
import com.tristanwiley.chatse.util.SharedPreferenceManager
import com.tristanwiley.chatse.util.UserPreferenceKeys
import com.tristanwiley.chatse.views.DividerItemDecoration
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.room_nav_header.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * ChatActivity is the main activity that sets the Fragment and Drawer layouts
 *
 * @property serviceBinder: IncomingEventServiceBinder that is used to load rooms
 * @property soRoomList: A list of all the rooms the user is currently in for StackOverflow
 * @property seRoomList: A list of all the rooms the user is currently in for StackExchange
 * @property fkey: The glorious fkey used to authenticate requests
 * @property soRoomAdapter: Adapter for StackOverflow Rooms the user is in
 * @property seRoomAdapter: Adapter for StackExchange Rooms the user is in
 */
class ChatActivity : AppCompatActivity(), ServiceConnection, RoomAdapter.OnItemClickListener {

    private lateinit var serviceBinder: IncomingEventServiceBinder
    private val soRoomList = arrayListOf<Room>()
    private val seRoomList = arrayListOf<Room>()
    private lateinit var fkey: String
    private lateinit var soRoomAdapter: RoomAdapter
    private lateinit var seRoomAdapter: RoomAdapter
    private var isBound = false
    private val prefs = SharedPreferenceManager.sharedPreferences

    private var room_num: Int? = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_chat)

/*        //If the user is still logged in then continue, otherwise logout
        if (prefs.getBoolean(App.PREF_HAS_CREDS, false)) {
            //If the user came from a deep link, continue
            if (intent.action == Intent.ACTION_VIEW) {
                //Check if it's SO or SE
                val isSO = intent.data.toString().contains(Client.SITE_STACK_OVERFLOW)
                //Get room number
                val roomNum = intent.data.path.split("/")[2].toInt()

                //Figure out the site based on isSO
                var site = Client.SITE_STACK_OVERFLOW
                if (!isSO && roomNum != 0) {
                    site = Client.SITE_STACK_EXCHANGE
                }

                //Load the site from the deep link
                loadChatFragment(ChatRoom(site, roomNum))
            }
        } else {
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        } */

        //Create adapters for current user's rooms
        soRoomAdapter = RoomAdapter(Client.SITE_STACK_OVERFLOW, soRoomList, this)
        seRoomAdapter = RoomAdapter(Client.SITE_STACK_EXCHANGE, seRoomList, this)

        //Set adapters to RecyclerViews along with LayoutManagers
        stackoverflow_room_list.addItemDecoration(DividerItemDecoration(this))
        stackoverflow_room_list.adapter = soRoomAdapter
        stackoverflow_room_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        stackexchange_room_list.addItemDecoration(DividerItemDecoration(this))
        stackexchange_room_list.adapter = seRoomAdapter
        stackexchange_room_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)

//        //Notify data is changed
//        soRoomAdapter.notifyDataSetChanged()
//        seRoomAdapter.notifyDataSetChanged()

        //Set toolbar as SupportActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Color the toolbar for StackOverflow as a default
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.stackoverflow_orange)))

        //On the toggle button pressed open the NavigationDrawer
        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        //Load the user data for the NavigationDrawer header
        loadUserData()

        val serviceIntent = Intent(this, IncomingEventService::class.java)
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    /**
     * Function to load user data and set it to the NavigationDrawer header
     */
    private fun loadUserData() {
        val userID = prefs.getInt("SOID", -1)
        val seID = prefs.getInt("SEID", -1)

        //Does the user have a SO account? What about SE?
        when {
            userID != -1 -> doAsync {
                val client = ClientManager.client
                val request = Request.Builder()
                        .url("https://chat.stackoverflow.com/users/thumbs/$userID")
                        .build()
                val response = client.newCall(request).execute()
                val jsonResult = JSONObject(response.body().string())
                uiThread {
                    userName.text = jsonResult.getString("name")
                    userEmail.text = prefs.getString("email", "")
                }
            }
            seID != -1 -> doAsync {
                val client = ClientManager.client
                val request = Request.Builder()
                        .url("https://chat.stackoverflow.com/users/thumbs/$userID")
                        .build()
                val response = client.newCall(request).execute()
                val jsonResult = JSONObject(response.body().string())
                uiThread {
                    userName.text = jsonResult.getString("name")
                    userEmail.text = prefs.getString("email", "")
                }
            }
            else -> Log.e("ChatActivity", "Userid not found")
        }
    }

    /**
     * When network is connected add the rooms to the drawer
     */
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Log.d("onServiceConnected", "Service connect")
        serviceBinder = binder as IncomingEventServiceBinder

        //Asynchronously add rooms to drawer
        doAsync {
            fkey = serviceBinder.loadRoom(ChatRoom(Client.SITE_STACK_OVERFLOW, 1)).fkey
            addRoomsToDrawer(fkey)
        }

        //Load a default room
        loadChatFragment(ChatRoom(prefs.getString("lastRoomSite", Client.SITE_STACK_OVERFLOW), prefs.getInt("lastRoomNum", 15)))
        room_num = 15
    }

    /**
     * onDestroy unbind the bound IncomingEventService
     * Protects from leaking.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(this)
            isBound = false
        }
        if (!prefs.getBoolean(UserPreferenceKeys.IS_LOGGED_IN, false)) {
            PersistentCookieStore(App.instance).removeAll()
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * On resume rebind the service to listen for incoming events
     */
    override fun onResume() {
        super.onResume()
        if (!isBound) {
            val serviceIntent = Intent(this, IncomingEventService::class.java)
            this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
            isBound = true
        }
    }

    /**
     * Add all the rooms that the user is in to the NavigationDrawer
     */
    fun addRoomsToDrawer(fkey: String) {
        val soID = prefs.getInt("SOID", -1)
        soRoomList.clear()
        seRoomList.clear()

        doAsync {
            //If the user has a StackOverflow ID then load rooms
            if (soID != -1) {
                val client = ClientManager.client

                val soChatPageRequest = Request.Builder()
                        .url("${Client.SITE_STACK_OVERFLOW}/users/thumbs/$soID")
                        .build()
                val response = client.newCall(soChatPageRequest).execute()
                val jsonData = response.body().string()
                Log.wtf("JSON", jsonData)
                val result = JSONObject(jsonData)

                runOnUiThread {
                    //Clear all the rooms and make the text visible
                    so_header_text.visibility = View.VISIBLE
                    stackoverflow_room_list.visibility = View.VISIBLE
                    //For each room, create a room and add it to the list
                    val rooms = result.getJSONArray("rooms")
                    for (i in 0..(rooms.length() - 1)) {
                        val room = rooms.getJSONObject(i)
                        val roomName = room.getString("name")
                        val roomNum = room.getLong("id")
                        //Create room and add it to the list
                        createRoom(Client.SITE_STACK_OVERFLOW, roomName, roomNum, 0, fkey)
                    }
                    //If the rooms are empty then remove the list and header
                    if (rooms.length() == 0) {
                        so_header_text.visibility = View.GONE
                        stackoverflow_room_list.visibility = View.GONE
                    }
                }
            } else {
                runOnUiThread {
                    so_header_text.visibility = View.GONE
                    stackoverflow_room_list.visibility = View.GONE
                }
            }

            //If the user has a StackExchange ID then load rooms
            val seID = prefs.getInt("SEID", -1)
            if (seID != -1) {
                val client = ClientManager.client

                val soChatPageRequest = Request.Builder()
                        .url("${Client.SITE_STACK_EXCHANGE}/users/thumbs/$seID")
                        .build()
                val response = client.newCall(soChatPageRequest).execute()
                val jsonData = response.body().string()
                val result = JSONObject(jsonData)

                runOnUiThread {
                    se_header_text.visibility = View.VISIBLE
                    stackexchange_room_list.visibility = View.VISIBLE
                }
                //For each room, create a room and add it to the list
                val rooms = result.getJSONArray("rooms")
                for (i in 0..(rooms.length() - 1)) {
                    val room = rooms.getJSONObject(i)
                    val roomName = room.getString("name")
                    val roomNum = room.getLong("id")
                    //Create room and add it to the list
                    createRoom(Client.SITE_STACK_EXCHANGE, roomName, roomNum, 0, fkey)
                }
                //If the rooms are empty then remove the list and header
                if (rooms.length() == 0) {
                    runOnUiThread {
                        se_header_text.visibility = View.GONE
                        stackexchange_room_list.visibility = View.GONE
                    }
                }
            } else {
                runOnUiThread {
                    se_header_text.visibility = View.GONE
                    stackexchange_room_list.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Create a room from the information we got in addRoomsToDrawer(), but get more information
     */
    fun createRoom(site: String, roomName: String, roomNum: Long, lastActive: Long, fkey: String) {
        if (site == Client.SITE_STACK_OVERFLOW) {
            if (soRoomList.any { it.roomID == roomNum }) return
        } else {
            if (seRoomList.any { it.roomID == roomNum }) return
        }
        doAsync {
            val client = ClientManager.client

            val soChatPageRequest = Request.Builder()
                    .url("$site/rooms/thumbs/$roomNum/")
                    .build()
            val response = client.newCall(soChatPageRequest).execute()
            val jsonData = response.body().string()
            val json = JSONObject(jsonData)

            //Get description for room
            val description = json.getString("description")

            //Is this a user's favorite?
            val isFavorite = json.getBoolean("isFavorite")

            //Get the room's tags
            val tags = json.getString("tags")

            //If this is for StackOverflow then add it to the SO list
            //Otherwise, add it to the SE list
            if (site == Client.SITE_STACK_OVERFLOW) {
                soRoomList.add(Room(roomName, roomNum, description, lastActive, isFavorite, tags, fkey))
                runOnUiThread {
                    soRoomAdapter.notifyDataSetChanged()
                }
            } else {
                runOnUiThread {
                    seRoomList.add(Room(roomName, roomNum, description, lastActive, isFavorite, tags, fkey))
                    seRoomAdapter.notifyDataSetChanged()
                }
            }
        }
    }


    //Inflate the menu for the Toolbar
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    /**
     * Manage each menu item selected
     * With search_rooms we allow the user to join a room by id (eventually search for rooms)
     * With action_logout we log out of the app
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.search_rooms -> {
                    //Create an AlertDialog to join a room
                    val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_SO))

                    //Set the title
                    builder.setTitle("Join Room")

                    //Create a layout to set as Dialog view
                    val l = LinearLayout(applicationContext)

                    //Set orientation
                    l.orientation = LinearLayout.VERTICAL

                    //Get DPI so we can add padding and look natural
                    val dpi = application.resources.displayMetrics.density.toInt()
                    l.setPadding((19 * dpi), (5 * dpi), (14 * dpi), (5 * dpi))

                    //Input for inputting room ID
                    val input = EditText(applicationContext)

                    //Set hint, input type as a number, and match parent
                    input.hint = "Enter Room ID"
                    input.inputType = InputType.TYPE_CLASS_NUMBER
                    input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

                    //Add EditText to view
                    l.addView(input)

                    //Create dropdown to choose which site
                    val spinner = Spinner(applicationContext)
                    input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
                    spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, arrayListOf("StackOverflow", "StackExchange"))
                    var site = Client.SITE_STACK_OVERFLOW

                    //Add dropdown to View
                    l.addView(spinner)

                    //On dropdown click set the site as the appropriate site
                    spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                            when (pos) {
                            //If the first item is clicked, set the site as SO
                                0 -> site = Client.SITE_STACK_OVERFLOW
                            //Otherwise set to SE
                                1 -> site = Client.SITE_STACK_EXCHANGE
                            }
                        }

                        //Default as StackOverflow
                        override fun onNothingSelected(parent: AdapterView<out Adapter>?) {
                            site = Client.SITE_STACK_OVERFLOW
                        }
                    }

                    //Set the view to the layout built above
                    builder.setView(l)

                    //Join the room the user chose and load it into the container
                    builder.setPositiveButton("Join Room", { dialog, _ ->
                        loadChatFragment(ChatRoom(site, input.text.toString().toInt()))
                        room_num = input.text.toString().toInt()
                        //Dismiss the dialog
                        dialog.dismiss()
                    })
                    builder.setNegativeButton("Cancel", { dialog, _ ->
                        //Dismiss the dialog and cancel
                        dialog.cancel()
                    })

                    //Show the dialog
                    builder.show()
                }
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
            //Logout of app by clearing all SharedPreferences and loading the LoginActivity
                R.id.action_logout -> {
                    prefs.edit().clear().apply()
                    finish()
                }
            //This is handled by the ChatFragment
                R.id.room_information -> return false
                R.id.room_stars -> return false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Log.d("ChatActivity", "Service disconnect")
    }

    override fun onItemClick(chatRoom: ChatRoom) {

        val roomNo = chatRoom.num
        if ((room_num!=-1) && room_num != roomNo) {
             room_num = room_num
            doAsync {
                addChatFragment(createChatFragment(chatRoom))
            }
            prefs.edit().putString("lastRoomSite", chatRoom.site).putInt("lastRoomNum", chatRoom.num).apply()
        }

        drawer_layout.closeDrawers()
    }

    //Load the chat fragment by creating it and adding it
    fun loadChatFragment(room: ChatRoom) {
        doAsync {
            addChatFragment(createChatFragment(room))
        }
        prefs.edit().putString("lastRoomSite", room.site).putInt("lastRoomNum", room.num).apply()
        drawer_layout.closeDrawers()
    }

    //Add the fragment by replacing the current fragment with the new ChatFragment
    private fun addChatFragment(fragment: ChatFragment) {
        runOnUiThread {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitAllowingStateLoss()
        }
    }

    //Create the ChatFragment by joining room and creating an instance of the ChatFragment
    @Throws(IOException::class, JSONException::class)
    private fun createChatFragment(room: ChatRoom): ChatFragment {
        val roomInfo = serviceBinder.loadRoom(room)
        rejoinFavoriteRooms()
        serviceBinder.joinRoom(room, roomInfo.fkey)
        val chatFragment = ChatFragment.createInstance(room, roomInfo.name, roomInfo.fkey)
        serviceBinder.registerListener(room, chatFragment)

        //Set the title and color of Toolbar depending on room
        runOnUiThread {
            supportActionBar?.title = roomInfo.name

            //If the room is for StackOverflow set the color orange, otherwise SE is blue
            if (room.site == Client.SITE_STACK_OVERFLOW) {
                supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.stackoverflow_orange)))
                //Set the multitasking color to orange
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    this.setTaskDescription(ActivityManager.TaskDescription(ActivityManager.TaskDescription().label, ActivityManager.TaskDescription().icon, ContextCompat.getColor(applicationContext, R.color.stackoverflow_orange)))
                }

            } else if (room.site == Client.SITE_STACK_EXCHANGE) {
                supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.stackexchange_blue)))
                //Set the multitasking color to blue
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    this.setTaskDescription(ActivityManager.TaskDescription(ActivityManager.TaskDescription().label, ActivityManager.TaskDescription().icon, ContextCompat.getColor(applicationContext, R.color.stackexchange_blue)))
                }
            }
        }
        return chatFragment
    }

    /**
     * Function to rejoin the user's favorite rooms
     */

    private fun rejoinFavoriteRooms() {
        val client = ClientManager.client
        doAsync {
            //Get the fkey to make a call
            val soRoomInfo = serviceBinder.loadRoom(ChatRoom(Client.SITE_STACK_OVERFLOW, 1))

            //Create a body and add appropriate parameters
            val soRequestBody = FormEncodingBuilder()
                    .add("fkey", soRoomInfo.fkey)
                    .add("immediate", "true")
                    .add("quiet", "true")
                    .build()

            //Add body and url and build call
            val soChatPageRequest = Request.Builder()
                    .url(Client.SITE_STACK_OVERFLOW + "/chats/join/favorite")
                    .post(soRequestBody)
                    .build()

            //Execute call
            client.newCall(soChatPageRequest).execute()

            //Do the same for StackExchange
            val seRoomInfo = serviceBinder.loadRoom(ChatRoom(Client.SITE_STACK_EXCHANGE, 1))
            val seRequestBody = FormEncodingBuilder()
                    .add("fkey", seRoomInfo.fkey)
                    .add("immediate", "true")
                    .add("quiet", "true")
                    .build()
            val seChatPageRequest = Request.Builder()
                    .url(Client.SITE_STACK_EXCHANGE + "/chats/join/favorite")
                    .post(seRequestBody)
                    .build()
            client.newCall(seChatPageRequest).execute()
        }
    }
}

//A class for a room with all the things we get from /rooms/thumbs/{id}
data class Room(val name: String, val roomID: Long, val description: String, val lastActive: Long?, var isFavorite: Boolean, val tags: String, val fkey: String)