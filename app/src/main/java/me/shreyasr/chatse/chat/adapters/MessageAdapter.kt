package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.util.Linkify
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.koushikdutta.ion.Ion
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ListHolder
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import kotlinx.android.synthetic.main.list_item_message.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.ChatRoom
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.event.presenter.message.MessageEvent
import me.shreyasr.chatse.network.Client
import me.shreyasr.chatse.network.ClientManager
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * The beautiful adapter that handles all new messages in the chat
 */
class MessageAdapter(val mContext: Context, val events: EventList, val chatFkey: String?, val room: ChatRoom?, var messages: ArrayList<MessageEvent> = ArrayList()) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onBindViewHolder(viewHolder: MessageViewHolder?, pos: Int) {
        val message = messages[pos]
        val holder = viewHolder as MessageViewHolder
        holder.bindMessage(message)
    }

    /**
     * Called when we want to update the messages (there's a new message)
     */
    fun update() {
        messages.clear()
        messages.addAll(events.messagePresenter.getEventsList())
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(mContext, view, chatFkey, room)
    }

    override fun getItemCount() = messages.size

    /**
     * ViewHolder that handles setting all content in itemView
     */
    class MessageViewHolder(val mContext: Context, itemView: View, val chatFkey: String?, val room: ChatRoom?) : RecyclerView.ViewHolder(itemView) {
        val timestampFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        val messageView = itemView.findViewById(R.id.message_content) as TextView
        val userNameView = itemView.findViewById(R.id.message_user_name) as TextView
        val messageTimestamp = itemView.findViewById(R.id.message_timestamp) as TextView
        val editIndicator = itemView.findViewById(R.id.message_edit_indicator) as ImageView
        val starIndicator = itemView.findViewById(R.id.message_star_indicator) as ImageView
        val starCount = itemView.findViewById(R.id.message_star_count) as TextView
        val oneboxImage = itemView.findViewById(R.id.message_image) as ImageView
        val userPicture = itemView.findViewById(R.id.message_user_picture) as ImageView

        fun bindMessage(message: MessageEvent) {
            //Hide elements in case not used
            oneboxImage.visibility = View.INVISIBLE
            starIndicator.visibility = View.INVISIBLE
            starCount.visibility = View.INVISIBLE

            //Load the profile pictures! Create a request to get the url for the picture
            Ion.with(mContext)
                    .load("${room?.site}/users/thumbs/${message.userId}")
                    .noCache()
                    .asJsonObject()
                    .setCallback { e, result ->
                        if (e != null) {
                            Log.e("MessageAdapter", e.message.toString())
                        } else {
                            //Get the email_hash attribute which contains either a link to Imgur or a hash for Gravatar
                            val hash = result.get("email_hash").asString.replace("!", "")
                            var imageLink = hash
                            //If Gravatar, create link
                            if (!hash.contains(".")) {
                                imageLink = "https://www.gravatar.com/avatar/$hash"
                            }

                            //Load it into the ImageView!
                            Ion.with(userPicture)
                                    .load(imageLink)
                        }
                    }

            if (room?.site == Client.SITE_STACK_OVERFLOW) {
                if (message.userId == mContext.defaultSharedPreferences.getInt("SOID", -1).toLong()) {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_stackoverflow_mine))
                } else {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_other))
                }
            } else {
                if (message.userId == mContext.defaultSharedPreferences.getInt("SEID", -1).toLong()) {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_stackexchange_mine))
                } else {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_other))
                }
            }
            //If the message is starred, show the indicator and set the count text to the star count
            if (message.message_starred) {
                starIndicator.visibility = View.VISIBLE
                starCount.visibility = View.VISIBLE
                starCount.text = message.message_stars.toString()
            }

            //If the message is deleted, show the text "removed" and make it gray
            if (message.isDeleted) {
                messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.deleted))
                messageView.text = itemView.context.getString(R.string.removed)
            } else {
                //If it's just a plain message, then set the text from HTML
                if (!message.onebox) {
                    messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_text))
                    //If Android version is 24 and above use the updated version, otherwise use the deprecated version
                    val doc = Jsoup.parseBodyFragment("<span>" + message.content + "</span>")
                    val parsedHTML = doc.body().unwrap().toString()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        messageView.text = Html.fromHtml(parsedHTML, Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        @Suppress("DEPRECATION")
                        messageView.text = Html.fromHtml(parsedHTML)
                    }
                    BetterLinkMovementMethod.linkify(Linkify.ALL, messageView)
                } else {
                    //if it's a onebox, then display it specially
                    when (message.onebox_type) {
                        "image" -> {
                            //For images, load the image into the ImageView, making sure it's visible
                            oneboxImage.visibility = View.VISIBLE

                            itemView.setOnClickListener {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.onebox_content)))
                            }

                            Ion.with(itemView.context)
                                    .load(message.onebox_content)
                                    .intoImageView(itemView.message_image)

                            //Set the text to nothing just in case
                            messageView.text = ""
                        }
                    //For Youtube videos, display the image and some text, linking the view to the video on Youtube
                        "youtube" -> {
                            itemView.setOnClickListener {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.onebox_extra)))
                            }
                            oneboxImage.visibility = View.VISIBLE

                            Ion.with(itemView.context)
                                    .load(message.onebox_content)
                                    .intoImageView(itemView.message_image)
                            messageView.text = message.content
                        }
                    //Other oneboxed items just display the HTML until we implement them all
                        else -> {
                            Log.d("Onebox", "Type: ${message.onebox_type}")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                messageView.text = Html.fromHtml(message.content, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                @Suppress("DEPRECATION")
                                messageView.text = Html.fromHtml(message.content)
                            }
                        }
                    }
                }
            }

            //On long clicking the itemView, show a dialog that allows you to edit, delete, or star, depending on the ownership of the message
            itemView.setOnLongClickListener {
                loadMessageActionDialog(message)
                true
            }

            messageView.setOnLongClickListener {
                loadMessageActionDialog(message)
                true
            }

            //Set the username view to the message's user's name
            userNameView.text = message.userName

            //Show the date
            messageTimestamp.text = timestampFormat.format(Date(message.timestamp * 1000))

            //Show if it's been edited
            editIndicator.visibility = if (message.isEdited) View.VISIBLE else View.INVISIBLE
        }

        private fun loadMessageActionDialog(message: MessageEvent) {
            //Initiate a list of Strings, the current user ID, and a Boolean to determine if it's the user's message
            val dialogMessages = mutableListOf<String>()
            val curUserId: Int
            val isUserMessage: Boolean

            //Set the user ID depending on the site
            if (room?.site == Client.SITE_STACK_OVERFLOW) {
                curUserId = mContext.defaultSharedPreferences.getInt("SOID", -1)
            } else {
                curUserId = mContext.defaultSharedPreferences.getInt("SEID", -1)
            }

            //Determine if it's the user's message
            isUserMessage = curUserId == message.userId.toInt()

            //If it's not the user's message, set the dialog messages to starring. Otherwise, let them delete and edit it!
            if (curUserId != -1) {
                if (isUserMessage) {
                    dialogMessages.add(mContext.getString(R.string.edit_message))
                    dialogMessages.add(mContext.getString(R.string.delete_message))
                } else {
                    dialogMessages.add(mContext.getString(R.string.star_message))
                }
            }

            //Show the dialog
            val dialog = DialogPlus.newDialog(mContext)
                    .setContentHolder(ListHolder())
                    .setGravity(Gravity.CENTER)
                    .setAdapter(ModifyMessageAdapter(dialogMessages, mContext))
                    .setOnItemClickListener { plusDialog, _, _, position ->
                        //The clicking of items depends on how many and whether it's the user's message
                        if (isUserMessage) {
                            when (position) {
                                0 -> {
                                    //Show the dialog letting the user edit their message
                                    showEditDialog(message, mContext, plusDialog)
                                }
                                1 -> {
                                    //Delete message
                                    deleteMessage(message.messageId, chatFkey)
                                    plusDialog.dismiss()
                                }
                            }
                        } else {
                            when (position) {
                                0 -> {
                                    //Star the message
                                    starMessage(message.messageId, chatFkey)
                                    plusDialog.dismiss()
                                }
                            }
                        }
                    }
                    //Set some nice padding
                    .setPadding(50, 50, 50, 50)
                    //Create the dialog
                    .create()

            //Show the dialog
            dialog.show()
        }

        /**
         * Function to star the message
         * @param messageId: Integer that is the message to star's ID
         * @param chatFkey: Magical fkey for the room
         */
        fun starMessage(messageId: Int, chatFkey: String?) {
            val client = ClientManager.client
            doAsync {
                //Create request body
                val soLoginRequestBody = FormEncodingBuilder()
                        .add("fkey", chatFkey)
                        .build()
                //Create request
                val soChatPageRequest = Request.Builder()
                        .url("https://chat.stackoverflow.com/messages/$messageId/star")
                        .post(soLoginRequestBody)
                        .build()

                //Star that message!
                client.newCall(soChatPageRequest).execute()
            }
        }

        /**
         * Function to delete the message
         * @param messageId: Integer that is the message to star's ID
         * @param chatFkey: Magical fkey for the room
         */
        fun deleteMessage(messageId: Int, chatFkey: String?) {
            val client = ClientManager.client
            doAsync {
                //Add fkey to body
                val soLoginRequestBody = FormEncodingBuilder()
                        .add("fkey", chatFkey)
                        .build()

                //Create request and execute
                val soChatPageRequest = Request.Builder()
                        .url("https://chat.stackoverflow.com/messages/$messageId/delete")
                        .post(soLoginRequestBody)
                        .build()
                client.newCall(soChatPageRequest).execute()
            }
        }

        /**
         * Function to edit the message
         * @param message: MessageEvent of the message to edit
         * @param mContext: Application context
         * @param plusDialog: DialogPlus so we can dismiss
         */
        fun showEditDialog(message: MessageEvent, mContext: Context, plusDialog: DialogPlus) {
            //Create AlertDialog
            val builder = AlertDialog.Builder(mContext)

            //Set title
            builder.setTitle("Edit message")

            //Create Layout and set padding
            val l = LinearLayout(mContext)
            l.setPadding(14, 14, 14, 14)

            //Create EditText and populate with current message text
            val input = EditText(mContext)
            input.setText(message.content, TextView.BufferType.EDITABLE)

            //Set EditText layoutparams and add to view
            input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            l.addView(input)

            //Set LinearLayout as AlertDialog view
            builder.setView(l)

            //When you press okay, call the function to edit the message
            builder.setPositiveButton("OK", { dialog, _ ->
                editMessage(input.text.toString(), message.messageId, chatFkey)

                //Dismiss both dialogs
                dialog.dismiss()
                plusDialog.dismiss()
            })

            //Cancel the AlertDialog and dismiss
            builder.setNegativeButton("Cancel", { dialog, _ ->
                dialog.cancel()
            })

            builder.show()
        }

        /**
         * Function to edit the message
         * @param editText: String that contains the text to change the message to
         * @param messageId: ID of the message we want to modify
         * @param chatFkey: MAGICAL FKEY
         */
        fun editMessage(editText: String, messageId: Int, chatFkey: String?) {
            val client = ClientManager.client
            doAsync {
                //Create body with text and fkey
                val soLoginRequestBody = FormEncodingBuilder()
                        .add("text", editText)
                        .add("fkey", chatFkey)
                        .build()

                //Create request
                val soChatPageRequest = Request.Builder()
                        .url("https://chat.stackoverflow.com/messages/$messageId/")
                        .post(soLoginRequestBody)
                        .build()

                //Extermin... I mean, Execute
                client.newCall(soChatPageRequest).execute()
            }
        }
    }
}