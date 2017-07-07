package me.shreyasr.chatse.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.squareup.okhttp.Request
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

    private lateinit var pagerAdapter: ChatFragmentPagerAdapter
    private lateinit var viewPager: ViewPager
    private lateinit var serviceBinder: IncomingEventServiceBinder
    private var networkHandler: Handler? = null
    private val uiThreadHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_chat)

        // Setup toolbar
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val serviceIntent = Intent(this, IncomingEventService::class.java)
        this.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)

        val handlerThread = HandlerThread("ChatActivityNetworkHandlerThread")
        handlerThread.start()
        networkHandler = Handler(handlerThread.looper)

        viewPager = this.findViewById(R.id.pager) as ViewPager
        pagerAdapter = ChatFragmentPagerAdapter(supportFragmentManager)
        viewPager.adapter = pagerAdapter
    }

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Timber.d("Service connect")
        serviceBinder = binder as IncomingEventServiceBinder

        joinRooms()
    }

    private fun joinRooms() {
        val client = ClientManager.client
        doAsync {
            val chatPageRequest = Request.Builder()
                    .url("https://chat.stackoverflow.com/rooms?tab=mine&sort=active")
                    .build()
            val chatPageResponse = client.newCall(chatPageRequest).execute()
            val chatPage = Jsoup.parse(chatPageResponse.body().string())

            val rooms = chatPage.getElementsByClass("room-name").filter { it.children()[0].hasAttr("href") }
            rooms.forEach {
                val roomNum = it.child(0).attr("href").split("/")[2].toInt()
                loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, roomNum))
            }

        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Service disconnect")
    }

    private fun loadChatFragment(room: ChatRoom) {
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
            pagerAdapter.addFragment(fragment)
            setupTabLayout()
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

    private fun setupTabLayout() {
        // Get and set up tablayout
        val tabLayout = findViewById(R.id.room_tabs) as TabLayout
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        tabLayout.post { tabLayout.setupWithViewPager(viewPager) }
    }
}