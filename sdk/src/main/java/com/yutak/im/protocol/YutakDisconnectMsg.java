package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2020-01-30 17:34
 * 断开连接消息
 */
public class YutakDisconnectMsg extends YutakBaseMsg {
    public byte reasonCode;
    public String reason;

    public YutakDisconnectMsg() {
        packetType = Yutak.MsgType.DISCONNECT;
    }
}
