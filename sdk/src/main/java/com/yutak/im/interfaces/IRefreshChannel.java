package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakChannel;

// refresh channel
public interface IRefreshChannel {
    void onRefreshChannel(YutakChannel channel, boolean isEnd);
}
