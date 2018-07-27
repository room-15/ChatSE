package com.tristanwiley.chatse.chat.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.util.Linkify
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.orhanobut.dialogplus.DialogPlus
import com.orhanobut.dialogplus.ListHolder
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatMessageCallback
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.EventList
import com.tristanwiley.chatse.event.presenter.message.MessageEvent
import com.tristanwiley.chatse.extensions.loadUrl
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import kotlinx.android.synthetic.main.list_item_message.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * The beautiful adapter that handles all new messages in the chat
 */
class MessageAdapter(
        private val mContext: Context,
        private val events: EventList,
        private val chatFkey: String?,
        val room: ChatRoom?,
        private var messages: ArrayList<MessageEvent> = ArrayList(),
        private val messageCallback: ChatMessageCallback) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onBindViewHolder(viewHolder: MessageViewHolder, pos: Int) {
        val message = messages[pos]
        val holder = viewHolder as MessageAdapter.MessageViewHolder
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageAdapter.MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_message, parent, false)
        return MessageAdapter.MessageViewHolder(mContext, view, chatFkey, room, messageCallback)
    }

    override fun getItemCount() = messages.size

    /**
     * ViewHolder that handles setting all content in itemView
     */
    class MessageViewHolder(private val mContext: Context, itemView: View, private val chatFkey: String?, val room: ChatRoom?, val messageCallback: ChatMessageCallback) : RecyclerView.ViewHolder(itemView) {
        private val root: LinearLayout = itemView.findViewById(R.id.message_root)
        private val rootMessageLayout = itemView.findViewById<ConstraintLayout>(R.id.message_root_container)
        private val timestampFormat = SimpleDateFormat("hh:mm aa", Locale.getDefault())
        private val messageView = itemView.findViewById<TextView>(R.id.message_content)
        private val userNameView = itemView.findViewById<TextView>(R.id.message_user_name)
        private val messageTimestamp = itemView.findViewById<TextView>(R.id.message_timestamp)
        private val editIndicator = itemView.findViewById<ImageView>(R.id.message_edit_indicator)
        private val starIndicator = itemView.findViewById<ImageView>(R.id.message_star_indicator)
        private val starCount = itemView.findViewById<TextView>(R.id.message_star_count)
        private val oneboxImage = itemView.findViewById<ImageView>(R.id.message_image)
        private val userPicture = itemView.findViewById<ImageView>(R.id.message_user_picture)
        private val userBarBottom = itemView.findViewById<ImageView>(R.id.message_user_color)
        private val placeholderDrawable = ContextCompat.getDrawable(mContext, R.drawable.box) as Drawable
        private var isFullSize: Boolean = false
        private var origWidth = 0
        private var origHeight = 0
        private var isSelected = false

        private val footer: View = itemView.findViewById(R.id.message_footer_actions)

        private val footerOthers: View = itemView.findViewById(R.id.footer_actions_others)
        private val footerSelf: View = itemView.findViewById(R.id.footer_actions_self)

        private val actionSelfFlag: ImageButton = itemView.findViewById(R.id.action_self_flag)
        private val actionSelfEdit: ImageButton = itemView.findViewById(R.id.action_self_edit)
        private val actionSelfDelete: ImageButton = itemView.findViewById(R.id.action_self_delete)

        private val actionFlag: ImageButton = itemView.findViewById(R.id.action_others_flag)
        private val actionStar: ImageButton = itemView.findViewById(R.id.action_others_star)
        private val actionReply: ImageButton = itemView.findViewById(R.id.action_others_reply)

        private var bgColor: Int = Color.WHITE

        fun bindMessage(message: MessageEvent) {
            //Hide elements in case not used
            oneboxImage.visibility = View.GONE
            starIndicator.visibility = View.INVISIBLE
            starCount.visibility = View.INVISIBLE
            messageView.setBackgroundResource(0)

            if (userPicture.drawable != placeholderDrawable)
                userPicture.setImageResource(R.drawable.box)

            actionSelfFlag.setOnClickListener {
                // TODO
            }

            actionSelfEdit.setOnClickListener {
                // TODO
            }

            actionSelfDelete.setOnClickListener {
                // TODO
            }

            actionFlag.setOnClickListener {
                // TODO
            }

            actionStar.setOnClickListener {
                starMessage(message.messageId, chatFkey)
                isSelected = !isSelected
                checkIfSelected(message.messageStars > 0, message.userId.toInt())
            }

            actionReply.setOnClickListener {
                messageCallback.onReplyMessage(message.messageId)
            }

            //Load the profile pictures! Create a request to get the url for the picture
            doAsync {
                val client = ClientManager.client
                val request = Request.Builder()
                        .url("${room?.site}/users/thumbs/${message.userId}")
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

                uiThread {
                    Glide.with(mContext)
                            .asBitmap()
                            .load(imageLink)
                            .into(object : SimpleTarget<Bitmap>() {
                                override fun onResourceReady(result: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                                    //Load it into the ImageView!
                                    userPicture.setImageBitmap(result)
                                    userBarBottom.setBackgroundColor(getDominantColor(result))
                                }
                            })
                }
            }

            itemView.setOnClickListener {
                isSelected = !isSelected
                checkIfSelected(message.messageStars > 0, message.userId.toInt())
            }

            // Setting background color based on the SE chat the user is logged in.
            bgColor = if (room?.site == Client.SITE_STACK_OVERFLOW) {
                if (message.userId == mContext.defaultSharedPreferences.getInt("SOID", -1).toLong()) {
                    ContextCompat.getColor(mContext, R.color.message_stackoverflow_mine)

                } else {
                    ContextCompat.getColor(mContext, R.color.message_other)
                }
            } else {
                if (message.userId == mContext.defaultSharedPreferences.getInt("SEID", -1).toLong()) {
                    ContextCompat.getColor(mContext, R.color.message_stackexchange_mine)
                } else {
                    ContextCompat.getColor(mContext, R.color.message_other)
                }
            }
            rootMessageLayout.setBackgroundColor(bgColor)

            //If the message is starred, show the indicator and set the count text to the star count
            if (message.messageStars > 0) {
                starIndicator.visibility = View.VISIBLE
                starCount.visibility = View.VISIBLE
                starCount.text = message.messageStars.toString()
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
                    when (message.oneboxType) {
                        "image" -> {
                            //For images, load the image into the ImageView, making sure it's visible
                            oneboxImage.visibility = View.VISIBLE

                            if (!isFullSize) {
                                val layoutParams: ViewGroup.LayoutParams = oneboxImage.layoutParams
                                origWidth = layoutParams.width
                                origHeight = layoutParams.height
                            }

                            oneboxImage.loadUrl(message.oneboxContent)

                            val transitionListener = object : Transition.TransitionListener {
                                override fun onTransitionEnd(transition: Transition) {
                                    oneboxImage.loadUrl(message.oneboxContent)
                                }

                                override fun onTransitionResume(transition: Transition) {}

                                override fun onTransitionPause(transition: Transition) {}

                                override fun onTransitionCancel(transition: Transition) {}

                                override fun onTransitionStart(transition: Transition) {}
                            }

                            oneboxImage.setOnClickListener {
                                val trans: Transition = AutoTransition()

                                if (!isFullSize) {
                                    val cSet = ConstraintSet()
                                    cSet.clone(rootMessageLayout)

                                    // Grow as much as it can.
                                    cSet.constrainWidth(R.id.message_image, ConstraintSet.MATCH_CONSTRAINT)
                                    cSet.constrainHeight(R.id.message_image, ConstraintSet.WRAP_CONTENT)

                                    cSet.connect(R.id.message_image, ConstraintSet.LEFT, R.id.message_root_container, ConstraintSet.LEFT)
                                    cSet.connect(R.id.message_image, ConstraintSet.START, R.id.message_root_container, ConstraintSet.START)
                                    cSet.connect(R.id.message_image, ConstraintSet.RIGHT, R.id.message_root_container, ConstraintSet.RIGHT)
                                    cSet.connect(R.id.message_image, ConstraintSet.END, R.id.message_root_container, ConstraintSet.END)

                                    trans.addListener(transitionListener)

                                    TransitionManager.beginDelayedTransition(rootMessageLayout, trans)
                                    cSet.applyTo(rootMessageLayout)

                                } else {
                                    val cSet = ConstraintSet()
                                    cSet.clone(rootMessageLayout)

                                    // Revert it back to its original size.
                                    cSet.constrainWidth(R.id.message_image, origWidth)
                                    cSet.constrainHeight(R.id.message_image, origHeight)

                                    cSet.connect(R.id.message_image, ConstraintSet.LEFT, R.id.message_edit_indicator, ConstraintSet.RIGHT)
                                    cSet.connect(R.id.message_image, ConstraintSet.START, R.id.message_edit_indicator, ConstraintSet.END)
                                    cSet.connect(R.id.message_image, ConstraintSet.RIGHT, R.id.guideline_80, ConstraintSet.LEFT)
                                    cSet.connect(R.id.message_image, ConstraintSet.END, R.id.guideline_80, ConstraintSet.START)

                                    trans.addListener(transitionListener)

                                    TransitionManager.beginDelayedTransition(rootMessageLayout, trans)
                                    cSet.applyTo(rootMessageLayout)
                                }

                                isFullSize = !isFullSize
                            }

//                            itemView.setOnClickListener {
//                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.oneboxContent)))
//                            }

                            //Set the text to nothing just in case
                            messageView.text = ""
                        }
                    //For Youtube videos, display the image and some text, linking the view to the video on Youtube
                        "youtube" -> {
                            itemView.setOnClickListener {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.oneboxExtra)))
                            }
                            oneboxImage.visibility = View.VISIBLE

                            itemView.message_image.loadUrl(message.oneboxContent)
                            messageView.text = message.content
                        }
                    //for twitter tweets, display the profile pic, profile name, and render the text. might need some css
                        "tweet" -> {
                            messageView.background = ContextCompat.getDrawable(itemView.context, R.drawable.background_twitter)
                            messageView.setPadding(15, 15, 15, 15)
                            messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                            messageView.setLinkTextColor(ContextCompat.getColor(itemView.context, R.color.accent_twitter))
                            Log.d("Onebox", "Type: ${message.oneboxType}")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                messageView.text = Html.fromHtml(message.content, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                @Suppress("DEPRECATION")
                                messageView.text = Html.fromHtml(message.content)
                            }

                        }
                    //Other oneboxed items just display the HTML until we implement them all
                        else -> {
                            Log.d("Onebox", "Type: ${message.oneboxType}")
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

        private fun checkIfSelected(isStarred: Boolean, messageUid: Int) {
            // TODO - Move this somewhere else.
            //Set the user ID depending on the site
            val curUserId: Int = if (room?.site == Client.SITE_STACK_OVERFLOW) {
                mContext.defaultSharedPreferences.getInt("SOID", -1)
            } else {
                mContext.defaultSharedPreferences.getInt("SEID", -1)
            }

            if (isSelected) {
                val hsv = FloatArray(3)
                Color.colorToHSV(bgColor, hsv)
                hsv[2] *= 0.9f
                rootMessageLayout.setBackgroundColor(Color.HSVToColor(hsv))

                TransitionManager.beginDelayedTransition(root, AutoTransition())
                // Show self actions
                if (messageUid == curUserId) {
                    footerSelf.visibility = View.VISIBLE
                }
                else {
                    footerOthers.visibility = View.VISIBLE
                }

                if (isStarred && (curUserId == messageUid))
                    actionStar.setColorFilter(ContextCompat.getColor(mContext, R.color.star_starred))
                else
                    actionStar.setColorFilter(Color.BLACK)
            }
            else {
                rootMessageLayout.setBackgroundColor(bgColor)
                footerSelf.visibility = View.GONE
                footerOthers.visibility = View.GONE
            }
        }

        private fun loadMessageActionDialog(message: MessageEvent) {
            //Initiate a list of Strings, the current user ID, and a Boolean to determine if it's the user's message
            val dialogMessages = mutableListOf<String>()
            val isUserMessage: Boolean

            //Set the user ID depending on the site
            val curUserId: Int = if (room?.site == Client.SITE_STACK_OVERFLOW) {
                mContext.defaultSharedPreferences.getInt("SOID", -1)
            } else {
                mContext.defaultSharedPreferences.getInt("SEID", -1)
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
        private fun starMessage(messageId: Int, chatFkey: String?) {
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
        private fun deleteMessage(messageId: Int, chatFkey: String?) {
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
        private fun showEditDialog(message: MessageEvent, mContext: Context, plusDialog: DialogPlus) {
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
        private fun editMessage(editText: String, messageId: Int, chatFkey: String?) {
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

        fun getDominantColor(bitmap: Bitmap): Int {
            val swatchesTemp = Palette.from(bitmap).generate().swatches
            val swatches = ArrayList<Palette.Swatch>(swatchesTemp)
            Collections.sort(swatches) { swatch1, swatch2 -> swatch2.population - swatch1.population }
            return if (swatches.size > 0) swatches[0].rgb else ContextCompat.getColor(mContext, (R.color.primary))
        }
    }
}