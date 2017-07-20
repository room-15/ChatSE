package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import me.shreyasr.chatse.R

class ModifyMessageAdapter(context: Context) : BaseAdapter() {

    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val viewHolder: ViewHolder
        var view = convertView

        if (view == null) {
            view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)

            viewHolder = ViewHolder()
            viewHolder.textView = view.findViewById(android.R.id.text1) as TextView
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val context = parent.context
        when (position) {
            0 -> {
                viewHolder.textView?.text = "Edit Message"
            }
            1 -> {
                viewHolder.textView?.text = "Star Message"
            }
//            2 -> {
//                viewHolder.textView?.text = context.getString(R.string.upload_from_url)
//                viewHolder.imageView?.setImageResource(R.drawable.ic_link)
//            }
        }

        return view
    }

    internal class ViewHolder {
        var textView: TextView? = null
    }
}
