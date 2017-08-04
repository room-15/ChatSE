package me.shreyasr.chatse.chat.adapters

import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatActivity
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.chat.Room
import me.shreyasr.chatse.network.ClientManager
import org.jetbrains.anko.doAsync

class RoomAdapter(val site: String, val list: MutableList<Room>, val context: Context) : RecyclerView.Adapter<RoomAdapter.ListRowHolder>() {

    override fun onBindViewHolder(viewHolder: ListRowHolder?, position: Int) {
        val room = list[position]
        val holder = viewHolder
        holder?.bindMessage(room)
    }

    override fun getItemCount() = list.size


    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ListRowHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.drawer_list_item, parent, false)
        return ListRowHolder(context, view, site)
    }

    class ListRowHolder(val mContext: Context, itemView: View, val site: String) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById(R.id.room_name) as TextView

        fun bindMessage(room: Room) {
            name.text = room.name

            itemView.setOnClickListener {
                val roomNum = room.roomID.toInt()
                (mContext as ChatActivity).loadChatFragment(ChatRoom(site, roomNum))
            }

            itemView.setOnLongClickListener {
                val favoriteToggleString: String
                if (room.isFavorite) {
                    favoriteToggleString = "Remove from Favorites"
                } else {
                    favoriteToggleString = "Add to Favorites"
                }

                AlertDialog.Builder(mContext)
                        .setTitle("Modify Room")
                        .setMessage("Would you like to modify room #${room.roomID}, ${room.name}?")
                        .setPositiveButton("Leave Room", { dialog, _ ->
                            leaveRoom(room.roomID, room.fkey)
                            dialog.dismiss()
                        })
                        .setNegativeButton(favoriteToggleString, { dialog, _ ->
                            toggleFavoriteRoom(room, room.fkey)
                            dialog.dismiss()
                        })
                        .setNeutralButton("Cancel", { dialog, _ ->
                            dialog.cancel()
                        })
                        .show()
                true
            }
        }

        fun leaveRoom(roomID: Long, fkey: String) {
            doAsync {
                val client = ClientManager.client

                val soRequestBody = FormEncodingBuilder()
                        .add("fkey", fkey)
                        .add("quiet", "true")
                        .build()
                val soChatPageRequest = Request.Builder()
                        .url(site + "/chats/leave/" + roomID)
                        .post(soRequestBody)
                        .build()
                client.newCall(soChatPageRequest).execute()
            }
        }

        fun toggleFavoriteRoom(room: Room, fkey: String) {
            room.isFavorite = !room.isFavorite

            doAsync {
                val client = ClientManager.client

                val soRequestBody = FormEncodingBuilder()
                        .add("fkey", fkey)
                        .add("roomId", room.roomID.toString())
                        .build()
                val soChatPageRequest = Request.Builder()
                        .url(site + "/rooms/favorite/")
                        .post(soRequestBody)
                        .build()
                client.newCall(soChatPageRequest).execute()
            }
        }

    }

}