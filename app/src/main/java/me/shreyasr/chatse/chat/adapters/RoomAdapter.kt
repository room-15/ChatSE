package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import me.shreyasr.chatse.App
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatActivity
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.chat.Room

class RoomAdapter(val site: String, val list: MutableList<Room>, val context: Context) : RecyclerView.Adapter<RoomAdapter.ListRowHolder>() {

    override fun onBindViewHolder(viewHolder: ListRowHolder?, position: Int) {
        val room = list[position]
        val holder = viewHolder
        holder?.bindMessage(context, room)
    }

    override fun getItemCount() = list.size


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ListRowHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.drawer_list_item, parent, false)
        return ListRowHolder(context, view, site)
    }

    class ListRowHolder(val mContext: Context, itemView: View, val site: String) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById(R.id.room_name) as TextView

        fun bindMessage(context: Context, room: Room) {
            name.text = room.name

            itemView.setOnClickListener {
                val roomNum = room.roomID.toInt()
                (mContext as ChatActivity).loadChatFragment(ChatRoom(site, roomNum))
            }
        }

    }

}