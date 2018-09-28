package com.tristanwiley.chatse.chat.adapters

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.SpannableString
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.event.EventList
import com.tristanwiley.chatse.event.presenter.message.MessageEvent
import com.tristanwiley.chatse.extensions.loadUrl
import com.tristanwiley.chatse.network.ClientManager
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject


/**
 * Adapter to display the users in the right NavigationDrawer of the activity.
 * @param mContext: Context
 * @param events: All the events to get the users from
 */

class UsersAdapter(private val mContext: Context, private val events: EventList, private var users: ArrayList<MessageEvent> = ArrayList()) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    override fun onBindViewHolder(holder: UsersViewHolder, pos: Int) {
        val user = users[pos]
        holder.bindMessage(user)
    }

    //Used to update the list on new events
    fun update() {
        users.clear()
        users.addAll(events.messagePresenter.getUsersList())
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user, parent, false)
        return UsersAdapter.UsersViewHolder(mContext, view)
    }

    override fun getItemCount() = users.size

    class UsersViewHolder(private val mContext: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userPicture: ImageView = itemView.findViewById(R.id.user_icon)
        private val userName: TextView = itemView.findViewById(R.id.user_name)

        fun bindMessage(user: MessageEvent) {
            //Set the username to the TextView
            userName.text = user.userName

            userPicture.loadUrl(user.emailHash)

            //On click, show information about user
            itemView.setOnClickListener {
                doAsync {
                    val client = ClientManager.client
                    val request = Request.Builder()
                            .url("https://chat.stackoverflow.com/users/thumbs/${user.userId}")
                            .build()
                    val response = client.newCall(request).execute()
                    val result = JSONObject(response.body().string())

                    uiThread {
                        //Create AlertDialog with title of their name
                        val builder = AlertDialog.Builder(mContext)
                                .setTitle(result.getString("name"))

                        //Create layout
                        val layout = LinearLayout(mContext)

                        //Get DPI so we can add padding and look natural
                        val dpi = mContext.resources.displayMetrics.density.toInt()
                        layout.setPadding((19 * dpi), (5 * dpi), (14 * dpi), (5 * dpi))

                        val s: SpannableString

                        //Set user message to the body of the AlertDialog and Linkify links
                        if (!result.isNull("user_message")) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                s = SpannableString(Html.fromHtml(result.getString("user_message"), Html.FROM_HTML_MODE_COMPACT))
                            } else {
                                @Suppress("DEPRECATION")
                                s = SpannableString(Html.fromHtml(result.getString("user_message")))
                            }
                            Linkify.addLinks(s, Linkify.ALL)
                        } else {
                            s = SpannableString("There's no user bio! :(")
                        }

                        //Set SpannableString to TextView
                        val tv = TextView(mContext)
                        tv.text = s

                        //Add TextView to Layout and set Layout to AlertDialog view
                        layout.addView(tv)
                        builder.setView(layout)

                        //Create cancel button to cancel dialog
                        builder.setNegativeButton("Cancel", { dialog, _ ->
                            dialog.cancel()
                        })

                        //Show Dialog
                        builder.show()
                    }
                }
            }
        }
    }
}

