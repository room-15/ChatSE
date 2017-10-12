package com.tristanwiley.chatse.stars

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.AutoTransition
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.koushikdutta.ion.Ion
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.chat.ChatRoom
import com.tristanwiley.chatse.event.ChatEvent
import com.tristanwiley.chatse.extensions.loadUrl
import com.tristanwiley.chatse.network.Client
import kotlinx.android.synthetic.main.list_item_message.view.*
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import org.jetbrains.anko.defaultSharedPreferences
import org.jsoup.Jsoup
import java.util.*
import kotlin.collections.ArrayList

/**
 * The fantastic adapter that displays all stars in a room
 */
class StarsMessageAdapter(val mContext: Context, val events: ArrayList<ChatEvent>, val room: ChatRoom) : RecyclerView.Adapter<StarsMessageAdapter.MessageViewHolder>() {

    override fun onBindViewHolder(viewHolder: MessageViewHolder?, pos: Int) {
        val message = events[pos]
        val holder = viewHolder as MessageViewHolder
        holder.bindMessage(message)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_message, parent, false)
        return MessageViewHolder(mContext, view, room)
    }

    override fun getItemCount() = events.size

    /**
     * ViewHolder that handles setting all content in itemView
     */
    class MessageViewHolder(private val mContext: Context, itemView: View, val room: ChatRoom) : RecyclerView.ViewHolder(itemView) {
        private val rootLayout = itemView.findViewById<ConstraintLayout>(R.id.message_root_container)
        private val messageView = itemView.findViewById<TextView>(R.id.message_content)
        private val userNameView = itemView.findViewById<TextView>(R.id.message_user_name)
        private val messageTimestamp = itemView.findViewById<TextView>(R.id.message_timestamp)
        private val starIndicator = itemView.findViewById<ImageView>(R.id.message_star_indicator)
        private val starCount = itemView.findViewById<TextView>(R.id.message_star_count)
        private val oneboxImage = itemView.findViewById<ImageView>(R.id.message_image)
        private val userPicture = itemView.findViewById<ImageView>(R.id.message_user_picture)
        private val userBarBottom = itemView.findViewById<ImageView>(R.id.message_user_color)
        private val placeholderDrawable = ContextCompat.getDrawable(mContext, R.drawable.box) as Drawable
        var isFullSize: Boolean = false
        var origWidth = 0
        var origHeight = 0

        fun bindMessage(message: ChatEvent) {
            //Hide elements in case not used
            oneboxImage.visibility = View.GONE
            starIndicator.visibility = View.INVISIBLE
            starCount.visibility = View.INVISIBLE
            messageView.setBackgroundResource(0)

            if (userPicture.drawable != placeholderDrawable)
                userPicture.setImageResource(R.drawable.box)

            //Load the profile pictures! Create a request to get the url for the picture
            Ion.with(mContext)
                    .load("${room.site}/users/thumbs/${message.user_id}")
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
                            } else if (!hash.contains("http")) {
                                imageLink = room.site + hash
                            }

                            //Load it into the ImageView!
                            Ion.with(mContext)
                                    .load(imageLink)
                                    .asBitmap()
                                    .setCallback { error, imageResult ->
                                        if (error != null || imageResult == null) {
                                            Log.e("profilePic", e.toString())
                                        } else {
                                            userPicture.setImageBitmap(imageResult)
                                            userBarBottom.setBackgroundColor(getDominantColor(imageResult))
                                        }
                                    }
                        }
                    }

            if (room.site == Client.SITE_STACK_OVERFLOW) {
                if (message.user_id == mContext.defaultSharedPreferences.getInt("SOID", -1)) {
                    rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_stackoverflow_mine))
                } else {
                    rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_other))
                }
            } else {
                if (message.user_id == mContext.defaultSharedPreferences.getInt("SEID", -1)) {
                    rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_stackexchange_mine))
                } else {
                    rootLayout.setBackgroundColor(ContextCompat.getColor(mContext, R.color.message_other))
                }
            }

            //If the message is starred, show the indicator and set the count text to the star count
            if (message.message_stars > 0) {
                starIndicator.visibility = View.VISIBLE
                starCount.visibility = View.VISIBLE
                starCount.text = message.message_stars.toString()
            }

            //If it's just a plain message, then set the text from HTML
            if (!message.message_onebox) {
                messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_text))
                //If Android version is 24 and above use the updated version, otherwise use the deprecated version
                val doc = Jsoup.parseBodyFragment("<span>" + message.contents + "</span>")
                val parsedHTML = doc.body().unwrap().toString()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    messageView.text = Html.fromHtml(parsedHTML, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION")
                    messageView.text = Html.fromHtml(parsedHTML)
                }
                BetterLinkMovementMethod.linkify(Linkify.ALL, messageView)
            } else {
                Log.wtf("ONEBOX_CONTENT", message.onebox_content)
                //if it's a onebox, then display it specially
                when (message.onebox_type) {
                    "image" -> {
                        //For images, load the image into the ImageView, making sure it's visible
                        oneboxImage.visibility = View.VISIBLE

                        if (!isFullSize) {
                            val layoutParams: ViewGroup.LayoutParams = oneboxImage.layoutParams
                            origWidth = layoutParams.width
                            origHeight = layoutParams.height
                        }

                        oneboxImage.loadUrl(message.onebox_content)

                        val transitionListener = object : Transition.TransitionListener {
                            override fun onTransitionEnd(transition: Transition) {
                                oneboxImage.loadUrl(message.onebox_content)
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
                                cSet.clone(rootLayout)

                                // Grow as much as it can.
                                cSet.constrainWidth(R.id.message_image, ConstraintSet.MATCH_CONSTRAINT)
                                cSet.constrainHeight(R.id.message_image, ConstraintSet.WRAP_CONTENT)

                                cSet.connect(R.id.message_image, ConstraintSet.LEFT, R.id.message_root_container, ConstraintSet.LEFT)
                                cSet.connect(R.id.message_image, ConstraintSet.START, R.id.message_root_container, ConstraintSet.START)
                                cSet.connect(R.id.message_image, ConstraintSet.RIGHT, R.id.message_root_container, ConstraintSet.RIGHT)
                                cSet.connect(R.id.message_image, ConstraintSet.END, R.id.message_root_container, ConstraintSet.END)

                                trans.addListener(transitionListener)

                                TransitionManager.beginDelayedTransition(rootLayout, trans)
                                cSet.applyTo(rootLayout)

                            } else {
                                val cSet = ConstraintSet()
                                cSet.clone(rootLayout)

                                // Revert it back to its original size.
                                cSet.constrainWidth(R.id.message_image, origWidth)
                                cSet.constrainHeight(R.id.message_image, origHeight)

                                cSet.connect(R.id.message_image, ConstraintSet.RIGHT, R.id.guideline_80, ConstraintSet.LEFT)
                                cSet.connect(R.id.message_image, ConstraintSet.END, R.id.guideline_80, ConstraintSet.START)

                                trans.addListener(transitionListener)

                                TransitionManager.beginDelayedTransition(rootLayout, trans)
                                cSet.applyTo(rootLayout)
                            }

                            isFullSize = !isFullSize
                        }

//                            itemView.setOnClickListener {
//                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.onebox_content)))
//                            }

                        //Set the text to nothing just in case
                        messageView.text = ""
                    }
                //For Youtube videos, display the image and some text, linking the view to the video on Youtube
                    "youtube" -> {
                        itemView.setOnClickListener {
                            mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(message.onebox_content)))
                        }
                        oneboxImage.visibility = View.VISIBLE

                        itemView.message_image.loadUrl(message.onebox_content)

                        messageView.text = message.contents
                    }
                //for twitter tweets, display the profile pic, profile name, and render the text. might need some css
                    "tweet" -> {
                        messageView.background = ContextCompat.getDrawable(itemView.context, R.drawable.background_twitter)
                        messageView.setPadding(15, 15, 15, 15)
                        messageView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                        messageView.setLinkTextColor(ContextCompat.getColor(itemView.context, R.color.accent_twitter))
                        Log.d("Onebox", "Type: ${message.onebox_type}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            messageView.text = Html.fromHtml(message.onebox_content, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            @Suppress("DEPRECATION")
                            messageView.text = Html.fromHtml(message.onebox_content)
                        }
                        BetterLinkMovementMethod.linkify(Linkify.ALL, messageView)
                    }
                //Other oneboxed items just display the HTML until we implement them all
                    else -> {
                        Log.d("Onebox", "Type: ${message.onebox_type}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            messageView.text = Html.fromHtml(message.contents, Html.FROM_HTML_MODE_LEGACY)
                        } else {
                            @Suppress("DEPRECATION")
                            messageView.text = Html.fromHtml(message.contents)
                        }
                    }
                }
            }

            //Set the username view to the message's user's name
            userNameView.text = message.user_name

            //Show the date
            messageTimestamp.text = message.star_timestamp
        }


        fun getDominantColor(bitmap: Bitmap): Int {
            val swatchesTemp = Palette.from(bitmap).generate().swatches
            val swatches = ArrayList<Palette.Swatch>(swatchesTemp)
            Collections.sort(swatches) { swatch1, swatch2 -> swatch2.population - swatch1.population }
            return if (swatches.size > 0) swatches[0].rgb else ContextCompat.getColor(mContext, (R.color.primary))
        }
    }
}