package com.tristanwiley.chatse.chat.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedListAdapterCallback
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.style.QuoteSpan
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.squareup.okhttp.FormEncodingBuilder
import com.squareup.okhttp.Request
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatMessageCallback
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.presenter.message.MessageEvent
import com.tristanwiley.chatse.extensions.loadUrl
import com.tristanwiley.chatse.network.Client
import com.tristanwiley.chatse.network.ClientManager
import kotlinx.android.synthetic.main.list_item_message.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.doAsync
import org.jsoup.Jsoup
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

/**
 * The beautiful adapter that handles all new messages in the chat
 */
class MessageAdapter(
        private val mContext: Context,
        private val chatFkey: String?,
        val room: ChatRoom?,
        private val messageCallback: ChatMessageCallback) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(), SelectedMessagesListener {

    private val sortedList: SortedList<MessageEvent>

    init {
        setHasStableIds(true)
        this.sortedList = SortedList<MessageEvent>(MessageEvent::class.java, object : SortedListAdapterCallback<MessageEvent>(this) {
            override fun compare(item1: MessageEvent, item2: MessageEvent): Int = item1.compareTo(item2)

            override fun areContentsTheSame(oldItem: MessageEvent, newItem: MessageEvent): Boolean = (oldItem.hashCode() == newItem.hashCode())

            override fun areItemsTheSame(item1: MessageEvent, item2: MessageEvent): Boolean = (item1 == item2)

        })
    }

    private val selectedMessages: HashSet<Int> = HashSet()

    override fun selectMessage(mId: Int) {
        selectedMessages.add(mId)
    }

    override fun deselectMessage(mId: Int) {
        selectedMessages.remove(mId)
    }

    override fun isSelected(mId: Int): Boolean = selectedMessages.contains(mId)

    override fun onBindViewHolder(holder: MessageViewHolder, pos: Int) {
        val message = sortedList[pos]
        holder.bindMessage(message)
    }

    /**
     * Called when we want to update the messages (there's a new message)
     */
    fun add(messageEvent: MessageEvent) {
        sortedList.add(messageEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_message, parent, false)
        return MessageAdapter.MessageViewHolder(mContext, view, chatFkey, room, messageCallback, this)
    }

    override fun getItemCount() = sortedList.size()

    override fun getItemId(position: Int) = sortedList[position].hashCode().toLong()

    /**
     * ViewHolder that handles setting all content in itemView
     */
    class MessageViewHolder(private val mContext: Context,
                            itemView: View, private val chatFkey: String?,
                            val room: ChatRoom?, private val messageCallback: ChatMessageCallback,
                            private val selectionListener: SelectedMessagesListener) : RecyclerView.ViewHolder(itemView) {

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
//        private var isSelected = false

//        private val footer: View = itemView.findViewById(R.id.message_footer_actions)

        private val footerOthers: View = itemView.findViewById(R.id.footer_actions_others)
        private val footerSelf: View = itemView.findViewById(R.id.footer_actions_self)

        private val actionSelfFlag: ImageButton = itemView.findViewById(R.id.action_self_flag)
        private val actionSelfEdit: ImageButton = itemView.findViewById(R.id.action_self_edit)
        private val actionSelfDelete: ImageButton = itemView.findViewById(R.id.action_self_delete)

        private val actionFlag: ImageButton = itemView.findViewById(R.id.action_others_flag)
        private val actionStar: ImageButton = itemView.findViewById(R.id.action_others_star)
        private val actionReply: ImageButton = itemView.findViewById(R.id.action_others_reply)

        private var bgColor: Int = Color.WHITE

        private fun toggleSelection(message: MessageEvent) {
            if (!message.isDeleted) {
                if (selectionListener.isSelected(message.messageId)) {
                    selectionListener.deselectMessage(message.messageId)
                } else {
                    selectionListener.selectMessage(message.messageId)
                }
                checkIfSelected(selectionListener.isSelected(message.messageId), message.messageStars > 0, message.userId.toInt())
            }
        }

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
                Toast.makeText(itemView.context, "Will be added soon.", Toast.LENGTH_LONG).show()
            }

            actionSelfEdit.setOnClickListener {
                showEditDialog(message, mContext)
            }

            actionSelfDelete.setOnClickListener {
                deleteMessage(message.messageId, chatFkey)
                selectionListener.deselectMessage(message.messageId)
            }

            actionFlag.setOnClickListener {
                // TODO
                Toast.makeText(itemView.context, "Will be added soon.", Toast.LENGTH_LONG).show()
            }

            actionStar.setOnClickListener {
                starMessage(message.messageId, chatFkey)
            }

            actionReply.setOnClickListener {
                messageCallback.onReplyMessage(message.messageId)
                selectionListener.deselectMessage(message.messageId)
                checkIfSelected(selectionListener.isSelected(message.messageId), message.messageStars > 0, message.userId.toInt())
            }

            //Load the profile pictures!
            Glide.with(itemView.context.applicationContext)
                            .asBitmap()
                            .load(message.emailHash)
                            .into(object : CustomViewTarget<ImageView,Bitmap>(userPicture) {
                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    // LoadFailed!. show error image
                                }

                                override fun onResourceCleared(placeholder: Drawable?) {
                                    // show placeholder.
                                }

                                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                                    //Load it into the ImageView!
                                    userPicture.setImageBitmap(resource)
                                    userBarBottom.setBackgroundColor(getDominantColor(resource))
                                }
                            })
 
            itemView.setOnClickListener {
                toggleSelection(message)
            }

            messageView.setOnClickListener {
                toggleSelection(message)
            }

            checkIfSelected(selectionListener.isSelected(message.messageId), message.messageStars > 0, message.userId.toInt())

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
                    if(message.content!!.startsWith("> ")) {
                        Timber.i("Message Content :${message.content}")
                        val content = message.content!!.replaceFirst("> ","")
                        val spanString = SpannableString(content)
                        // QuoteSpan with constructor with color, stripeWidth and gapWidth only available in api 28
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            spanString.setSpan(QuoteSpan(ContextCompat.getColor(mContext, R.color.color_accent), 5, 40), 0, spanString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        } else {
                            spanString.setSpan(QuoteSpan(ContextCompat.getColor(mContext, R.color.color_accent)), 0, spanString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        messageView.text = spanString
                    } else{
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
                            val twitterUrl = Jsoup.parse(message.oneboxContent).getElementsByTag("a")[1].attr("href")
                            messageView.setOnClickListener {
                                itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(twitterUrl)))
                            }
                            messageView.background = ContextCompat.getDrawable(itemView.context, R.drawable.background_twitter)
                            messageView.setPadding(15, 15, 15, 15)
                            messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                            messageView.setLinkTextColor(ContextCompat.getColor(itemView.context, R.color.accent_twitter))
                            Timber.d("Onebox Type: ${message.oneboxType}")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                messageView.text = Html.fromHtml(message.content, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                @Suppress("DEPRECATION")
                                messageView.text = Html.fromHtml(message.content)
                            }

                        }
                        //Other oneboxed items just display the HTML until we implement them all
                        else -> {
                            Timber.d("Onebox Type: ${message.oneboxType}")
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

            //Set the username view to the message's user's name
            userNameView.text = message.userName

            //Show the date
            messageTimestamp.text = timestampFormat.format(Date(message.timestamp * 1000))

            //Show if it's been edited
            editIndicator.visibility = if (message.isEdited) View.VISIBLE else View.INVISIBLE
        }

        private fun checkIfSelected(isSelected: Boolean, isStarred: Boolean, messageUid: Int) {
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
                } else {
                    footerOthers.visibility = View.VISIBLE
                }

                if (isStarred && (curUserId != messageUid))
                    actionStar.setColorFilter(ContextCompat.getColor(mContext, R.color.star_starred))
                else
                    actionStar.setColorFilter(Color.BLACK)
            } else {
                rootMessageLayout.setBackgroundColor(bgColor)
                footerSelf.visibility = View.GONE
                footerOthers.visibility = View.GONE
            }
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
         * plusDialog: DialogPlus so we can dismiss
         */
        private fun showEditDialog(message: MessageEvent, mContext: Context) {
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
            input.setSelection(input.length())

            //Set EditText layoutparams and add to view
            input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            l.addView(input)

            //Set LinearLayout as AlertDialog view
            builder.setView(l)

            //When you press okay, call the function to edit the message
            builder.setPositiveButton("OK") { dialog, _ ->
                editMessage(input.text.toString(), message.messageId, chatFkey)
                selectionListener.deselectMessage(message.messageId)
                dialog.dismiss()
            }

            //Cancel the AlertDialog and dismiss
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

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
            swatches.sortWith(Comparator { swatch1, swatch2 -> swatch2.population - swatch1.population })
            return if (swatches.size > 0) swatches[0].rgb else ContextCompat.getColor(mContext, (R.color.primary))
        }
    }
}
