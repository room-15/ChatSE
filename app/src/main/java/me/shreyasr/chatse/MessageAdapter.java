package me.shreyasr.chatse;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.shreyasr.chatse.event.message.MessageEvent;

public class MessageAdapter extends RecyclerView.Adapter {

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

    SimpleDateFormat timestampFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int pos) {
        MessageEvent message = messages.get(pos);
        MessageViewHolder root = (MessageViewHolder) viewHolder;

        root.messageView.setText(message.content);
        root.userNameView.setText(message.user_name);
        root.messageTimestamp.setText(timestampFormat.format(new Date(message.time_stamp*1000)));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
