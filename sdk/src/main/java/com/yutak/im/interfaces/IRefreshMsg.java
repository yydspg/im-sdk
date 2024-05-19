package com.yutak.im.interfaces;


import com.yutak.im.domain.YutakMsg;

/**
 * 2020-08-27 21:18
 * 消息修改监听
 */
public interface IRefreshMsg {
    void onRefresh(YutakMsg msg, boolean left);
}
