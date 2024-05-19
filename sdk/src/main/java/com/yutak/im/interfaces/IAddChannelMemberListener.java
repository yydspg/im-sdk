package com.yutak.im.interfaces;



import java.util.List;

/**
 * 2020-02-01 16:39
 * 添加频道成员
 */
public interface IAddChannelMemberListener {
    void onAddMembers(List<YutakChannelMember> list);
}
