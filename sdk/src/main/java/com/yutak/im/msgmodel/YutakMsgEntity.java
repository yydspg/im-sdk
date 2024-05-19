package com.yutak.im.msgmodel;

import android.os.Parcel;
import android.os.Parcelable;

public class YutakMsgEntity implements Parcelable {
    public int offset;
    public int length;
    public String type;
    public String value;

    public YutakMsgEntity() {
    }

    protected YutakMsgEntity(Parcel in) {
        offset = in.readInt();
        length = in.readInt();
        type = in.readString();
        value = in.readString();
    }

    public static final Creator<YutakMsgEntity> CREATOR = new Creator<YutakMsgEntity>() {
        @Override
        public YutakMsgEntity createFromParcel(Parcel in) {
            return new YutakMsgEntity(in);
        }

        @Override
        public YutakMsgEntity[] newArray(int size) {
            return new YutakMsgEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(offset);
        parcel.writeInt(length);
        parcel.writeString(type);
        parcel.writeString(value);
    }
}
