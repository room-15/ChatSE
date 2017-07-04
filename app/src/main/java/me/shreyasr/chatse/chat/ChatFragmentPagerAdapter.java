package me.shreyasr.chatse.chat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ChatFragmentPagerAdapter extends FragmentPagerAdapter {

    private List<ChatFragment> chatFragments = new ArrayList<>();

    public ChatFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addFragment(ChatFragment fragment) {
        chatFragments.add(fragment);
        notifyDataSetChanged();
    }

    public void removeFragment(ChatFragment fragment) {
        chatFragments.remove(fragment);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return chatFragments.size();
    }

    @Override
    public Fragment getItem(int pos) {
        return chatFragments.get(pos);
    }

    @Override
    public String getPageTitle(int pos) {
        return chatFragments.get(pos).getPageTitle();
    }
}
