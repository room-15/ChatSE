package me.shreyasr.chatse.event.presenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.shreyasr.chatse.event.ChatEvent;

public class UnfilteredEventPresenter implements EventPresenter<ChatEvent> {

    private List<ChatEvent> events = new ArrayList<>();

    @Override
    public void addEvent(ChatEvent event, int roomNum) {
        if (event.room_id == roomNum) {
            events.add(event);
        }
    }

    @Override
    public List<ChatEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
