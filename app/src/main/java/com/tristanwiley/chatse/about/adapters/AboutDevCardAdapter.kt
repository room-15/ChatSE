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

/**
 * Created by mauker on 31/08/17.
 */
class AboutDevCardAdapter(mContext : Context, val data: ArrayList<com.tristanwiley.chatse.about.pokos.DevPoko>) : RecyclerView.Adapter<com.tristanwiley.chatse.about.adapters.AboutDevCardAdapter.ViewHolder>() {

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val headerPic : ImageView = itemView.findViewById(R.id.about_card_dev_header_image)
        val picture: ImageView = itemView.findViewById(R.id.about_card_profile_picture)
        val name: TextView = itemView.findViewById(R.id.about_card_dev_name)
        val job: TextView = itemView.findViewById(R.id.about_card_dev_job)
        val aboutMe: TextView = itemView.findViewById(R.id.about_card_dev_about_me)
        val rvIcons: RecyclerView = itemView.findViewById(R.id.about_card_dev_rv_icons)
        var adapter: com.tristanwiley.chatse.about.adapters.AboutVerticalIconAdapter? = null
        val glm: GridLayoutManager =
                GridLayoutManager(itemView.context,3,GridLayoutManager.VERTICAL, false)

        fun bind(item : com.tristanwiley.chatse.about.pokos.DevPoko) {
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
                adapter = com.tristanwiley.chatse.about.adapters.AboutVerticalIconAdapter(itemView.context, item.icons)
            }

            rvIcons.layoutManager = glm
            rvIcons.adapter = adapter
        }
    }

    private val li : LayoutInflater = LayoutInflater.from(mContext)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): com.tristanwiley.chatse.about.adapters.AboutDevCardAdapter.ViewHolder {
        val itemView : View = li.inflate(R.layout.card_about, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: com.tristanwiley.chatse.about.adapters.AboutDevCardAdapter.ViewHolder?, position: Int) {
        holder?.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position
}