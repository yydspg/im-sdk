package com.yutak.im.interfaces;


import com.xinbida.wukongim.domain.YutakChannel;

/**
 * 2020-02-01 14:38
 * 刷新频道
 */
public interface IRefreshChannel {
    void onRefreshChannel(YutakChannel channel, boolean isEnd);
}
