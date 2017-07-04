package me.shreyasr.chatse.event;

import me.shreyasr.chatse.event.presenter.EventPresenter;
import me.shreyasr.chatse.event.presenter.UnfilteredEventPresenter;
import me.shreyasr.chatse.event.presenter.message.MessageEventPresenter;

public class EventList {

    private final int roomNum;

    public UnfilteredEventPresenter unfilteredPresenter = new UnfilteredEventPresenter();
    public MessageEventPresenter messagePresenter = new MessageEventPresenter();

    private EventPresenter[] presenters
            = new EventPresenter[]{unfilteredPresenter, messagePresenter};

    public EventList(int roomNum) {
        this.roomNum = roomNum;
    }

    public void addEvent(ChatEvent event) {
        for (EventPresenter presenter : presenters) {
            presenter.addEvent(event, roomNum);
        }
    }
}
