package me.shreyasr.chatse.chat;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.koushikdutta.ion.Ion;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.shreyasr.chatse.R;
import me.shreyasr.chatse.event.EventList;
import me.shreyasr.chatse.event.presenter.message.MessageEvent;

public class MessageAdapter extends RecyclerView.Adapter {

    List<MessageEvent> messages = new ArrayList<>();
    private EventList events;
    private Resources res;
    private Context context;
    private SimpleDateFormat timestampFormat = new SimpleDateFormat("hh:mm aa", Locale.getDefault());

    public MessageAdapter(EventList events, Resources res, Context context) {
        this.events = events;
        this.res = res;
        this.context = context;
    }

    public void update() {
        messages = events.messagePresenter.getEvents();
        notifyDataSetChanged();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int pos) {
        MessageEvent message = messages.get(pos);
        MessageViewHolder holder = (MessageViewHolder) viewHolder;

        if (message.isDeleted()) {
            holder.messageView.setTextColor(res.getColor(R.color.deleted));
            holder.messageView.setText("(removed)");
//            Glide.clear(holder.oneboxImage);
            holder.oneboxImage.setImageDrawable(null);
        } else {
            if (!message.onebox) {
                holder.messageView.setTextColor(res.getColor(R.color.primary_text));
                //TODO: Testing
                // holder.messageView.setText(message.content);
                holder.messageView.setText(Html.fromHtml(message.content));
//                Ion.clear(holder.oneboxImage);
//                holder.oneboxImage.setImageDrawable(null); // only needed with placeholder
            } else {
                Log.wtf("The Image Url", message.onebox_content);
                Ion.with(context)
                        .load(message.onebox_content)
                        .intoImageView(holder.oneboxImage);
                // When we load an image remove any text from being recycled from the previous item.
                holder.messageView.setText("");
            }
        }

        holder.userNameView.setText(message.userName);
        holder.messageTimestamp.setText(timestampFormat.format(new Date(message.timestamp * 1000)));
        holder.editIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageView = (TextView) itemView.findViewById(R.id.message_content);

        //@BindView(R.id.message_user_name)
        TextView userNameView = (TextView) itemView.findViewById(R.id.message_user_name);
        //@BindView(R.id.c)
        TextView messageTimestamp = (TextView) itemView.findViewById(R.id.message_timestamp);
        //@BindView(R.id.message_edit_indicator)
        ImageView editIndicator = (ImageView) itemView.findViewById(R.id.message_edit_indicator);
        //        @BindView(R.id.message_image)
        ImageView oneboxImage = (ImageView) itemView.findViewById(R.id.message_image);

        public MessageViewHolder(View itemView) {
            super(itemView);
        }
    }
}
