package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 22:26
 * 消息搜索结果
 */
public class YutakMessageSearchResult implements Parcelable {
    //消息对应的频道信息
    public YutakChannel YutakChannel;
    //包含关键字的信息
    public String searchableWord;
    //条数
    public int messageCount;

    public YutakMessageSearchResult() {
    }

    protected YutakMessageSearchResult(Parcel in) {
        YutakChannel = in.readParcelable(YutakChannel.class.getClassLoader());
        searchableWord = in.readString();
        messageCount = in.readInt();
    }

    public static final Creator<YutakMessageSearchResult> CREATOR = new Creator<YutakMessageSearchResult>() {
        @Override
        public YutakMessageSearchResult createFromParcel(Parcel in) {
            return new YutakMessageSearchResult(in);
        }

        @Override
        public YutakMessageSearchResult[] newArray(int size) {
            return new YutakMessageSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(YutakChannel, flags);
        dest.writeString(searchableWord);
        dest.writeInt(messageCount);
    }
}
