package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class YutakMsgSetting implements Parcelable {
    // 消息是否回执
    public int receipt;
    // 是否开启top
    public int topic;
    // 是否未流消息
    public int stream;

    public YutakMsgSetting() {
    }

    protected YutakMsgSetting(Parcel in) {
        receipt = in.readInt();
        topic = in.readInt();
        stream = in.readInt();
    }

    public static final Creator<YutakMsgSetting> CREATOR = new Creator<YutakMsgSetting>() {
        @Override
        public YutakMsgSetting createFromParcel(Parcel in) {
            return new YutakMsgSetting(in);
        }

        @Override
        public YutakMsgSetting[] newArray(int size) {
            return new YutakMsgSetting[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(receipt);
        dest.writeInt(topic);
        dest.writeInt(stream);
    }
}
