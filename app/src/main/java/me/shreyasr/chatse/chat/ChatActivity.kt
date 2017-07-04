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
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.service.IncomingEventService
import me.shreyasr.chatse.chat.service.IncomingEventServiceBinder
import me.shreyasr.chatse.network.Client
import org.json.JSONException
import timber.log.Timber
import java.io.IOException

class ChatActivity : AppCompatActivity(), ServiceConnection {

    internal lateinit var pagerAdapter: ChatFragmentPagerAdapter
    internal lateinit var viewPager: ViewPager
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

        //TODO: Don't hardcode this.
        loadChatFragment(ChatRoom(Client.SITE_STACK_EXCHANGE, 1))
        loadChatFragment(ChatRoom(Client.SITE_STACK_OVERFLOW, 15))
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Service disconnect")
    }

    private fun loadChatFragment(room: ChatRoom) {
        networkHandler?.post(object : Runnable {
            override fun run() {
                try {
                    addChatFragment(createChatFragment(room))
                } catch (e: IOException) {
                    Timber.e("Failed to create chat fragment", e)
                } catch (e: JSONException) {
                    Timber.e("Failed to create chat fragment", e)
                }

            }
        })
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId


        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupTabLayout() {
        // Get and set up tablayout
        val tabLayout = findViewById(R.id.room_tabs) as TabLayout
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        tabLayout.post { tabLayout.setupWithViewPager(viewPager) }
    }
}