package me.shreyasr.chatse.chat

import android.content.Context
import android.content.res.Resources
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

class MessageAdapter(val events: EventList, private val res: Resources, private val context: Context) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    override fun onBindViewHolder(viewHolder: MessageViewHolder?, pos: Int) {
        val message = messages[pos]
        val holder = viewHolder as MessageViewHolder

        if (message.isDeleted) {
            holder.messageView.setTextColor(res.getColor(R.color.deleted))
            holder.messageView.text = "(removed)"
            //            Glide.clear(holder.oneboxImage);
            holder.oneboxImage.setImageDrawable(null)
        } else {
            if (!message.onebox) {
                holder.messageView.setTextColor(res.getColor(R.color.primary_text))
                //TODO: Testing
                // holder.messageView.setText(message.content);
                holder.messageView.text = Html.fromHtml(message.content)
                //                Ion.clear(holder.oneboxImage);
                //                holder.oneboxImage.setImageDrawable(null); // only needed with placeholder
            } else {
                Log.wtf("The Image Url", message.onebox_content)
                Ion.with(context)
                        .load(message.onebox_content)
                        .intoImageView(holder.oneboxImage)
                // When we load an image remove any text from being recycled from the previous item.
                holder.messageView.text = ""
            }
        }

        holder.userNameView.text = message.userName
        holder.messageTimestamp.text = timestampFormat.format(Date(message.timestamp * 1000))
        holder.editIndicator.visibility = if (message.isEdited) View.VISIBLE else View.INVISIBLE
    }

    var messages: List<MessageEvent> = ArrayList()
    private val timestampFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())

    fun update() {
        messages = events.messagePresenter.getEventsList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(view)
    }


    override fun getItemCount(): Int {
        return messages.size
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var messageView = itemView.findViewById(R.id.message_content) as TextView

        //@BindView(R.id.message_user_name)
        var userNameView = itemView.findViewById(R.id.message_user_name) as TextView
        //@BindView(R.id.c)
        var messageTimestamp = itemView.findViewById(R.id.message_timestamp) as TextView
        //@BindView(R.id.message_edit_indicator)
        var editIndicator = itemView.findViewById(R.id.message_edit_indicator) as ImageView
        //        @BindView(R.id.message_image)
        var oneboxImage = itemView.findViewById(R.id.message_image) as ImageView
    }
}
