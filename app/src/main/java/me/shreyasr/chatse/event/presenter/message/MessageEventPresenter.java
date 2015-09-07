package me.shreyasr.chatse.event.presenter.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import me.shreyasr.chatse.event.ChatEvent;
import me.shreyasr.chatse.event.presenter.EventPresenter;
import me.shreyasr.chatse.util.Logger;

public class MessageEventPresenter implements EventPresenter<MessageEvent> {

    TreeSet<MessageEvent> messages = new TreeSet<>();

    @Override public void addEvent(ChatEvent event, int roomNum) {
        switch (event.event_type) {
            case ChatEvent.EVENT_TYPE_MESSAGE:
                messages.add(new MessageEvent(event));
                break;
            case ChatEvent.EVENT_TYPE_EDIT:
            case ChatEvent.EVENT_TYPE_DELETE:
                MessageEvent newMessage = new MessageEvent(event);
                MessageEvent originalMessage = messages.floor(newMessage);
                if (!originalMessage.equals(newMessage)) {
                    Logger.exception(this.getClass(), "Attempting to edit nonexistent message", null);
                    return;
                }
                newMessage.previous = originalMessage;
                messages.remove(originalMessage);
                messages.add(newMessage);
                break;
        }
    }

    @Override public List<MessageEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }
}
