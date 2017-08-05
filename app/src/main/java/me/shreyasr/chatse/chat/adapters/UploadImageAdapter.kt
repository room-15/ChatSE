package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import me.shreyasr.chatse.R

/**
 * Adapter for DialogPlus to display the different uploading options
 * @param context: Application context
 */
class UploadImageAdapter(context: Context) : BaseAdapter() {

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
            view = layoutInflater.inflate(R.layout.simple_grid_item, parent, false)

            viewHolder = ViewHolder()
            viewHolder.textView = view.findViewById(R.id.text_view) as TextView
            viewHolder.imageView = view.findViewById(R.id.image_view) as ImageView
            view.tag = viewHolder
        } else {
            viewHolder = view.tag as ViewHolder
        }

        val context = parent.context
        when (position) {
            0 -> {
                viewHolder.textView?.text = context.getString(R.string.take_photo)
                viewHolder.imageView?.setImageResource(R.drawable.ic_camera)
            }
            1 -> {
                viewHolder.textView?.text = context.getString(R.string.choose_from_gallery)
                viewHolder.imageView?.setImageResource(R.drawable.ic_gallery_pick)
            }
        }

        return view
    }

    internal class ViewHolder {
        var textView: TextView? = null
        var imageView: ImageView? = null
    }
}
