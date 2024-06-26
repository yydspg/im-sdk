package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2019-11-11 10:33
 * 发送消息Ack消息
 */
public class YutakSendAckMsg extends YutakBaseMsg {
    //客户端消息序列号
    public int clientSeq;
    //服务端的消息ID(全局唯一)
    public String messageID;
    //消息序号（有序递增，用户唯一）
    public long messageSeq;
    //发送原因代码 1表示成功
    public byte reasonCode;

    public YutakSendAckMsg() {
        packetType = Yutak.MsgType.SENDACK;
    }
}
