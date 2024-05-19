package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 4/16/21 1:52 PM
 * 消息回应
 */
public class YutakMsgReaction implements Parcelable {
    public String messageID;
    public String channelID;
    public byte channelType;
    public String uid;
    public String name;
    public long seq;
    public String emoji;
    public int isDeleted;
    public String createdAt;

    public YutakMsgReaction() {
    }

    protected YutakMsgReaction(Parcel in) {
        messageID = in.readString();
        channelID = in.readString();
        channelType = in.readByte();
        uid = in.readString();
        name = in.readString();
        seq = in.readLong();
        emoji = in.readString();
        isDeleted = in.readInt();
        createdAt = in.readString();
    }

    public static final Creator<YutakMsgReaction> CREATOR = new Creator<YutakMsgReaction>() {
        @Override
        public YutakMsgReaction createFromParcel(Parcel in) {
            return new YutakMsgReaction(in);
        }

        @Override
        public YutakMsgReaction[] newArray(int size) {
            return new YutakMsgReaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(messageID);
        dest.writeString(channelID);
        dest.writeByte(channelType);
        dest.writeString(uid);
        dest.writeString(name);
        dest.writeLong(seq);
        dest.writeString(emoji);
        dest.writeInt(isDeleted);
        dest.writeString(createdAt);
    }
}
