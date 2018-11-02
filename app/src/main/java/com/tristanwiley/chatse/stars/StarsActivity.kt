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
import com.tristanwiley.chatse.chat.ChatActivity
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import kotlinx.android.synthetic.main.activity_stars.*
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber


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
            Timber.i("messages : ${messages.size}" )
            messages.forEach {
                val event = ChatEvent()
                var userID = -1
                if (it.classNames().toList()[1].removePrefix("user-").isNotEmpty()) {
                    userID = it.classNames().toList()[1].removePrefix("user-").toInt()
                }
                event.userId = userID

                if (it.getElementsByClass("username")[0].children().isEmpty()) {
                    event.userName = it.getElementsByClass("username")[0].text()
                } else {
                    event.userName = it.getElementsByClass("username")[0].child(0).text()
                }

                if(it.getElementsByClass("message").isNotEmpty()) {
                    val messagesDiv = it.getElementsByClass("message")
                    if (messagesDiv != null) {
                        val messageId = messagesDiv.attr("id")
                        if (messageId.isNotEmpty()) {
                            val id = messageId.substringAfter("message-").toInt()
                            event.messageId = id
                            Timber.i("messages : ${id}" )
                        }
                    }
                }

                event.contents = it.getElementsByClass("content")[0].html()
                event.messageStarred = true
                val times = it.getElementsByClass("times")[0].text()
                if (times.isNotEmpty()) {
                    event.messageStars = it.getElementsByClass("times")[0].text().toInt()
                } else {
                    event.messageStars = 1
                }
                if (Jsoup.parse(event.contents).getElementsByClass("onebox").isNotEmpty()) {
                    event.messageOnebox = true
                    event.oneboxType = it.getElementsByClass("content")[0].child(0).classNames().toList()[1].removePrefix("ob-")
                    when (event.oneboxType) {
                        "image" -> {
//                            event.oneboxContent = it.getElementsByClass("user-image")[0].attr("src").removePrefix("//")
                            var url = it.getElementsByClass("user-image")[0].attr("src").removePrefix("//")
                            if (!url.contains("http")) {
                                url = "https://$url"
                            }
                            event.oneboxContent = url
                        }
                        "youtube" -> {
                            event.oneboxContent = it.getElementsByClass("ob-youtube")[0].child(0).attr("href")
                            event.oneboxExtra = it.getElementsByClass("ob-youtube-preview").attr("src")
                            event.contents = it.getElementsByClass("ob-youtube-title")[0].text()
                        }
                        "tweet" -> {
                            var contents = "<p>" + it.getElementsByClass("ob-status-text")[0].html() + "</p>"
                            contents += "<p>" + it.getElementsByClass("ob-tweet-info")[0].html() + "</p>"
                            event.oneboxContent = contents
                        }
                    }
                }
                event.starTimestamp = it.getElementsByClass("timestamp")[0].text()
                eventList.add(event)
            }
            lateinit var messageAdapter:StarsMessageAdapter
            runOnUiThread {
                 messageAdapter = StarsMessageAdapter(applicationContext, eventList, room)
                starsRecyclerView.layoutManager = LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, true)
                starsRecyclerView.adapter = messageAdapter
            }

            // fetch url for each message based on user Id
            for((index,event) in eventList.withIndex())
                doAsync {
                    val client = ClientManager.client
                    val request = Request.Builder()
                            .url("${room?.site}/users/thumbs/${event.userId}")
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
                        imageLink = room?.site + hash
                    }
                    event.emailHash = imageLink

                    // tell adapter about url fetched at index so that glide loads the same
                    runOnUiThread {
                        messageAdapter.notifyItemChanged(index)
                    }
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