package me.shreyasr.chatse.chat

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import java.util.*

class ChatFragmentPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val chatFragments = ArrayList<ChatFragment>()

    fun addFragment(fragment: ChatFragment) {
        chatFragments.add(fragment)
        notifyDataSetChanged()
    }

    fun removeFragment(fragment: ChatFragment) {
        chatFragments.remove(fragment)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return chatFragments.size
    }

    override fun getItem(pos: Int): Fragment {
        return chatFragments[pos]
    }

    override fun getPageTitle(pos: Int): String {
        return chatFragments[pos].pageTitle
    }
}
