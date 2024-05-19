package com.yutak.im.protocol;


import com.yutak.im.cs.Yutak;

/**
 * 2019-11-11 10:27
 * 连接talk service确认消息
 */
public class YutakConnectAckMsg extends YutakBaseMsg {
    //客户端时间与服务器的差值，单位毫秒。
    public long timeDiff;
    //连接原因码
    public short reasonCode;
    // 服务端公钥
    public String serverKey;
    // 安全码
    public String salt;

    public YutakConnectAckMsg() {
        packetType = Yutak.MsgType.CONNACK;
    }
}
