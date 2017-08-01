package me.shreyasr.chatse.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.koushikdutta.ion.Ion
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.room_nav_header.*
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.adapters.RoomAdapter
import me.shreyasr.chatse.chat.service.IncomingEventService
import me.shreyasr.chatse.chat.service.IncomingEventServiceBinder
import me.shreyasr.chatse.login.LoginActivity
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.json.JSONException
import timber.log.Timber
import java.io.IOException

class ChatActivity : AppCompatActivity(), ServiceConnection {

    private lateinit var serviceBinder: IncomingEventServiceBinder
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    val soRoomList = arrayListOf<Room>()
    val seRoomList = arrayListOf<Room>()
    lateinit var soRoomAdapter: RoomAdapter
    lateinit var seRoomAdapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_chat)

        soRoomAdapter = RoomAdapter(Client.SITE_STACK_OVERFLOW, soRoomList, this)
        seRoomAdapter = RoomAdapter(Client.SITE_STACK_EXCHANGE, seRoomList, this)
        stackoverflow_room_list.adapter = soRoomAdapter
        stackoverflow_room_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        stackexchange_room_list.adapter = seRoomAdapter
        stackexchange_room_list.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)

        runOnUiThread {
            soRoomAdapter.notifyDataSetChanged()
            seRoomAdapter.notifyDataSetChanged()
        }
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        drawer_layout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
                addRoomsToDrawer()
            }
        })
        toggle.syncState()
        loadUserData()

        val serviceIntent = Intent(this, IncomingEventService::class.java)
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)

        val handlerThread = HandlerThread("ChatActivityNetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)
    }

    fun loadUserData() {
        val userID = defaultSharedPreferences.getInt("SOID", -1)
        val seID = defaultSharedPreferences.getInt("SEID", -1)
        if (userID != -1) {
            Ion.with(applicationContext)
                    .load("https://chat.stackoverflow.com/users/thumbs/$userID")
                    .asJsonObject()
                    .setCallback { e, result ->
                        if (e != null) {
                            Log.e("loadUserData()", e.message)
                        }
                        userName.text = result.get("name").asString
                        userEmail.text = defaultSharedPreferences.getString("email", "")
                    }
        } else if (seID != -1) {
            Ion.with(applicationContext)
                    .load("https://chat.stackexchange.com/users/thumbs/$userID")
                    .asJsonObject()
                    .setCallback { e, result ->
                        if (e != null) {
                            Log.e("loadUserData()", e.message)
                        }
                        userName.text = result.get("name").asString
                        userEmail.text = defaultSharedPreferences.getString("email", "")
                    }
        } else {
            Log.e("ChatActivity", "Userid not found")
        }
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Timber.d("Service connect")
        serviceBinder = binder as IncomingEventServiceBinder
        addRoomsToDrawer()
        loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, 15))
    }

    fun addRoom(site: String, room: Room) {
        if (site == Client.SITE_STACK_OVERFLOW) {
            soRoomList.add(room)
            soRoomAdapter.notifyDataSetChanged()
        } else {
            seRoomList.add(room)
            seRoomAdapter.notifyDataSetChanged()
        }
    }

    fun addRoomsToDrawer() {
        soRoomList.clear()
        seRoomList.clear()
        val soID = defaultSharedPreferences.getInt("SOID", -1)
        if (soID != -1) {
            Ion.with(applicationContext)
                    .load("${Client.SITE_STACK_OVERFLOW}/users/thumbs/$soID")
                    .asJsonObject()
                    .setCallback { e, result ->
                        if (e != null) {
                            Log.wtf("addRoomsToDrawer", e.message)
                        } else {
                            val rooms = result.get("rooms").asJsonArray
                            rooms.forEach {
                                val room = it.asJsonObject
                                val roomName = room.get("name").asString
                                val roomNum = room.get("id").asLong
                                soRoomList.add(Room(roomName, roomNum, 0))
                            }
                            runOnUiThread {
                                soRoomAdapter.notifyDataSetChanged()
                                Log.wtf("addRoomsToDrawer", soRoomAdapter.list.size.toString())
                            }
                        }
                    }

        }

        val seID = defaultSharedPreferences.getInt("SEID", -1)
        if (seID != -1) {
            Ion.with(applicationContext)
                    .load("${Client.SITE_STACK_EXCHANGE}/users/thumbs/$seID")
                    .asJsonObject()
                    .setCallback { e, result ->
                        if (e != null) {
                            Log.wtf("addRoomsToDrawer", e.message)
                        } else {
                            val rooms = result.get("rooms").asJsonArray
                            rooms.forEach {
                                val room = it.asJsonObject
                                val roomName = room.get("name").asString
                                val roomNum = room.get("id").asLong

                                seRoomList.add(Room(roomName, roomNum, 0))
                            }
                            runOnUiThread {
                                seRoomAdapter.notifyDataSetChanged()
                            }
                        }
                    }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null) {
            when (item.itemId) {
                R.id.search_rooms -> startActivity(intentFor<RoomSearchActivity>())
                R.id.action_logout -> {
                    startActivity(Intent(applicationContext, LoginActivity::class.java))
                    defaultSharedPreferences.edit().clear().apply()
                }
                R.id.room_information -> return false
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Service disconnect")
    }

    fun loadChatFragment(room: ChatRoom) {
        networkHandler?.post {
            try {
                addChatFragment(createChatFragment(room))
            } catch (e: IOException) {
                Timber.e("Failed to create chat fragment", e)
            } catch (e: JSONException) {
                Timber.e("Failed to create chat fragment", e)
            }
        }
        drawer_layout.closeDrawers()
    }

    private fun addChatFragment(fragment: ChatFragment) {
        uiThreadHandler.post {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commitAllowingStateLoss()
        }
    }

    @Throws(IOException::class, JSONException::class)
    private fun createChatFragment(room: ChatRoom): ChatFragment {
        val roomInfo = serviceBinder.loadRoom(room)
        rejoinFavoriteRooms()
        serviceBinder.joinRoom(room, roomInfo.fkey)
        val chatFragment = ChatFragment.createInstance(room, roomInfo.name, roomInfo.fkey)
        serviceBinder.registerListener(room, chatFragment)
        runOnUiThread {
            supportActionBar?.title = roomInfo.name
        }
        return chatFragment
    }

    fun rejoinFavoriteRooms() {
        val client = ClientManager.client
        doAsync {
            val soRoomInfo = serviceBinder.loadRoom(ChatRoom(Client.SITE_STACK_OVERFLOW, 1))
            val soRequestBody = FormEncodingBuilder()
                    .add("fkey", soRoomInfo.fkey)
                    .add("immediate", "true")
                    .add("quiet", "true")
                    .build()
            val soChatPageRequest = Request.Builder()
                    .url(Client.SITE_STACK_OVERFLOW + "/chats/join/favorite")
                    .post(soRequestBody)
                    .build()
            client.newCall(soChatPageRequest).execute()

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
