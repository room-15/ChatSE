package me.shreyasr.chatse.about.adapters

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import me.shreyasr.chatse.R
import me.shreyasr.chatse.about.pokos.DevPoko

/**
 * Created by mauker on 31/08/17.
 */
class AboutDevCardAdapter(mContext : Context, val data: ArrayList<DevPoko>) : RecyclerView.Adapter<AboutDevCardAdapter.ViewHolder>() {

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val picture: ImageView = itemView.findViewById(R.id.about_card_profile_picture)
        val name: TextView = itemView.findViewById(R.id.about_card_dev_name)
        val job: TextView = itemView.findViewById(R.id.about_card_dev_job)
        val aboutMe: TextView = itemView.findViewById(R.id.about_card_dev_about_me)
        val rvIcons: RecyclerView = itemView.findViewById(R.id.about_card_dev_rv_icons)
        var adapter: AboutVerticalIconAdapter? = null
        val glm: GridLayoutManager =
                GridLayoutManager(itemView.context,3,GridLayoutManager.VERTICAL, false)

        fun bind(item : DevPoko) {
//            Glide.with(itemView.context)
//                    .load(item.imageRes)
//                    .into(picture)

            picture.setImageResource(item.imageRes)
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

    private val li : LayoutInflater = LayoutInflater.from(mContext)

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): AboutDevCardAdapter.ViewHolder {
        val itemView : View = li.inflate(R.layout.card_about, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AboutDevCardAdapter.ViewHolder?, position: Int) {
        holder?.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int = position
}