package me.shreyasr.chatse.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import com.squareup.okhttp.Request
import kotlinx.android.synthetic.main.activity_chat.*
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.service.IncomingEventService
import me.shreyasr.chatse.chat.service.IncomingEventServiceBinder
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.json.JSONException
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException


class ChatActivity : AppCompatActivity(), ServiceConnection {

    private lateinit var mDrawerList: ListView
    private lateinit var serviceBinder: IncomingEventServiceBinder
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())
    val roomList = mutableListOf<Room>()
    lateinit var mAdapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_chat)

        mAdapter = RoomAdapter(roomList, applicationContext)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        mDrawerList = findViewById(R.id.lst_menu_items) as ListView
        mDrawerList.adapter = mAdapter
        mDrawerList.onItemClickListener = OnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
            loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, mAdapter.getItem(position).roomID.toInt()))
            drawer_layout.closeDrawers()
        }

        val serviceIntent = Intent(this, IncomingEventService::class.java)
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)


        val handlerThread = HandlerThread("ChatActivityNetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Timber.d("Service connect")
        serviceBinder = binder as IncomingEventServiceBinder
        addRoomsToDrawer()
        loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, 15))
    }

    private fun addRoomsToDrawer() {
        val client = ClientManager.client
        roomList.clear()
        doAsync {
            val chatPageRequest = Request.Builder()
                    .url("https://chat.stackoverflow.com/rooms?tab=favorite&sort=active")
                    .build()
            val chatPageResponse = client.newCall(chatPageRequest).execute()
            val chatPage = Jsoup.parse(chatPageResponse.body().string())

            val rooms = chatPage.getElementsByClass("room-name").filter { it.children()[0].hasAttr("href") }
            rooms.forEach {
                val roomName = it.attr("title")
                val roomNum = it.child(0).attr("href").split("/")[2].toInt()

                roomList.add(Room(roomName, roomNum.toLong(), 0))
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

        return chatFragment
    }

}