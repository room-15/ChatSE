package me.shreyasr.chatse.chat

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a room: a site and a room number. Immutable.
 */
class ChatRoom : Parcelable {
    val site: String
    val num: Int

    constructor(site: String, num: Int) {
        this.site = site
        this.num = num
    }

    protected constructor(`in`: Parcel) {
        site = `in`.readString()
        num = `in`.readInt()
    }

    override fun toString(): String {
        return "Room " + num
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(site)
        dest.writeInt(num)
    }

    companion object {

        val CREATOR: Parcelable.Creator<ChatRoom> = object : Parcelable.Creator<ChatRoom> {
            override fun createFromParcel(`in`: Parcel): ChatRoom {
                return ChatRoom(`in`)
            }

            override fun newArray(size: Int): Array<ChatRoom?> {
                return arrayOfNulls(size)
            }
        }
    }
}