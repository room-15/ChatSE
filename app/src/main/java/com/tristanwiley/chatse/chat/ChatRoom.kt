package com.tristanwiley.chatse.chat

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a room: a site and a room number. Immutable.
 */
class ChatRoom(val site: String, val num: Int) : Parcelable {

    private constructor(source: Parcel) : this(source.readString(), source.readInt())

    override fun toString(): String {
        return "Room $num"
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(site)
        dest.writeInt(num)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<com.tristanwiley.chatse.chat.ChatRoom> = object : Parcelable.Creator<com.tristanwiley.chatse.chat.ChatRoom> {
            override fun createFromParcel(`in`: Parcel): com.tristanwiley.chatse.chat.ChatRoom {
                return com.tristanwiley.chatse.chat.ChatRoom(`in`)
            }

            override fun newArray(size: Int): Array<com.tristanwiley.chatse.chat.ChatRoom?> {
                return arrayOfNulls(size)
            }
        }
    }
}