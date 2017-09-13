package com.tristanwiley.chatse.stars

import android.graphics.drawable.ColorDrawable

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import kotlinx.android.synthetic.main.activity_stars.*
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup

class StarsActivity : AppCompatActivity() {
    private val eventList = arrayListOf<ChatEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)
        val room = intent.getParcelableExtra<ChatRoom>("room")
        val roomName = intent.getStringExtra("roomName")

        //Set toolbar as SupportActionBar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Color the toolbar for StackOverflow as a default
        supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.stackoverflow_orange)))
        if (room.site == Client.SITE_STACK_EXCHANGE) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(applicationContext, R.color.stackexchange_blue)))
        }

        supportActionBar?.title = "Stars - $roomName"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        doAsync {
            val client = ClientManager.client
            val newMessageRequest = Request.Builder()
                    .url(room.site + "/rooms/info/" + room.num + "/android/?tab=stars")
                    .build()

            val response = client.newCall(newMessageRequest).execute()
            val doc = Jsoup.parse(response.body().string())
            val messages = doc.getElementsByClass("monologue")
            messages.forEach {
                val event = ChatEvent()
                event.user_id = it.classNames().toList()[1].removePrefix("user-").toInt()
                event.user_name = it.getElementsByClass("username")[0].child(0).text()
                event.contents = it.getElementsByClass("content")[0].html()
                event.message_starred = true
                val times = it.getElementsByClass("times")[0].text()
                if (times.isNotEmpty()) {
                    event.message_stars = it.getElementsByClass("times")[0].text().toInt()
                } else {
                    event.message_stars = 1
                }
                if (event.contents.contains("onebox")) {
                    event.message_onebox = true
                    event.onebox_type = it.getElementsByClass("content")[0].child(0).classNames().toList()[1].removePrefix("ob-")
                }
                event.star_timestamp = it.getElementsByClass("timestamp")[0].text()
                eventList.add(event)
            }

            runOnUiThread {
                val messageAdapter = StarsMessageAdapter(applicationContext, eventList, room)
                starsRecyclerView.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, true)
                starsRecyclerView.adapter = messageAdapter
            }
        }


    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}