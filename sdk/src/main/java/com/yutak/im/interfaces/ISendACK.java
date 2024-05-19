package com.yutak.im.interfaces;

import com.yutak.im.domain.YutakMsg;

/**
 * 5/12/21 2:02 PM
 * 发送消息ack监听
 */
public interface ISendACK {
    void msgACK(YutakMsg msg);
}
