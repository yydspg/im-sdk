package com.yutak.im.domain;


import android.text.TextUtils;

import com.yutak.im.db.MsgDBManager;
import com.yutak.im.db.ReminderDBManager;
import com.yutak.im.manager.ChannelManager;

import java.util.HashMap;
import java.util.List;

/**
 * 2019-12-01 17:50
 * UI层显示最近会话消息
 */
public class YutakUIConversationMsg {
    public long lastMsgSeq;
    public String clientMsgNo;
    //频道ID
    public String channelID;
    //频道类型
    public byte channelType;
    //最后一条消息时间
    public long lastMsgTimestamp;
    //消息频道
    private YutakChannel YutakChannel;
    //消息正文
    private YutakMsg YutakMsg;
    //未读消息数量
    public int unreadCount;
    public int isDeleted;
    private YutakConversationMsgExtra remoteMsgExtra;
    //高亮内容[{type:1,text:'[有人@你]'}]
    private List<YutakReminder> reminderList;
    //扩展字段
    public HashMap<String, Object> localExtraMap;
    public String parentChannelID;
    public byte parentChannelType;


    public YutakMsg getYutakMsg() {
        if (YutakMsg == null) {
            YutakMsg = MsgDBManager.getInstance().queryWithClientMsgNo(clientMsgNo);
            if (YutakMsg != null && YutakMsg.isDeleted == 1) YutakMsg = null;
        }
        return YutakMsg;
    }

    public void setYutakMsg(YutakMsg YutakMsg) {
        this.YutakMsg = YutakMsg;
    }

    public YutakChannel getYutakChannel() {
        if (YutakChannel == null) {
            YutakChannel = ChannelManager.getInstance().getChannel(channelID, channelType);
        }
        return YutakChannel;
    }

    public void setYutakChannel(YutakChannel YutakChannel) {
        this.YutakChannel = YutakChannel;
    }

    public List<YutakReminder> getReminderList() {
        if (reminderList == null) {
            reminderList = ReminderDBManager.getInstance().queryWithChannelAndDone(channelID, channelType, 0);
        }

        return reminderList;
    }

    public void setReminderList(List<YutakReminder> list) {
        this.reminderList = list;
    }

    public YutakConversationMsgExtra getRemoteMsgExtra() {
        return remoteMsgExtra;
    }

    public void setRemoteMsgExtra(YutakConversationMsgExtra extra) {
        this.remoteMsgExtra = extra;
    }

    public long getSortTime() {
        if (getRemoteMsgExtra() != null && !TextUtils.isEmpty(getRemoteMsgExtra().draft)) {
            return getRemoteMsgExtra().draftUpdatedAt;
        }
        return lastMsgTimestamp;
    }

}
