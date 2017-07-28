package me.shreyasr.chatse.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import com.koushikdutta.ion.Ion
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
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException

class ChatActivity : AppCompatActivity(), ServiceConnection {

    private lateinit var serviceBinder: IncomingEventServiceBinder
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    val soRoomList = mutableListOf<Room>()
    val seRoomList = mutableListOf<Room>()
    lateinit var soRoomAdapter: RoomAdapter
    lateinit var seRoomAdapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_chat)

        soRoomAdapter = RoomAdapter(soRoomList, applicationContext)
        seRoomAdapter = RoomAdapter(seRoomList, applicationContext)
        runOnUiThread {
            soRoomAdapter.notifyDataSetChanged()
            seRoomAdapter.notifyDataSetChanged()
        }
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        loadUserData()

        val soID = defaultSharedPreferences.getInt("SOID", -1)
        if (soID != -1) {
            stackoverflow_room_list.adapter = soRoomAdapter
            stackoverflow_room_list.onItemClickListener = OnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                val roomNum = soRoomAdapter.getItem(position).roomID.toInt()
                loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, roomNum))
                drawer_layout.closeDrawers()
            }
        }

        val seID = defaultSharedPreferences.getInt("SEID", -1)
        if (seID != -1) {
            stackexchange_room_list.adapter = seRoomAdapter
            stackexchange_room_list.onItemClickListener = OnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                val roomNum = seRoomAdapter.getItem(position).roomID.toInt()
                loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, roomNum))
                drawer_layout.closeDrawers()
            }
        }


        val serviceIntent = Intent(this, IncomingEventService::class.java)
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)


        val handlerThread = HandlerThread("ChatActivityNetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)
    }

    fun loadUserData() {
        val userID = defaultSharedPreferences.getInt("SOID", -1)
        val seID = defaultSharedPreferences.getInt("SEID", -1)
        Log.e("USERID", userID.toString())
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

    fun addNewUser(user: String){
        Toast.makeText(applicationContext, "bruh nice" + user, Toast.LENGTH_SHORT).show()
    }

    private fun addRoomsToDrawer() {
        val client = ClientManager.client
        soRoomList.clear()
        seRoomList.clear()
        doAsync {
            val soChatPageRequest = Request.Builder()
                    .url("https://chat.stackoverflow.com/rooms?tab=favorite&sort=active")
                    .build()
            val soChatPageResponse = client.newCall(soChatPageRequest).execute()
            val soChatPage = Jsoup.parse(soChatPageResponse.body().string())

            val soRooms = soChatPage.getElementsByClass("room-name").filter { it.children()[0].hasAttr("href") }
            soRooms.forEach {
                val roomName = it.attr("title")
                val roomNum = it.child(0).attr("href").split("/")[2].toInt()

                soRoomList.add(Room(roomName, roomNum.toLong(), 0))
            }

            val seChatPageRequest = Request.Builder()
                    .url("https://chat.stackexchange.com/rooms?tab=favorite&sort=active")
                    .build()
            val seChatPageResponse = client.newCall(seChatPageRequest).execute()
            val seChatPage = Jsoup.parse(seChatPageResponse.body().string())

            val seRooms = seChatPage.getElementsByClass("room-name").filter { it.children()[0].hasAttr("href") }
            seRooms.forEach {
                val roomName = it.attr("title")
                val roomNum = it.child(0).attr("href").split("/")[2].toInt()

                seRoomList.add(Room(roomName, roomNum.toLong(), 0))
            }
            runOnUiThread {
                soRoomAdapter.notifyDataSetChanged()
                seRoomAdapter.notifyDataSetChanged()
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
        serviceBinder.joinRoom(room, roomInfo.fkey)
        val chatFragment = ChatFragment.createInstance(room, roomInfo.name, roomInfo.fkey)
        serviceBinder.registerListener(room, chatFragment)
        runOnUiThread {
            supportActionBar?.title = roomInfo.name
        }
        return chatFragment
    }

}