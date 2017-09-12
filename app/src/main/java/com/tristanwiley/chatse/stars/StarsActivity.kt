package com.tristanwiley.chatse.stars

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import com.tristanwiley.chatse.network.ClientManager
import kotlinx.android.synthetic.main.activity_stars.*
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup

class StarsActivity : AppCompatActivity() {
    val eventList = arrayListOf<ChatEvent>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)

        val room = intent.getParcelableExtra<ChatRoom>("room")


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
                val messageAdapter = StarsMessageAdapter(applicationContext, getEventsListfromStarPage(room), room)
                starsRecyclerView.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, true)
                starsRecyclerView.adapter = messageAdapter
            }
        }


    }

    private fun getEventsListfromStarPage(room: ChatRoom): ArrayList<ChatEvent> {

        return eventList
    }
}
