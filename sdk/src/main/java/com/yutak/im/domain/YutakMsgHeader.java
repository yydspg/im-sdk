package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

public class YutakMsgHeader implements Parcelable {
    //是否持久化[是否保存在数据库]
    public boolean noPersist;
    //对方是否显示红点
    public boolean redDot = true;
    //消息是否只同步一次
    public boolean syncOnce;

    YutakMsgHeader() {

    }

    protected YutakMsgHeader(Parcel in) {
        noPersist = in.readByte() != 0;
        redDot = in.readByte() != 0;
        syncOnce = in.readByte() != 0;
    }

    public static final Creator<YutakMsgHeader> CREATOR = new Creator<YutakMsgHeader>() {
        @Override
        public YutakMsgHeader createFromParcel(Parcel in) {
            return new YutakMsgHeader(in);
        }

        @Override
        public YutakMsgHeader[] newArray(int size) {
            return new YutakMsgHeader[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (noPersist ? 1 : 0));
        parcel.writeByte((byte) (redDot ? 1 : 0));
        parcel.writeByte((byte) (syncOnce ? 1 : 0));
    }
}
