package me.shreyasr.chatse.event.presenter;

import java.util.List;

import me.shreyasr.chatse.event.ChatEvent;

public interface EventPresenter<T> {

    void addEvent(ChatEvent event, int roomNum);

    List<T> getEvents();
}
