package me.shreyasr.chatse.chat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import me.shreyasr.chatse.R
import me.shreyasr.chatse.chat.Room

class RoomAdapter(val list: MutableList<Room>, context: Context) : BaseAdapter() {
    val mInflater: LayoutInflater = LayoutInflater.from(context)
    val mContext = context

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view: View?
        val vh: ListRowHolder
        if (convertView == null) {
            view = mInflater.inflate(R.layout.drawer_list_item, parent, false)
            vh = ListRowHolder(view)
            view?.tag = vh
        } else {
            view = convertView
            vh = view.tag as ListRowHolder
        }
        if (list.isNotEmpty()) {
            vh.name.text = list[position].name
        }
        return view
    }

    private class ListRowHolder(row: View?) {
        val name = row?.findViewById(R.id.room_name) as TextView
    }

    override fun getItem(position: Int): Room {
        return list[position]
    }

    override fun getCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).roomID
    }

}