package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

import com.yutak.im.kit.DateKit;

import java.util.HashMap;

// channel info ,base model
public class YutakChannel implements Parcelable {
    //自增ID
    public long id;
    //频道ID
    public String channelID;
    //频道类型
    public byte channelType;
    //频道名称
    public String channelName;
    //频道备注(频道的备注名称，个人的话就是个人备注，群的话就是群别名)
    public String channelRemark;
    public int showNick;
    //是否置顶
    public int top;
    //是否保存在通讯录
    public int save;
    //免打扰
    public int mute;
    //禁言
    public int forbidden;
    //邀请确认
    public int invite;
    //频道状态[1：正常2：黑名单]
    public int status;
    //是否已关注 0.未关注（陌生人） 1.已关注（好友）
    public int follow;
    //是否删除
    public int isDeleted;
    //创建时间
    public String createdAt;
    //修改时间
    public String updatedAt;
    //频道头像
    public String avatar;
    //版本
    public long version;
    //扩展字段
    public HashMap localExtra;
    //是否在线
    public int online;
    //最后一次离线时间
    public long lastOffline;
    //    优先显示的设备标识
//    public int mainDeviceFlag;
    // 最后一次离线设备标识
    public int deviceFlag;
    //是否回执消息
    public int receipt;
    // 机器人
    public int robot;
    //分类[service:客服]
    public String category;
    public String username;
    public String avatarCacheKey;
    public HashMap remoteExtraMap;
    public int flame;
    public int flameSecond;
    public String parentChannelID;
    public byte parentChannelType;

    public YutakChannel() {
    }

    public YutakChannel(String channelID, byte channelType) {
        this.channelID = channelID;
        this.channelType = channelType;
        avatarCacheKey = "";
        this.createdAt = DateKit.get().format(DateKit.get().nowSeconds());
        this.updatedAt = DateKit.get().format(DateKit.get().nowSeconds());
    }

    protected YutakChannel(Parcel in) {
        id = in.readLong();
        channelID = in.readString();
        channelType = in.readByte();
        channelName = in.readString();
        channelRemark = in.readString();
        showNick = in.readInt();
        top = in.readInt();
        save = in.readInt();
        mute = in.readInt();
        forbidden = in.readInt();
        invite = in.readInt();
        status = in.readInt();
        follow = in.readInt();
        isDeleted = in.readInt();
        createdAt = in.readString();
        updatedAt = in.readString();
        avatar = in.readString();
        version = in.readLong();
        localExtra = in.readHashMap(HashMap.class.getClassLoader());
        online = in.readInt();
        lastOffline = in.readLong();
        category = in.readString();
        receipt = in.readInt();
        robot = in.readInt();
        username = in.readString();
        avatarCacheKey = in.readString();
        remoteExtraMap = in.readHashMap(HashMap.class.getClassLoader());
        flame = in.readInt();
        flameSecond = in.readInt();
        deviceFlag = in.readInt();
        parentChannelID = in.readString();
        parentChannelType = in.readByte();
//        mainDeviceFlag = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(channelID);
        dest.writeByte(channelType);
        dest.writeString(channelName);
        dest.writeString(channelRemark);
        dest.writeInt(showNick);
        dest.writeInt(top);
        dest.writeInt(save);
        dest.writeInt(mute);
        dest.writeInt(forbidden);
        dest.writeInt(invite);
        dest.writeInt(status);
        dest.writeInt(follow);
        dest.writeInt(isDeleted);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
        dest.writeString(avatar);
        dest.writeLong(version);
        dest.writeMap(localExtra);
        dest.writeInt(online);
        dest.writeLong(lastOffline);
        dest.writeString(category);
        dest.writeInt(receipt);
        dest.writeInt(robot);
        dest.writeString(username);
        dest.writeString(avatarCacheKey);
        dest.writeMap(remoteExtraMap);
        dest.writeInt(flame);
        dest.writeInt(flameSecond);
        dest.writeInt(deviceFlag);
        dest.writeString(parentChannelID);
        dest.writeByte(parentChannelType);
//        dest.writeInt(mainDeviceFlag);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<YutakChannel> CREATOR = new Creator<YutakChannel>() {
        @Override
        public YutakChannel createFromParcel(Parcel in) {
            return new YutakChannel(in);
        }

        @Override
        public YutakChannel[] newArray(int size) {
            return new YutakChannel[size];
        }
    };

}
