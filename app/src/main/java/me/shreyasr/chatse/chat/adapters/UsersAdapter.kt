package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.InputType
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.koushikdutta.ion.Ion
import me.shreyasr.chatse.R
import me.shreyasr.chatse.event.EventList
import me.shreyasr.chatse.event.presenter.message.MessageEvent


/**
 * Adapter to display the users in the right NavigationDrawer of the activity.
 * @param mContext: Context
 * @param events: All the events to get the users from
 */

class UsersAdapter(val mContext: Context, val events: EventList, var users: ArrayList<MessageEvent> = ArrayList()) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    override fun onBindViewHolder(viewHolder: UsersViewHolder?, pos: Int) {
        val user = users[pos]
        val holder = viewHolder as UsersViewHolder
        holder.bindMessage(user)
    }

    //Used to update the list on new events
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
            //Set the username to the TextView
            userName.text = user.userName

            //Load profile into ImageView with Ion
            Ion.with(mContext)
                    .load(user.email_hash)
                    .intoImageView(userPicture)

            //On click, show information about user
            itemView.setOnClickListener {
                Ion.with(mContext)
                        .load("https://chat.stackoverflow.com/users/thumbs/${user.userId}")
                        .asJsonObject()
                        .setCallback { e, result ->
                            if (e != null) {
                                Log.e("ChatFragment", e.message.toString())
                            } else {
                                //Create AlertDialog with title of their name
                                val builder = AlertDialog.Builder(mContext)
                                        .setTitle(result.get("name").asString)

                                //Create layout
                                val layout = LinearLayout(mContext)

                                //Get DPI so we can add padding and look natural
                                val dpi = mContext.resources.displayMetrics.density.toInt()
                                layout.setPadding((19 * dpi), (5 * dpi), (14 * dpi), (5 * dpi))

                                val s: SpannableString

                                //Set user message to the body of the AlertDialog and Linkify links
                                if (result.has("user_message")) {
                                    if (result.get("user_message").asString.isNotEmpty()) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            s = SpannableString(Html.fromHtml(result.get("user_message").asString, Html.FROM_HTML_MODE_COMPACT))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            s = SpannableString(Html.fromHtml(result.get("user_message").asString))
                                        }
                                        Linkify.addLinks(s, Linkify.ALL)
                                    } else {
                                        s = SpannableString("There's no user bio! :(")
                                    }
                                } else {
                                    s = SpannableString("There's no user bio! :(")
                                }

                                //Set SpannableString to TextView
                                val tv = TextView(mContext)
                                tv.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                tv.text = s

                                //Add TextView to Layout and set Layout to AlertDialog view
                                layout.addView(tv)
                                builder.setView(layout)

                                //Create cancel button to cancel dialog
                                builder.setNegativeButton("Cancel", { dialog, _ ->
                                    dialog.cancel()
                                })

                                //Show Dialog
                                builder.show()
                            }
                        }
            }
        }
    }
}