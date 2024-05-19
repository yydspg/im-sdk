package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakSyncChannelMsg;

/**
 * 2020-10-10 15:17
 */
public interface ISyncChannelMsgBack {
    void onBack(YutakSyncChannelMsg syncChannelMsg);
}
