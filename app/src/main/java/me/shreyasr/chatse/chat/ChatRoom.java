package me.shreyasr.chatse.chat;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a room: a site and a room number. Immutable.
 */
public class ChatRoom implements Parcelable {

    public final String site;
    public final int num;

    public ChatRoom(String site, int num) {
        this.site = site;
        this.num = num;
    }

    protected ChatRoom(Parcel in) {
        site = in.readString();
        num = in.readInt();
    }

    @Override public String toString() {
        return "Room " + num;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(site);
        dest.writeInt(num);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ChatRoom> CREATOR = new Parcelable.Creator<ChatRoom>() {
        @Override
        public ChatRoom createFromParcel(Parcel in) {
            return new ChatRoom(in);
        }

        @Override
        public ChatRoom[] newArray(int size) {
            return new ChatRoom[size];
        }
    };
}