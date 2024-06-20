package com.yutak.im.interfaces;

import com.yutak.im.domain.YutakChannelMember;

import java.util.List;

public interface IGetChannelMemberListResult {
    public void onResult(List<YutakChannelMember> list, boolean isRemote);
}
