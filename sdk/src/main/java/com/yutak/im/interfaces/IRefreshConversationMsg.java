package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakUIConversationMsg;

/**
 * 2020-02-21 11:11
 * 刷新最近会话
 */
public interface IRefreshConversationMsg {
    void onRefreshConversationMsg(YutakUIConversationMsg YutakuiConversationMsg, boolean isEnd);
}
