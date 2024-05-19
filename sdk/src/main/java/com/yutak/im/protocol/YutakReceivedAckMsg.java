package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2019-11-11 10:46
 * 收到消息Ack消息
 */
public class YutakReceivedAckMsg extends YutakBaseMsg {
    //服务端的消息ID(全局唯一)
    public String messageID;
    //序列号
    public int messageSeq;
    public YutakReceivedAckMsg() {
        packetType = Yutak.MsgType.REVACK;
        remainingLength = 8;//序列号
    }
}
