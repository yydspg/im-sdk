package com.yutak.im.interfaces;


import com.yutak.im.protocol.YutakDisconnectMsg;
import com.yutak.im.protocol.YutakPongMsg;
import com.yutak.im.protocol.YutakSendAckMsg;

/**
 * 2019-11-10 17:03
 * 接受通讯协议消息protocol
 */
public interface IReceivedMsgListener {
    /**
     * 登录状态消息
     *
     * @param statusCode 状态
     */
    void loginStatusMsg(short statusCode);

    /**
     * 心跳消息
     */
    void pongMsg(YutakPongMsg pongMsg);

    /**
     * 被踢消息
     */
    void kickMsg(YutakDisconnectMsg disconnectMsg);

    /**
     * 发送消息状态消息
     *
     * @param sendAckMsg ack
     */
    void sendAckMsg(YutakSendAckMsg sendAckMsg);

    /**
     * 重连
     */
    void reconnect();
}
