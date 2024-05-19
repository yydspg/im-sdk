package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2019-11-11 10:49
 * 心跳消息
 */
public class YutakPingMsg extends YutakBaseMsg {
    public YutakPingMsg() {
        packetType = Yutak.MsgType.PING;
    }
}
