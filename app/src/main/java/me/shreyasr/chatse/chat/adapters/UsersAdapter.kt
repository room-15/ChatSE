package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import me.shreyasr.chatse.R
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
        }
    }
}