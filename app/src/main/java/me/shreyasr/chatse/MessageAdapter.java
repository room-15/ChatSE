package me.shreyasr.chatse;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.shreyasr.chatse.event.message.MessageEvent;

public class MessageAdapter extends RecyclerView.Adapter {

    private class MessageViewHolder extends RecyclerView.ViewHolder {

        public final View view;

        public MessageViewHolder(View itemView) {
            super(itemView);
            this.view = itemView;
        }
    }

    List<MessageEvent> messages = new ArrayList<>();

    public void addMessages(List<MessageEvent> messageEvents) {
        messages.addAll(messageEvents);
        Collections.sort(messages);
        notifyDataSetChanged();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int pos) {
        TextView view = (TextView) viewHolder.itemView;
        MessageEvent message = messages.get(pos);

        view.setText(message.content);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
