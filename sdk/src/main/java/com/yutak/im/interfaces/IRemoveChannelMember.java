package com.yutak.im.interfaces;


import com.xinbida.wukongim.domain.YutakChannelMember;

import java.util.List;

/**
 * 2020-02-01 16:43
 * 移除频道成员
 */
public interface IRemoveChannelMember {
    void onRemoveMembers(List<YutakChannelMember> list);
}
