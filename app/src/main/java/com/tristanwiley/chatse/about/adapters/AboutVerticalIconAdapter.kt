package com.tristanwiley.chatse.about.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.about.pokos.AboutIconPoko

/**
 * Adapter for displaying a list of icons about a user, but vertically.
 */
class AboutVerticalIconAdapter(mContext: Context, val data: ArrayList<AboutIconPoko>) : RecyclerView.Adapter<AboutVerticalIconAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.item_about_dev_icon)
        val message: TextView = itemView.findViewById(R.id.item_about_dev_text)

        fun bind(item: AboutIconPoko) {
            icon.setImageResource(item.iconResource)
            message.text = item.message
            itemView.setOnClickListener(item.clickListener)
        }
    }

    private val li: LayoutInflater = LayoutInflater.from(mContext)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView: View = li.inflate(R.layout.item_about_dev, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position
}