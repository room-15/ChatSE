package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.koushikdutta.ion.Ion
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.event.presenter.message.MessageEvent
import java.util.*

/**
 * Created by Tristan on 7/30/17
 */

class UsersAdapter(val mContext: Context, val events: EventList, var users: ArrayList<MessageEvent> = ArrayList()) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    override fun onBindViewHolder(viewHolder: UsersViewHolder?, pos: Int) {
        val user = users[pos]
        val holder = viewHolder as UsersViewHolder
        holder.bindMessage(user)
    }

    fun update() {
        users.clear()
        users.addAll(events.messagePresenter.getUsersList())
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_user, parent, false)
        return UsersViewHolder(mContext, view)
    }

    override fun getItemCount() = users.size

    class UsersViewHolder(val mContext: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPicture = itemView.findViewById(R.id.user_icon) as ImageView
        val userName = itemView.findViewById(R.id.user_name) as TextView

        fun bindMessage(user: MessageEvent) {
            userName.text = user.userName
            Log.wtf("UserIcon", user.email_hash)
            Ion.with(mContext)
                    .load(user.email_hash)
                    .setLogging("ION", Log.ERROR)
                    .intoImageView(userPicture)

            itemView.setOnClickListener {
                Ion.with(mContext)
                        .load("https://chat.stackoverflow.com/users/thumbs/${user.userId}")
                        .asJsonObject()
                        .setCallback { e, result ->
                            if (e != null) {
                                Log.wtf("ChatFragment", e.message)
                            } else {
                                val builder = AlertDialog.Builder(mContext)
                                        .setTitle(result.get("name").asString)
                                        .setNegativeButton("Close", { dialog, _ ->
                                            dialog.cancel()
                                        })
                                val s: SpannableString
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    s = SpannableString(Html.fromHtml(result.get("user_message").asString, Html.FROM_HTML_MODE_COMPACT))
                                } else {
                                    @Suppress("DEPRECATION")
                                    s = SpannableString(Html.fromHtml(result.get("user_message").asString))
                                }
                                Linkify.addLinks(s, Linkify.ALL)
                                builder.setMessage(s)
                                builder.show()
                            }
                        }
            }
        }
    }
}