package com.yutak.im.interfaces;


import com.xinbida.wukongim.domain.YutakChannelMember;

/**
 * 2019-12-01 15:54
 * 频道成员
 */
public interface IChannelMemberInfoListener {
    void onResult(YutakChannelMember channelMember);
}
