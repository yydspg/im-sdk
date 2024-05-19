package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2019-11-11 10:49
 * 对ping请求的响应
 */
public class YutakPongMsg extends YutakBaseMsg {
    public YutakPongMsg() {
        packetType = Yutak.MsgType.PONG;
    }
}
