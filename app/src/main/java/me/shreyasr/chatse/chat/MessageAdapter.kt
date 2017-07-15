package me.shreyasr.chatse.chat

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.koushikdutta.ion.Ion
import me.shreyasr.chatse.R
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.event.presenter.message.MessageEvent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageAdapter(val events: EventList, var messages: List<MessageEvent> = ArrayList()) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onBindViewHolder(viewHolder: MessageViewHolder?, pos: Int) {
        val message = messages[pos]
        val holder = viewHolder as MessageViewHolder
        val context = viewHolder.context

        if (message.isDeleted) {
            Log.wtf("DELETED", "TRUE")
            holder.messageView.setTextColor(ContextCompat.getColor(context, R.color.deleted))
            holder.messageView.text = context.getString(R.string.removed)
        } else {
            if (!message.onebox) {
                holder.messageView.setTextColor(ContextCompat.getColor(context, R.color.primary_text))
                holder.messageView.text = message.content
                //If Android version is 24 and above use the updated version, otherwise use the deprecated version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    holder.messageView.text = Html.fromHtml(message.content, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    holder.messageView.text = Html.fromHtml(message.content)
                }
            } else {
                Log.e("IMAGE", message.onebox_content)
                Ion.with(holder.oneboxImage)
                        .load("http://24.media.tumblr.com/tumblr_m32yg2ZGV51qbd47zo1_1280.jpg")
                holder.messageView.text = ""
                //TODO fix the images, reverting to just showing the image URL for now
//                    messageView.text = Html.fromHtml("<html><a href=\"" + message.onebox_content + "\">" + message.onebox_content + "</a></html>")
            }
        }

        holder.userNameView.text = message.userName
        holder.messageTimestamp.text = holder.timestampFormat.format(Date(message.timestamp * 1000))
        holder.editIndicator.visibility = if (message.isEdited) View.VISIBLE else View.INVISIBLE
    }

    fun update() {
        messages = events.messagePresenter.getEventsList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val messageView = itemView.findViewById(R.id.message_content) as TextView
        val userNameView = itemView.findViewById(R.id.message_user_name) as TextView
        val messageTimestamp = itemView.findViewById(R.id.message_timestamp) as TextView
        val editIndicator = itemView.findViewById(R.id.message_edit_indicator) as ImageView
        val oneboxImage = itemView.findViewById(R.id.message_image) as ImageView
        val context: Context = itemView.context
    }
}