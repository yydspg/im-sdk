package com.yutak.im.domain;

import android.os.Parcel;
import android.os.Parcelable;

import com.yutak.im.kit.DateKit;

import java.util.HashMap;

/**
 * 2019-11-09 18:07
 * 频道成员实体
 */
public class YutakChannelMember implements Parcelable {
    //自增ID
    public long id;
    //频道id
    public String channelID;
    //频道类型
    public byte channelType;
    //成员id
    public String memberUID;
    //成员名称
    public String memberName;
    //成员备注
    public String memberRemark;
    //成员头像
    public String memberAvatar;
    //成员角色
    public int role;
    //成员状态黑名单等1：正常2：黑名单
    public int status;
    //是否删除
    public int isDeleted;
    //创建时间
    public String createdAt;
    //修改时间
    public String updatedAt;
    //版本
    public long version;
    // 机器人0否1是
    public int robot;
    //扩展字段
    public HashMap extraMap;
    // 用户备注
    public String remark;
    // 邀请者uid
    public String memberInviteUID;
    // 被禁言到期时间
    public long forbiddenExpirationTime;
    public String memberAvatarCacheKey;

    public YutakChannelMember() {
        createdAt = DateKit.get().format(DateKit.get().nowSeconds());
        updatedAt = DateKit.get().format(DateKit.get().nowSeconds());
    }

    protected YutakChannelMember(Parcel in) {
        id = in.readLong();
        status = in.readInt();
        channelID = in.readString();
        channelType = in.readByte();
        memberUID = in.readString();
        memberName = in.readString();
        memberRemark = in.readString();
        memberAvatar = in.readString();
        role = in.readInt();
        isDeleted = in.readInt();
        createdAt = in.readString();
        updatedAt = in.readString();
        version = in.readLong();
        extraMap = in.readHashMap(HashMap.class.getClassLoader());
        remark = in.readString();
        memberInviteUID = in.readString();
        robot = in.readInt();
        forbiddenExpirationTime = in.readLong();
        memberAvatarCacheKey = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(status);
        dest.writeString(channelID);
        dest.writeByte(channelType);
        dest.writeString(memberUID);
        dest.writeString(memberName);
        dest.writeString(memberRemark);
        dest.writeString(memberAvatar);
        dest.writeInt(role);
        dest.writeInt(isDeleted);
        dest.writeString(createdAt);
        dest.writeString(updatedAt);
        dest.writeLong(version);
        dest.writeMap(extraMap);
        dest.writeString(remark);
        dest.writeString(memberInviteUID);
        dest.writeInt(robot);
        dest.writeLong(forbiddenExpirationTime);
        dest.writeString(memberAvatarCacheKey);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<YutakChannelMember> CREATOR = new Creator<YutakChannelMember>() {
        @Override
        public YutakChannelMember createFromParcel(Parcel in) {
            return new YutakChannelMember(in);
        }

        @Override
        public YutakChannelMember[] newArray(int size) {
            return new YutakChannelMember[size];
        }
    };
}
