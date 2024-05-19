package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 2020-05-10 19:16
 * 频道搜索结果
 */
public class YutakChannelSearchResult implements Parcelable {
    //频道信息
    public YutakChannel YutakChannel;
    //包含的成员名称
    public String containMemberName;

    public YutakChannelSearchResult() {
    }

    protected YutakChannelSearchResult(Parcel in) {
        YutakChannel = in.readParcelable(YutakChannel.class.getClassLoader());
        containMemberName = in.readString();
    }

    public static final Creator<YutakChannelSearchResult> CREATOR = new Creator<YutakChannelSearchResult>() {
        @Override
        public YutakChannelSearchResult createFromParcel(Parcel in) {
            return new YutakChannelSearchResult(in);
        }

        @Override
        public YutakChannelSearchResult[] newArray(int size) {
            return new YutakChannelSearchResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(YutakChannel, flags);
        dest.writeString(containMemberName);
    }
}
