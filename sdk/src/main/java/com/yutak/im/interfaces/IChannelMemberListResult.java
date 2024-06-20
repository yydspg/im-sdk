package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakChannelMember;

import java.util.List;

public interface IChannelMemberListResult {
    void onResult(List<YutakChannelMember> list);
}
