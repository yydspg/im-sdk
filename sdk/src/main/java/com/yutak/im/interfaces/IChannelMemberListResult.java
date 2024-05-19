package com.yutak.im.interfaces;


import com.xinbida.wukongim.domain.YutakChannelMember;

import java.util.List;

public interface IChannelMemberListResult {
    void onResult(List<YutakChannelMember> list);
}
