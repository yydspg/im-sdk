package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakChannelMember;

/**
 * 2020-02-01 15:19
 * 刷新频道成员信息
 */
public interface IRefreshChannelMember {
    void onRefresh(YutakChannelMember channelMember, boolean isEnd);
}
