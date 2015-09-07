package me.shreyasr.chatse.chat;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.shreyasr.chatse.R;
import me.shreyasr.chatse.event.EventList;
import me.shreyasr.chatse.event.presenter.message.MessageEvent;

public class MessageAdapter extends RecyclerView.Adapter {

    private EventList events;

    public MessageAdapter(EventList events) {
        this.events = events;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.message_content) TextView messageView;
        @Bind(R.id.message_user_name) TextView userNameView;
        @Bind(R.id.message_timestamp) TextView messageTimestamp;

        public MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    List<MessageEvent> messages = new ArrayList<>();

    public void update() {
        messages = events.messagePresenter.getEvents();
        notifyDataSetChanged();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message, parent, false);
        return new MessageViewHolder(view);
    }

    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat timestampFormat = new SimpleDateFormat("hh:mm aa");

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int pos) {
        MessageEvent message = messages.get(pos);
        MessageViewHolder root = (MessageViewHolder) viewHolder;

        root.messageView.setText(message.content);
        root.userNameView.setText(message.userName);
        root.messageTimestamp.setText(timestampFormat.format(new Date(message.timestamp*1000)));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
