package me.shreyasr.chatse.chat.adapters

import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.koushikdutta.ion.Ion
import kotlinx.android.synthetic.main.list_item_message.view.*
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
        holder.bindMessage(message)
    }

    fun update() {
        messages = events.messagePresenter.getEventsList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestampFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val messageView = itemView.findViewById(R.id.message_content) as TextView
        val userNameView = itemView.findViewById(R.id.message_user_name) as TextView
        val messageTimestamp = itemView.findViewById(R.id.message_timestamp) as TextView
        val editIndicator = itemView.findViewById(R.id.message_edit_indicator) as ImageView
        val oneboxImage = itemView.findViewById(R.id.message_image) as ImageView

        fun bindMessage(message: MessageEvent) {
            oneboxImage.visibility = View.GONE

            if (message.isDeleted) {
                messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.deleted))
                messageView.text = itemView.context.getString(R.string.removed)
            } else {
                if (!message.onebox) {
                    messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_text))
                    messageView.text = message.content
                    //If Android version is 24 and above use the updated version, otherwise use the deprecated version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        messageView.text = Html.fromHtml(message.content, Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        messageView.text = Html.fromHtml(message.content)
                    }
                } else {
                    oneboxImage.visibility = View.VISIBLE

                    Ion.with(itemView.context)
                            .load(message.onebox_content)
                            .intoImageView(itemView.message_image)
                    messageView.text = ""
                    //TODO fix the images, reverting to just showing the image URL for now
                }
            }

            userNameView.text = message.userName
            messageTimestamp.text = timestampFormat.format(Date(message.timestamp * 1000))
            editIndicator.visibility = if (message.isEdited) View.VISIBLE else View.INVISIBLE
        }
    }
}