package com.tristanwiley.chatse.chat.adapters

import android.content.Context
import android.os.Build
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Html
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
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.event.EventList


/**
 * Adapter to display the users in the right NavigationDrawer of the activity.
 * @param mContext: Context
 * @param events: All the events to get the users from
 */

class UsersAdapter(val mContext: Context, val events: EventList, var users: ArrayList<com.tristanwiley.chatse.event.presenter.message.MessageEvent> = ArrayList()) : RecyclerView.Adapter<com.tristanwiley.chatse.chat.adapters.UsersAdapter.UsersViewHolder>() {

    override fun onBindViewHolder(viewHolder: com.tristanwiley.chatse.chat.adapters.UsersAdapter.UsersViewHolder?, pos: Int) {
        val user = users[pos]
        val holder = viewHolder as com.tristanwiley.chatse.chat.adapters.UsersAdapter.UsersViewHolder
        holder.bindMessage(user)
    }

    //Used to update the list on new events
    fun update() {
        users.clear()
        users.addAll(events.messagePresenter.getUsersList())
        this.notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): com.tristanwiley.chatse.chat.adapters.UsersAdapter.UsersViewHolder {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_user, parent, false)
        return com.tristanwiley.chatse.chat.adapters.UsersAdapter.UsersViewHolder(mContext, view)
    }

    override fun getItemCount() = users.size

    class UsersViewHolder(val mContext: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userPicture = itemView.findViewById<ImageView>(R.id.user_icon)
        val userName = itemView.findViewById<TextView>(R.id.user_name)

        fun bindMessage(user: com.tristanwiley.chatse.event.presenter.message.MessageEvent) {
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
                                if (!result.get("user_message").isJsonNull) {
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

                                //Set SpannableString to TextView
                                val tv = TextView(mContext)
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