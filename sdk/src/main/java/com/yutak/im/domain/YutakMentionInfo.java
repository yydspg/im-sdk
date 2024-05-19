package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * 2020-10-22 13:28
 * 提醒对象
 */
public class YutakMentionInfo implements Parcelable {

    public boolean isMentionMe;
    public List<String> uids;

    public YutakMentionInfo() {
    }

    protected YutakMentionInfo(Parcel in) {
        isMentionMe = in.readByte() != 0;
        uids = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isMentionMe ? 1 : 0));
        dest.writeStringList(uids);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<YutakMentionInfo> CREATOR = new Creator<YutakMentionInfo>() {
        @Override
        public YutakMentionInfo createFromParcel(Parcel in) {
            return new YutakMentionInfo(in);
        }

        @Override
        public YutakMentionInfo[] newArray(int size) {
            return new YutakMentionInfo[size];
        }
    };
}
