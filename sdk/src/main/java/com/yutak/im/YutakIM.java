package com.yutak.im;

import com.yutak.im.manager.CMDManager;
import com.yutak.im.manager.ChannelManager;
import com.yutak.im.manager.ChannelMembersManager;
import com.yutak.im.manager.ConnectionManager;
import com.yutak.im.manager.ConversationManager;
import com.yutak.im.manager.MsgManager;
import com.yutak.im.manager.ReminderManager;
import com.yutak.im.manager.RobotManager;

public class YutakIM {
    private final String Version = "V1.1.4";
    private boolean isDebug = false;
    private boolean isWriteLog = false;

    private final static YutakIM yutakIM = new YutakIM();
    private YutakIM() {}

    public static YutakIM get() {return yutakIM;}
    public boolean isDebug() {
        return isDebug;
    }

    public boolean isWriteLog() {
        return isWriteLog;
    }

    public void setWriteLog(boolean isWriteLog) {
        this.isWriteLog = isWriteLog;
    }

    // debug模式会输出一些连接信息，发送消息情况等
    public void setDebug(boolean isDebug) {
        this.isDebug = isDebug;
    }
    public void setFileCacheDir(String fileDir) {
        YutakApplication.get().setFileCacheDir(fileDir);
    }

    public String getVersion() {
        return Version;
    }

    // 获取消息管理
    public MsgManager getMsgManager() {
        return MsgManager.getInstance();
    }

    // 获取连接管理
    public ConnectionManager getConnectionManager() {
        return ConnectionManager.getInstance();
    }

    // 获取频道管理
    public ChannelManager getChannelManager() {
        return ChannelManager.getInstance();
    }

    // 获取最近会话管理
    public ConversationManager getConversationManager() {
        return ConversationManager.getInstance();
    }

    // 获取频道成员管理
    public ChannelMembersManager getChannelMembersManager() {
        return ChannelMembersManager.getInstance();
    }

    //获取提醒管理
    public ReminderManager getReminderManager() {
        return ReminderManager.getInstance();
    }

    // 获取cmd管理
    public CMDManager getCMDManager() {
        return CMDManager.getInstance();
    }

    public RobotManager getRobotManager() {
        return RobotManager.getInstance();
    }

}
