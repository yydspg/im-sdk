package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakSyncChat;

/**
 * 2020-10-09 14:43
 * 同步消息返回
 */
public interface ISyncConversationChatBack {
    void onBack(YutakSyncChat syncChat);
}
