package me.shreyasr.chatse.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

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
    private Resources res;
    Context context;

    public MessageAdapter(EventList events, Resources res) {
        this.events = events;
        this.res = res;
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.message_content) TextView messageView;
        @Bind(R.id.message_user_name) TextView userNameView;
        @Bind(R.id.message_timestamp) TextView messageTimestamp;
        @Bind(R.id.message_edit_indicator) ImageView editIndicator;
        @Bind(R.id.message_image) ImageView oneboxImage;

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
        context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message, parent, false);
        return new MessageViewHolder(view);
    }

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat timestampFormat = new SimpleDateFormat("hh:mm aa");

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int pos) {
        MessageEvent message = messages.get(pos);
        MessageViewHolder holder = (MessageViewHolder) viewHolder;

        if (message.isDeleted()) {
            holder.messageView.setTextColor(res.getColor(R.color.deleted));
            holder.messageView.setText("(removed)");
        } else {
            if(!message.onebox) {
                holder.messageView.setTextColor(res.getColor(R.color.primary_text));
                holder.messageView.setText(message.content);
            }else{
//                Toast.makeText(context, message.onebox_content, Toast.LENGTH_SHORT).show();
                Glide.with(context)
                        .load(message.onebox_content)
                        .into(holder.oneboxImage);
            }
        }
        holder.userNameView.setText(message.userName);
        holder.messageTimestamp.setText(timestampFormat.format(new Date(message.timestamp*1000)));
        holder.editIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
