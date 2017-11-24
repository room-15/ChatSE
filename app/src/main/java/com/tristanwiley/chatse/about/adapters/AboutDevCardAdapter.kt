package com.tristanwiley.chatse.about.adapters

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.tristanwiley.chatse.R
import com.tristanwiley.chatse.about.pokos.DevPoko

/**
 * Adapter that displays the list of cards about developers.
 */
class AboutDevCardAdapter(mContext: Context, val data: ArrayList<DevPoko>) : RecyclerView.Adapter<AboutDevCardAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerPic: ImageView = itemView.findViewById(R.id.about_card_dev_header_image)
        private val picture: ImageView = itemView.findViewById(R.id.about_card_profile_picture)
        val name: TextView = itemView.findViewById(R.id.about_card_dev_name)
        private val job: TextView = itemView.findViewById(R.id.about_card_dev_job)
        private val aboutMe: TextView = itemView.findViewById(R.id.about_card_dev_about_me)
        private val rvIcons: RecyclerView = itemView.findViewById(R.id.about_card_dev_rv_icons)
        var adapter: AboutVerticalIconAdapter? = null
        private val glm: GridLayoutManager =
                GridLayoutManager(itemView.context, 3, GridLayoutManager.VERTICAL, false)

        fun bind(item: DevPoko) {
            Glide.with(itemView.context)
                    .load(R.drawable.material_bg2)
                    .into(headerPic)

            Glide.with(itemView.context)
                    .load(item.imageRes)
                    .into(picture)

            name.text = item.name
            job.text = item.job
            aboutMe.text = item.aboutMe

            if (adapter == null) {
                adapter = AboutVerticalIconAdapter(itemView.context, item.icons)
            }

            rvIcons.layoutManager = glm
            rvIcons.adapter = adapter
        }
    }

    private val li: LayoutInflater = LayoutInflater.from(mContext)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AboutDevCardAdapter.ViewHolder {
        val itemView: View = li.inflate(R.layout.card_about, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AboutDevCardAdapter.ViewHolder?, position: Int) {
        holder?.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position
}