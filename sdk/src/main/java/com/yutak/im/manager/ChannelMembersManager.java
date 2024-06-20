package com.yutak.im.manager;

import com.yutak.im.interfaces.IAddChannelMemberListener;
import com.yutak.im.interfaces.IGetChannelMemberInfo;
import com.yutak.im.interfaces.IGetChannelMemberList;
import com.yutak.im.interfaces.IRefreshChannelMember;
import com.yutak.im.interfaces.IRemoveChannelMember;
import com.yutak.im.interfaces.ISyncChannelMembers;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelMembersManager {
    private ChannelMembersManager() {
    }

    private static class ChannelMembersManagerBinder {
        static final ChannelMembersManager channelMembersManager = new ChannelMembersManager();
    }

    public static ChannelMembersManager getInstance() {
        return ChannelMembersManagerBinder.channelMembersManager;
    }
    private ConcurrentHashMap<String, IRefreshChannelMember> refreshMemberMap;
    private ConcurrentHashMap<String, IRemoveChannelMember> removeChannelMemberMap;//监听添加频道成员
    private ConcurrentHashMap<String, IAddChannelMemberListener> addChannelMemberMap;
    private ISyncChannelMembers syncChannelMembers;
    //获取频道成员监听
    private IGetChannelMemberInfo iGetChannelMemberInfoListener;
    private IGetChannelMemberList iGetChannelMemberList;

}
